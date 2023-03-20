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
import { AvatarProject } from 'components/shared/avatar/avatarProject';
import { ROLES } from 'projectConfig/constants';
import { MyProjectsListItemMemberContext } from 'layout/pages/projects/my/myProjectsListItemMemberContext';
import { UserRoles } from 'dynamicTranslationStrings';
import { MyProjectsListItemAdministratorContext } from 'layout/pages/projects/my/myProjectsListItemAdministratorContext';
import { useProjects } from 'lib/api/hooks/useProjects';
import { DataFetchEmpty } from 'components/shared/fetchState/dataFetchEmpty';
import { useIsClient } from 'usehooks-ts';
import { stringHash } from 'lib/stringHash';

export const MyProjectsList: React.FC = () => {
  const { t } = useTranslation('common');
  const { userProjects } = useProjects();
  const isClient = useIsClient();

  if (!isClient) return null;

  if (userProjects().length === 0) {
    return <DataFetchEmpty />;
  }

  return (
    <>
      <DataList>
        <DataListRow>
          <DataListHeaderItem />
          <DataListHeaderItem>{t('Name')}</DataListHeaderItem>
          <DataListHeaderItem>{t('Role')}</DataListHeaderItem>
          <DataListHeaderItem />
        </DataListRow>
        {userProjects().map((item) => (
          <DataListRow key={stringHash(item)}>
            <DataListField width={90}>
              <AvatarProject name={item.projectReference.key} />
            </DataListField>
            <DataListField>
              <Text>{item.projectReference.key}</Text>
            </DataListField>
            <DataListField>
              <Text>{UserRoles[item.role]}</Text>
            </DataListField>
            <DataListField>
              {item.role === ROLES.PROJECT_ADMIN ? (
                <MyProjectsListItemAdministratorContext item={item} />
              ) : (
                <MyProjectsListItemMemberContext item={item} />
              )}
            </DataListField>
          </DataListRow>
        ))}
      </DataList>
    </>
  );
};
