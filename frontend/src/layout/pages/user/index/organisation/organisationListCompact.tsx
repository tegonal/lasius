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
import { OrganisationListWrapper } from 'layout/pages/user/index/organisation/organisationListWrapper';
import { OrganisationItem } from 'layout/pages/user/index/organisation/organisationItem';
import { useGetUserBookingCurrentListByOrganisation } from 'lib/api/lasius/user-bookings/user-bookings';
import { DataFetchEmpty } from 'components/shared/fetchState/dataFetchEmpty';
import { DataFetchValidates } from 'components/shared/fetchState/dataFetchValidates';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { stringHash } from 'lib/stringHash';
import { AnimateList } from 'components/shared/motion/animateList';

export const OrganisationListCompact: React.FC = () => {
  const { selectedOrganisationId } = useOrganisation();

  const { data, isValidating } = useGetUserBookingCurrentListByOrganisation(selectedOrganisationId);

  const hasNoData = !data || data?.timeBookings.length === 0;

  return (
    <OrganisationListWrapper>
      <DataFetchValidates isValidating={isValidating} />

      {hasNoData ? (
        <DataFetchEmpty />
      ) : (
        <AnimateList popLayout>
          {data?.timeBookings.map((item) => (
            <OrganisationItem key={stringHash(item)} item={item} />
          ))}
        </AnimateList>
      )}
    </OrganisationListWrapper>
  );
};
