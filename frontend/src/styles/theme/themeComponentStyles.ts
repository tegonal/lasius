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

import { ThemeUIStyleObject } from 'theme-ui';
import { themeRadii } from 'styles/theme/radii';
import { effects } from 'styles/theme/effects';

export const themeComponentStyles: Record<string, ThemeUIStyleObject> = {
  cardContainer: {
    color: 'containerTextColor',
    background: 'containerBackground',
    borderRadius: themeRadii.large,
    width: ['100%', '375px'],
    p: 4,
    ...effects.shadows.softShadowOnWhite,
  },
  messageBoxWarning: {
    textAlign: 'center',
    background: 'warning',
    width: ['100%', '375px'],
    p: 3,
    borderRadius: themeRadii.small,
  },
  messageBoxInfo: {
    textAlign: 'center',
    background: 'containerBackground',
    width: ['100%', '375px'],
    p: 3,
    borderRadius: themeRadii.small,
  },
};
