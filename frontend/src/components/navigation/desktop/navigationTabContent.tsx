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

import React from 'react';
import { Box, Flex } from 'theme-ui';
import { Button } from '@theme-ui/components';
import { Icon } from 'components/shared/icon';
import { useTranslation } from 'next-i18next';
import { flexColumnJustifyStartAlignStart } from 'styles/shortcuts';
import { useRouter } from 'next/router';
import { getNavigation, NavigationRouteType } from 'projectConfig/routes';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useIsClient } from 'usehooks-ts';

const NavigationButton: React.FC<{ item: NavigationRouteType }> = ({ item }) => {
  const router = useRouter();
  const { t } = useTranslation('common');
  const isClient = useIsClient();
  if (!isClient) return null;

  return (
    <Button
      key={item.name}
      sx={{ label: 'NavigationButton', textAlign: 'left', lineHeight: '1.2rem' }}
      variant={router.route === item.route ? 'iconTextActive' : 'iconText'}
      onClick={() => router.push(item.route)}
    >
      <Icon name={item.icon} size={24} />
      <Box>{t(item.name as any)}</Box>
    </Button>
  );
};

type Props = {
  branch: string;
};

export const NavigationTabContent: React.FC<Props> = ({ branch }) => {
  const { isAdministrator } = useOrganisation();

  return (
    <Flex sx={{ ...flexColumnJustifyStartAlignStart(3) }}>
      {getNavigation({
        id: branch,
        isOrganisationAdministrator: isAdministrator,
      }).map((item) => (
        <NavigationButton key={item.name} item={item} />
      ))}
    </Flex>
  );
};
