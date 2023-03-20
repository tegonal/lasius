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

const sc = 'hsl(0deg 0% 0% / 0.075)';
const scSoft = 'hsl(0deg 0% 0% / 0.04)';

export const effects = {
  transition: {
    background: {
      transition: 'background 0.5s ease',
    },
  },
  color: {
    desaturate: {
      filter: 'saturate(33%)',
    },
  },
  shadows: {
    softShadowOnWhite: {
      boxShadow: `0 1px 1px ${sc}, 0 2px 2px ${sc}, 0 4px 4px ${sc}, 0 8px 8px ${sc}, 0 16px 16px ${sc}`,
    },
    softShadowOnWhiteUp: {
      boxShadow: `0 -1px 1px ${scSoft}, 0 -2px 2px ${scSoft}, 0 -4px 4px ${scSoft}, 0 -8px 8px ${scSoft}, 0 -16px 16px ${scSoft}`,
    },
    softShadowOnWhiteSmall: {
      boxShadow: `0 1px 1px ${sc}, 0 2px 2px ${sc}, 0 3px 3px ${sc}, 0 5px 5px ${sc}, 0 8px 8px ${sc}`,
    },
    softShadowOnDark: {
      boxShadow: `0 1px 1px ${sc}, 0 2px 2px ${sc}, 0 4px 4px ${sc}, 0 8px 8px ${sc}, 0 16px 16px ${sc}`,
    },
  },
};
