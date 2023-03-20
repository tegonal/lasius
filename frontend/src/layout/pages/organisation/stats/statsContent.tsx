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

import { ColumnList } from 'components/shared/columnList';
import { Tabs } from 'components/shared/tabs';
import { StatsCircleCategoryRange } from 'layout/pages/organisation/stats/statsCircleCategoryRange';
import { StatsBarsByAggregatedTags } from 'layout/pages/organisation/stats/statsBarsByAggregatedTags';
import React from 'react';
import { useTranslation } from 'next-i18next';
import { StatsBarsBySource } from 'layout/pages/organisation/stats/statsBarsBySource';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useIsClient } from 'usehooks-ts';

export const StatsContent: React.FC = () => {
  const { t } = useTranslation('common');
  const { selectedOrganisationId } = useOrganisation();
  const isClient = useIsClient();

  if (!isClient) return null;

  const chartTabs = [
    {
      label: t('Tags'),
      component: <StatsBarsBySource source="tag" groupMode="grouped" />,
    },
    {
      label: t('Users'),
      component: <StatsBarsBySource source="user" groupMode="stacked" />,
    },
  ];

  const barChartTabs = [
    {
      label: t('Projects'),
      component: <StatsCircleCategoryRange source="project" />,
    },
    {
      label: t('Users'),
      component: <StatsCircleCategoryRange source="user" />,
    },
  ];

  return (
    <ColumnList key={selectedOrganisationId}>
      <Tabs tabs={chartTabs} />
      <Tabs tabs={barChartTabs} />
      <StatsBarsByAggregatedTags />
    </ColumnList>
  );
};
