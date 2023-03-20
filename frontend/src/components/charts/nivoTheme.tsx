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

export const nivoTheme = {
  background: 'transparent',
  textColor: 'var(--theme-ui-colors-containerTextColorMuted)',
  fontSize: 11,
  axis: {
    domain: {
      line: {
        stroke: 'var(--theme-ui-colors-containerTextColor)',
        strokeWidth: 1,
      },
    },
    legend: {
      text: {
        fontSize: 12,
        fill: 'var(--theme-ui-colors-containerTextColor)',
      },
    },
    ticks: {
      line: {
        stroke: 'var(--theme-ui-colors-containerTextColorMuted)',
        strokeWidth: 1,
      },
      text: {
        fontSize: 11,
        fill: 'var(--theme-ui-colors-containerTextColor)',
      },
    },
  },
  grid: {
    line: {
      stroke: 'var(--theme-ui-colors-containerTextColorMuted)',
      strokeWidth: 1,
    },
  },
  legends: {
    title: {
      text: {
        fontSize: 11,
        fill: 'var(--theme-ui-colors-containerTextColorMuted)',
      },
    },
    text: {
      fontSize: 11,
      fill: 'var(--theme-ui-colors-containerTextColorMuted)',
    },
    ticks: {
      line: {},
      text: {
        fontSize: 10,
        fill: 'var(--theme-ui-colors-containerTextColorMuted)',
      },
    },
  },
  annotations: {
    text: {
      fontSize: 13,
      fill: '#333333',
      outlineWidth: 2,
      outlineColor: '#ffffff',
      outlineOpacity: 1,
    },
    link: {
      stroke: '#000000',
      strokeWidth: 1,
      outlineWidth: 2,
      outlineColor: '#ffffff',
      outlineOpacity: 1,
    },
    outline: {
      stroke: '#000000',
      strokeWidth: 2,
      outlineWidth: 2,
      outlineColor: '#ffffff',
      outlineOpacity: 1,
    },
    symbol: {
      fill: '#000000',
      outlineWidth: 2,
      outlineColor: '#ffffff',
      outlineOpacity: 1,
    },
  },
  tooltip: {
    container: {
      background: 'var(--theme-ui-colors-text)',
      color: 'var(--theme-ui-colors-background)',
      fontSize: 12,
    },
    basic: {},
    chip: {},
    table: {},
    tableCell: {},
    tableCellValue: {},
  },
};
