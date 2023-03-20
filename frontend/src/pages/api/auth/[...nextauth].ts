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

import CredentialsProvider from 'next-auth/providers/credentials';

import NextAuth, { NextAuthOptions } from 'next-auth';
import { NextApiRequest, NextApiResponse } from 'next';
import { logger } from 'lib/logger';
import { signIn } from 'lib/api/lasius/authentication/authentication';
import { getUserProfile, updateUserSettings } from 'lib/api/lasius/user/user';
import { getServerSideRequestHeaders } from 'lib/api/hooks/useTokensWithAxiosRequests';

const nextAuthOptions = (): NextAuthOptions => {
  return {
    providers: [
      CredentialsProvider({
        id: 'credentials',
        name: 'Basic auth on Lasius',
        credentials: {
          email: { label: 'E-Mail', type: 'text', placeholder: 'E-Mail' },
          password: { label: 'Password', type: 'password' },
        },
        async authorize(credentials) {
          if (!credentials?.email || !credentials?.password) {
            throw new Error('oneOfCredentialsMissing');
          }

          const { email, password } = credentials;

          let signInResponse;

          try {
            signInResponse = await signIn({ email, password });
          } catch (error) {
            logger.error('[nextauth][signInFailed]', error);
          }

          if (!signInResponse?.token) {
            logger.log('usernameOrPasswordWrong');
            throw new Error('usernameOrPasswordWrong');
          }

          if (signInResponse.token) {
            const { token: xsrfToken } = signInResponse;

            const user = await getUserProfile(getServerSideRequestHeaders(xsrfToken));

            //  TODO: remove this once the backend sets a lastselectedorganisation on profile creation
            if (!user.settings.lastSelectedOrganisation) {
              await updateUserSettings(
                {
                  lastSelectedOrganisation: user.organisations.filter((item) => item.private)[0]
                    .organisationReference,
                },
                getServerSideRequestHeaders(xsrfToken)
              );
            }

            if (!user) {
              logger.log('noUserFoundOnBackend', user);
              throw new Error('noUserFoundOnBackend');
            }

            const { id, key: name, email } = user;

            if (id) {
              return {
                id,
                name,
                email,
                // Hack: We need to store the xsrfToken in the user object. next auth strips unknown properties ...
                image: xsrfToken,
                xsrfToken,
              };
            }
          }

          return null;
        },
      }),
    ],
    pages: {
      signIn: '/login',
      signOut: '/',
    },
    callbacks: {
      async session({ session }) {
        return {
          ...session,
          user: { ...session.user, image: undefined, xsrfToken: session.user?.image || '' },
        };
      },
    },
    events: {
      async signOut() {
        logger.info('[nextauth][events][signOut]');
      },
      async signIn() {
        logger.info('[nextauth][events][signIn]');
      },
    },
  };
};

// eslint-disable-next-line
export default (req: NextApiRequest, res: NextApiResponse) => {
  // eslint-disable-next-line @typescript-eslint/ban-ts-comment
  // @ts-ignore
  return NextAuth(req, res, nextAuthOptions(req, res));
};
