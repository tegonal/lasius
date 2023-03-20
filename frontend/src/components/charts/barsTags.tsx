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
import { NivoChartDataType } from 'types/common';
import { colorPalette2 } from 'styles/theme/colors';
import { nivoTheme } from 'components/charts/nivoTheme';

type Props = {
  stats: { data: NivoChartDataType | undefined };
};

const BarsTags: React.FC<Props> = ({ stats }) => {
  const { data } = stats;
  if (!data) return null;
  return (
    <ResponsiveBar
      data={data}
      theme={nivoTheme}
      indexBy="id"
      layout="horizontal"
      enableGridX
      enableGridY={false}
      colors={colorPalette2}
      margin={{ top: 60, right: 50, bottom: 60, left: 110 }}
      padding={0.3}
      valueScale={{ type: 'linear' }}
      indexScale={{ type: 'band', round: true }}
      borderColor={{
        from: 'color',
        modifiers: [['brighter', 2]],
      }}
      cornerRadius={3}
      axisTop={{
        tickSize: 5,
        tickPadding: 5,
        tickRotation: 0,
      }}
      axisRight={null}
      axisBottom={{
        tickSize: 5,
        tickPadding: 5,
        tickRotation: 0,
      }}
      axisLeft={{
        tickSize: 5,
        tickPadding: 5,
        tickRotation: 0,
      }}
      labelSkipWidth={40}
      labelSkipHeight={16}
      labelTextColor={{
        from: 'color',
        modifiers: [['brighter', 4]],
      }}
      tooltipLabel={(item) => `${item.indexValue}`}
    />
  );
};

export default BarsTags;
