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
import { ModelsTag } from 'lib/api/lasius';

type Props = {
  tags: ModelsTag[];
};

export const TagGroupList: React.FC<Props> = ({ tags }) => {
  const { t } = useTranslation('common');

  return (
    <>
      <DataList>
        <DataListRow>
          <DataListHeaderItem>{t('Organisations')}</DataListHeaderItem>
          <DataListHeaderItem>{t('Organisations')}</DataListHeaderItem>
          <DataListHeaderItem />
        </DataListRow>
        {tags.map((item) => (
          <DataListRow key={item.id}>
            <DataListField>
              <Text>{item.id}</Text>
            </DataListField>
            <DataListField>
              <Text>{item.type}</Text>
            </DataListField>
            <DataListField>{/* <ProjectsListItemContext item={item} /> */}</DataListField>
          </DataListRow>
        ))}
      </DataList>
    </>
  );
};
