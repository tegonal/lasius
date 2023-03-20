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
import { BookingStart } from 'layout/pages/user/index/bookingStart';
import { useTranslation } from 'next-i18next';
import { OrganisationListCompact } from 'layout/pages/user/index/organisation/organisationListCompact';
import { BookingAddButton } from 'layout/pages/user/index/bookingAddButton';
import { FormElementSpacer } from 'components/forms/formElementSpacer';
import { IconTabs, IconTabsItem } from 'components/shared/iconTabs';
import { Flex } from 'theme-ui';
import { flexColumnJustifyCenterAlignCenter } from 'styles/shortcuts';
import { FavoriteListCompact } from 'layout/pages/user/index/favorites/favoriteListCompact';

export const IndexColumnTabs: React.FC = () => {
  const { t } = useTranslation('common');

  const tabs: IconTabsItem[] = [
    {
      id: 'bookingStart',
      name: t('Start booking'),
      component: (
        <Flex
          sx={{
            px: [2, 3],
            py: 4,
            ...flexColumnJustifyCenterAlignCenter([1, 4]),
            width: '100%',
          }}
        >
          <BookingStart />
          <FormElementSpacer />
          <BookingAddButton />
        </Flex>
      ),
      icon: 'stopwatch-interface-essential',
    },
    {
      id: 'bookingStartFav',
      name: t('Start booking from favorite'),
      component: <FavoriteListCompact />,
      icon: 'rating-star-social-medias-rewards-rating',
    },
    {
      id: 'bookingStartTeam',
      name: t('Start booking from team member'),
      component: <OrganisationListCompact />,
      icon: 'human-resources-search-team-work-office-companies',
    },
  ];

  return <IconTabs tabs={tabs} />;
};
