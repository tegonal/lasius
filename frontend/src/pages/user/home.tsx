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

import { HomeLayoutDesktop } from 'layout/pages/user/index/homeLayoutDesktop';
import { HomeLayoutMobile } from 'layout/pages/user/index/homeLayoutMobile';
import { NextPageWithLayout } from 'pages/_app';
import { GetServerSideProps } from 'next';
import { serverSideTranslations } from 'next-i18next/serverSideTranslations';
import { LayoutMobile } from 'layout/layoutMobile';
import { LayoutDesktop } from 'layout/layoutDesktop';

type DeviceType = {
  deviceType: 'desktop' | 'mobile';
};

const Home: NextPageWithLayout<DeviceType> = (context) => {
  if (context.deviceType === 'mobile') {
    return <HomeLayoutMobile />;
  }
  return <HomeLayoutDesktop />;
};

export const getServerSideProps: GetServerSideProps = async (context) => {
  const { locale = '' } = context;
  const UA = context.req.headers['user-agent'];
  const isMobile = Boolean(
    UA?.match(/Android|BlackBerry|iPhone|iPod|Opera Mini|IEMobile|WPDesktop/i)
  );

  return {
    props: {
      deviceType: isMobile ? 'mobile' : 'desktop',
      ...(await serverSideTranslations(locale, ['common'])),
    },
  };
};

Home.getLayout = function getLayout(page) {
  if (page.props.deviceType === 'mobile') {
    return <LayoutMobile>{page}</LayoutMobile>;
  }
  return <LayoutDesktop>{page}</LayoutDesktop>;
};

export default Home;
