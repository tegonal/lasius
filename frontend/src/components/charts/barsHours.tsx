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

// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-nocheck

import { ResponsiveBar } from '@nivo/bar';
import React from 'react';
import { line } from 'd3-shape';
import { nivoTheme } from 'components/charts/nivoTheme';
import { NivoChartDataType } from 'types/common';
import { useTranslation } from 'next-i18next';
import { colorPalette3 } from 'styles/theme/colors';

const Line =
  (props: { category: string; value: number }[]) =>
  (layerProps: { bars: any; xScale: any; yScale: any }) => {
    const { xScale, yScale } = layerProps;

    const lineBegins = line()
      .x((item) => xScale(item.category) + layerProps.bars[0].width / 2)
      .y((item) => yScale(item.value));

    return (
      <path
        d={lineBegins(props)}
        fill="none"
        stroke="var(--theme-ui-colors-selection)"
        strokeWidth={1}
        style={{ pointerEvents: 'none' }}
      />
    );
  };

export type BarChartGroupMode = 'grouped' | 'stacked';

type Props = {
  stats: { data: NivoChartDataType; keys: string[]; ceilingData: NivoChartDataType } | undefined;
  indexBy: string;
  groupMode: BarChartGroupMode;
};

const BarsHours: React.FC<Props> = ({ stats, indexBy, groupMode }) => {
  const { t } = useTranslation('common');
  const { data, keys, ceilingData } = stats;
  if (!data) return null;
  return (
    <ResponsiveBar
      data={data}
      keys={keys}
      indexBy={indexBy}
      theme={nivoTheme}
      colors={colorPalette3}
      groupMode={groupMode}
      margin={{ top: 25, right: 30, bottom: 60, left: 60 }}
      padding={0.3}
      valueScale={{ type: 'linear' }}
      indexScale={{ type: 'band', round: true }}
      borderColor={{
        from: 'color',
        modifiers: [['brighter', 2]],
      }}
      cornerRadius={3}
      axisTop={null}
      axisRight={null}
      axisBottom={{
        tickSize: 5,
        tickPadding: 5,
        tickRotation: 0,
        legend: t('Date'),
        legendPosition: 'middle',
        legendOffset: 40,
      }}
      axisLeft={{
        tickSize: 5,
        tickPadding: 5,
        tickRotation: 0,
        legend: t('Hours'),
        legendPosition: 'middle',
        legendOffset: -40,
      }}
      labelSkipWidth={20}
      labelSkipHeight={14}
      labelTextColor={{
        from: 'color',
        modifiers: [['brighter', 5]],
      }}
      tooltipLabel={(item) => `${item.id}`}
      layers={['axes', 'grid', 'bars', 'legends', Line(ceilingData)]}
    />
  );
};

export default BarsHours;
