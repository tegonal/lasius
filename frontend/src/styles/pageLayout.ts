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

/* eslint-disable prefer-destructuring */
const baseCol = (width: number, cols: number) => {
  return Math.ceil((width / 12) * cols);
};

type ThemePageLayoutType = { [key: string]: { [key: string]: any[] } };

export const themePageLayout: ThemePageLayoutType = {
  default: {
    page: ['100%', baseCol(720, 12), baseCol(960, 12)],
    grid: ['auto'],
  },
  noSidebar: {
    page: ['100%', baseCol(720, 12), baseCol(960, 12)],
    grid: ['auto'],
  },
  sidebarLeft: {
    page: ['100%', baseCol(720, 12), baseCol(960, 12)],
    grid: ['auto', `${baseCol(720, 3)} 1fr`],
  },
  sidebarRight: {
    page: ['100%', baseCol(720, 12), baseCol(960, 12)],
    grid: ['auto', ` 1fr ${baseCol(720, 3)}`],
  },
  sidebarLeftRight: {
    // TODO: implement
    page: ['100%', baseCol(720, 12), baseCol(960, 12)],
    grid: ['auto', `${baseCol(720, 3)} 1fr ${baseCol(720, 3)}`],
  },
};
