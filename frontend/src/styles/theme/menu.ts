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

const aHoverUnderline: ThemeUIStyleObject = {
  'a, a:link': {
    textDecoration: 'none',
  },
  'a:hover, a:active': {
    textDecoration: 'underline',
  },
};

const horizontalItem: ThemeUIStyleObject = {
  textTransform: 'uppercase',
  fontSize: 2,
  padding: 2,
  fontWeight: 'body',
  color: 'text',
  ...aHoverUnderline,
};

const horizontalItemSub: ThemeUIStyleObject = {
  textTransform: 'uppercase',
  fontSize: 2,
  padding: 2,
  fontWeight: 'body',
  color: 'text',
  ...aHoverUnderline,
};

const verticalItem: ThemeUIStyleObject = {
  textTransform: 'uppercase',
  py: 2,
  fontWeight: 'body',
  color: 'text',
  ...aHoverUnderline,
};

const verticalItemSub: ThemeUIStyleObject = {
  textTransform: 'uppercase',
  fontSize: 2,
  marginLeft: 3,
  py: 2,
  fontWeight: 'body',
  color: 'text',
  ...aHoverUnderline,
};

const overlayVertical: ThemeUIStyleObject = {
  textTransform: 'uppercase',
  py: 3,
  fontWeight: 'body',
  color: 'text',
  textAlign: 'center',
  borderBottom: '1px solid var(--theme-ui-colors-muted)',
  ...aHoverUnderline,
};

const overlayVerticalSub: ThemeUIStyleObject = {
  textTransform: 'uppercase',
  fontSize: 2,
  marginLeft: 3,
  py: 3,
  fontWeight: 'body',
  color: 'text',
  textAlign: 'center',
  ...aHoverUnderline,
};

export const themeMenu: Record<string, ThemeUIStyleObject> = {
  horizontalOn: {
    ...horizontalItem,
    fontWeight: 'bold',
  },
  horizontalOff: {
    ...horizontalItem,
  },
  horizontalSubOn: {
    ...horizontalItemSub,
    fontWeight: 'bold',
  },
  horizontalSubOff: {
    ...horizontalItemSub,
  },
  verticalOn: {
    ...verticalItem,
    fontWeight: 'bold',
  },
  verticalOff: {
    ...verticalItem,
  },
  verticalSubOn: {
    ...verticalItemSub,
    fontWeight: 'bold',
  },
  verticalSubOff: {
    ...verticalItemSub,
  },
  overlayVerticalOn: {
    ...overlayVertical,
    fontWeight: 'bold',
  },
  overlayVerticalOff: {
    ...overlayVertical,
  },
  overlayVerticalSubOn: {
    ...overlayVerticalSub,
    fontWeight: 'bold',
  },
  overlayVerticalSubOff: {
    ...overlayVerticalSub,
  },
};
