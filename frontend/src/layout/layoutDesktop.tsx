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

import React, { Suspense } from 'react';
import { Box, Flex } from 'theme-ui';
import { HeaderDesktop } from 'layout/headerDesktop';
import { themeRadii } from 'styles/theme/radii';
import { fullWidthHeight, layoutColumnStyle } from 'styles/shortcuts';
import { NavigationMenuTabs } from 'components/navigation/desktop/navigationMenuTabs';
import { ContainerColumnsDesktop } from 'layout/containerColumnsDesktop';
import { PageLayoutResponsive } from 'layout/pageLayoutResponsive';
import { effects } from 'styles/theme/effects';
import { Loading } from 'components/shared/fetchState/loading';

type Props = {
  children: React.ReactNode;
};

export const LayoutDesktop: React.FC<Props> = ({ children }) => {
  return (
    <PageLayoutResponsive>
      <HeaderDesktop />
      <Box
        as="section"
        sx={{
          background: 'backgroundContainer',
          borderTopLeftRadius: themeRadii.large,
          borderTopRightRadius: themeRadii.large,
          ...fullWidthHeight(),
          overflow: 'auto',
          borderTop: '1px solid',
          borderRight: '1px solid',
          borderBottom: '0px solid',
          borderLeft: '1px solid',
          borderColor: 'muted',
          ...effects.shadows.softShadowOnWhiteUp,
        }}
      >
        <ContainerColumnsDesktop>
          <Flex
            sx={{
              ...layoutColumnStyle,
              border: 'none',
              borderTopLeftRadius: themeRadii.large,
            }}
          >
            <NavigationMenuTabs />
          </Flex>
          <Suspense fallback={<Loading />}>{children}</Suspense>
        </ContainerColumnsDesktop>
      </Box>
    </PageLayoutResponsive>
  );
};
