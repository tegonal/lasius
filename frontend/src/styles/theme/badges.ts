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
import { flexRowJustifyStartAlignCenter } from 'styles/shortcuts';

const badgeDefault: ThemeUIStyleObject = {
  label: 'Badge',
  fontSize: 1,
  fontWeight: 700,
  letterSpacing: 'normal',
  lineHeight: 'normal',
  px: '11px',
  pt: '4px',
  pb: '4px',
  borderRadius: '16px',
  display: 'flex',
  ...flexRowJustifyStartAlignCenter(2),
};

const clickableBadge: ThemeUIStyleObject = {
  label: 'ClickableBadge',
  '&:hover': {
    color: 'negativeText',
    bg: 'selection',
    cursor: 'pointer',
  },
};

export const themeBadges: Record<string, ThemeUIStyleObject> = {
  primary: {
    ...badgeDefault,
    color: 'text',
    bg: 'containerBackgroundDarker',
  },
  muted: {
    variant: 'badges.primary',
    bg: 'muted',
  },
  tag: {
    variant: 'badges.primary',
    overflow: 'visible',
    color: 'tagText',
    bg: 'tagBackground',
  },
  tagSimpleTag: {
    variant: 'badges.primary',
    color: 'tagText',
    bg: 'tagBackground',
  },
  tagSimpleTagClickable: {
    variant: 'badges.tagSimpleTag',
    ...clickableBadge,
  },
  tagTagGroup: {
    variant: 'badges.tagSimpleTag',
    color: 'negativeText',
    bg: 'accentColors.1',
  },
  tagTagGroupClickable: {
    variant: 'badges.tagTagGroup',
    ...clickableBadge,
  },
  tagWithSummary: {
    variant: 'badges.tagSimpleTag',
    color: 'negativeText',
    bg: 'accentColors.2',
  },
  tagWithSummaryClickable: {
    variant: 'badges.tagWithSummary',
    ...clickableBadge,
  },
  tooltip: {
    variant: 'badges.primary',
    background: 'black',
    color: 'negativeText',
    whiteSpace: 'break-spaces',
    borderRadius: 4,
    px: 3,
    py: 2,
    maxWidth: '38ch',
  },
  warning: {
    variant: 'badges.primary',
    color: 'negativeText',
    bg: 'warning',
  },
  outline: {
    color: 'text',
    bg: 'transparent',
    boxShadow: 'inset 0 0 0 1px',
  },
};
