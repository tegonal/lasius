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
import { useTranslation } from 'next-i18next';
import { DataList } from 'components/dataList/dataList';
import { DataListHeaderItem } from 'components/dataList/dataListHeaderItem';
import { DataListField } from 'components/dataList/dataListField';
import { Text } from '@theme-ui/components';
import { DataListRow } from 'components/dataList/dataListRow';
import { AllProjectsListItemContext } from 'layout/pages/organisation/projects/allProjectsListItemContext';
import { AvatarProject } from 'components/shared/avatar/avatarProject';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useGetProjectList } from 'lib/api/lasius/projects/projects';
import { orderBy } from 'lodash';
import { useIsClient } from 'usehooks-ts';
import { DataFetchEmpty } from 'components/shared/fetchState/dataFetchEmpty';
import { stringHash } from 'lib/stringHash';

export const AllProjectsList: React.FC = () => {
  const { t } = useTranslation('common');
  const { selectedOrganisationId } = useOrganisation();
  const { data } = useGetProjectList(selectedOrganisationId);
  const isClient = useIsClient();

  if (!isClient) return null;

  if (!data || data?.length === 0) {
    return <DataFetchEmpty />;
  }

  return (
    <>
      <DataList>
        <DataListRow>
          <DataListHeaderItem />
          <DataListHeaderItem>{t('Name')}</DataListHeaderItem>
          <DataListHeaderItem>{t('Status')}</DataListHeaderItem>
          <DataListHeaderItem />
        </DataListRow>
        {orderBy(data, (data) => data.key).map((item) => (
          <DataListRow key={stringHash(item)}>
            <DataListField width={90}>
              <AvatarProject name={item.key} />
            </DataListField>
            <DataListField>
              <Text>{item.key}</Text>
            </DataListField>
            <DataListField>
              <Text>{item.active ? t('Active') : t('Inactive')}</Text>
            </DataListField>
            <DataListField>
              <AllProjectsListItemContext item={item} />
            </DataListField>
          </DataListRow>
        ))}
      </DataList>
    </>
  );
};
