/**
 * Lasius - Open source time tracker for teams
 * Copyright (c) Tegonal Genossenschaft (https://tegonal.com)
 *
 * This file is part of Lasius.
 *
 * Lasius is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Lasius is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Lasius.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 */

import { AppContext, AppProps } from 'next/app';
import Head from 'next/head';
import { ThemeProvider } from 'theme-ui';
import { globalStyles } from 'styles/theme/globalStyles';
import { theme } from 'styles/theme';
import { getSession, SessionProvider } from 'next-auth/react';
import React, { ReactElement, ReactNode } from 'react';
import { appWithTranslation } from 'next-i18next';
import { NextPage } from 'next';
import { BootstrapTasks } from 'components/system/bootstrapTasks';
import { DevInfoBadge } from 'components/system/devInfoBadge';
import { LasiusBackendWebsocketStatus } from 'components/system/lasiusBackendWebsocketStatus';
import { BrowserOnlineStatusCheck } from 'components/system/browserOnlineStatusCheck';
import { LasiusBackendOnlineCheck } from 'components/system/lasiusBackendOnlineCheck';
import { Session } from 'next-auth';
import { BundleVersionCheck } from 'components/system/bundleVersionCheck';
import { LasiusBackendWebsocketEventHandler } from 'components/system/lasiusBackendWebsocketEventHandler';
import { StoreContextProvider, useStore } from 'storeContext/store';
import { Error } from 'components/error';
import {
  DEV,
  LASIUS_TELEMETRY_PLAUSIBLE_SOURCE_DOMAIN,
  SOCIAL_MEDIA_CARD_IMAGE_URL,
} from 'projectConfig/constants';
import { SWRConfig } from 'swr';
import { DefaultSeo } from 'next-seo';
import { swrLogger } from 'lib/api/swrRequestLogger';
import { Toasts } from 'components/toasts/toasts';
import { LazyMotion } from 'framer-motion';
import { getGetUserProfileKey, getUserProfile } from 'lib/api/lasius/user/user';
import { ModelsUser } from 'lib/api/lasius';
import { CookieCutter } from 'components/system/cookieCutter';
import { useAsync } from 'react-async-hook';
import { removeAccessibleCookies } from 'lib/removeAccessibleCookies';
import { getServerSideRequestHeaders } from 'lib/api/hooks/useTokensWithAxiosRequests';
import { logger } from 'lib/logger';
import dynamic from 'next/dynamic';
import PlausibleProvider from 'next-plausible';

export type NextPageWithLayout<P = Record<string, unknown>, IP = P> = NextPage<P, IP> & {
  getLayout?: (page: ReactElement) => ReactNode;
};

type AppPropsWithLayout = AppProps & {
  session: Session;
  Component: NextPageWithLayout;
  initialState: any;
  statusCode: number;
  demandSignout: boolean;
  fallback: Record<string, any>;
  profile: ModelsUser;
};

const loadFeatures = () => import('../lib/framerMotionFeatures.js').then((res) => res.default);

// Enable PWA Updater only in browsers and if workbox object is available
const LasiusPwaUpdater =
  typeof window !== 'undefined' && window?.workbox
    ? dynamic(() => import(`../components/system/lasiusPwaUpdater`), {
        ssr: false,
      })
    : () => <></>;

const App = ({
  Component,
  pageProps,
  statusCode = 0,
  fallback,
  session,
  profile,
}: AppPropsWithLayout): JSX.Element => {
  // Use the layout defined at the page level, if available
  const getLayout = Component.getLayout ?? ((page) => page);
  const lasiusIsLoggedIn = !!(session?.user?.xsrfToken && profile?.id);
  const store = useStore();

  useAsync(async () => {
    if (!lasiusIsLoggedIn) {
      logger.log('[App][UserNotLoggedIn]');
      store.dispatch({ type: 'reset' });
      await removeAccessibleCookies();
    }
  }, [lasiusIsLoggedIn]);

  return (
    <>
      <SWRConfig
        value={{
          ...fallback,
          use: [swrLogger as any],
        }}
      >
        <SessionProvider session={session}>
          <CookieCutter />
          <Head>
            <meta
              name="viewport"
              content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, shrink-to-fit=no"
            />
            <title>Lasius</title>
          </Head>
          <StoreContextProvider>
            <LazyMotion features={loadFeatures}>
              <ThemeProvider theme={theme}>
                <PlausibleProvider
                  domain={LASIUS_TELEMETRY_PLAUSIBLE_SOURCE_DOMAIN}
                  enabled={!!LASIUS_TELEMETRY_PLAUSIBLE_SOURCE_DOMAIN}
                  trackLocalhost={DEV}
                  trackOutboundLinks
                >
                  <DefaultSeo
                    openGraph={{
                      images: [{ url: SOCIAL_MEDIA_CARD_IMAGE_URL }],
                    }}
                  />
                  {globalStyles}
                  {statusCode > 302 ? (
                    <Error statusCode={statusCode} />
                  ) : (
                    getLayout(<Component {...pageProps} />)
                  )}
                  <BrowserOnlineStatusCheck />
                  <LasiusBackendOnlineCheck />
                  <LasiusPwaUpdater />
                  <BundleVersionCheck />
                  <Toasts />
                  {lasiusIsLoggedIn && (
                    <>
                      <BootstrapTasks />
                      <LasiusBackendWebsocketStatus />
                      <LasiusBackendWebsocketEventHandler />
                      <DevInfoBadge />
                    </>
                  )}
                </PlausibleProvider>
              </ThemeProvider>
            </LazyMotion>
          </StoreContextProvider>
        </SessionProvider>
      </SWRConfig>
    </>
  );
};

type ExtendedAppContext = AppContext & {
  ctx: {
    req: Request & {
      useragent: any;
      originalUrl: string;
    };
  };
};

App.getInitialProps = async ({
  Component,
  ctx,
  ctx: { res, req, pathname },
}: ExtendedAppContext) => {
  const session = await getSession({ req });
  let profile = null;
  const token = session?.user?.xsrfToken;

  if (token && session) {
    logger.log('App.getInitialProps', { token, session });
    try {
      profile = await getUserProfile(getServerSideRequestHeaders(token));
    } catch (error) {
      logger.error(error);
      if (res && !pathname.includes('/login')) {
        res.writeHead(307, { Location: '/login' });
        res.end();
      }
    }
  }

  let pageProps = {};
  if (Component.getInitialProps) {
    pageProps = await Component.getInitialProps(ctx);
  }

  return {
    pageProps,
    session,
    profile,
    statusCode: res?.statusCode,
    fallback: {
      ...(profile && { [getGetUserProfileKey().toString()]: profile }),
    },
  };
};

export default appWithTranslation(App as any);
