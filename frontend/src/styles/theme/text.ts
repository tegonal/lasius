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

import { darken } from '@theme-ui/color';
import { ThemeUIStyleObject } from 'theme-ui';
import { flexRowJustifyBetweenAlignCenter } from 'styles/shortcuts';

export const defaultAnchorStyle: ThemeUIStyleObject = {
  'a, a:link': {
    color: 'text',
  },
  'a:hover, a:active, a:visited': {
    color: 'text',
    textDecoration: 'none',
  },
};

export const themeText: Record<string, ThemeUIStyleObject> = {
  paragraph: {
    fontSize: 2,
    paddingBottom: 3,
    ...defaultAnchorStyle,
  },
  normal: {
    // default
    fontSize: 2,
    paddingBottom: 3,
    ...defaultAnchorStyle,
  },
  lead: {
    fontSize: [3, 4],
    ...defaultAnchorStyle,
  },
  footnote: {
    fontSize: 1,
    opacity: 0.5,
    ...defaultAnchorStyle,
  },
  small: {
    fontSize: 1,
    opacity: 0.5,
    ...defaultAnchorStyle,
  },
  caption: {
    fontSize: 2,
    paddingBottom: 3,
    ...defaultAnchorStyle,
  },
  infoText: {
    fontSize: 2,
    mb: 4,
    ...defaultAnchorStyle,
  },
  heading: {
    // default
    paddingTop: 3,
    paddingBottom: 2,
    letterSpacing: 0.15,
    fontSize: 3,
    fontWeight: 400,
    borderBottom: '1px solid',
    borderBottomColor: 'containerTextColorMuted',
    width: '100%',
    mb: [2, 4],
  },
  headingUnderlined: {
    fontSize: 2,
    borderBottom: '1px solid',
    borderBottomColor: 'containerTextColorMuted',
    width: '100%',
    color: 'containerTextColor',
    display: 'flex',
    ...flexRowJustifyBetweenAlignCenter(),
    pb: 2,
    mb: 3,
  },
  headingUnderlinedMuted: {
    fontSize: 1,
    borderBottom: '1px solid',
    borderBottomColor: 'containerTextColorMuted',
    width: '100%',
    color: 'containerTextColorMuted',
    mb: 4,
  },
  headingTableHeader: {
    fontSize: 1,
    fontWeight: 400,
    mb: 1,
  },
  label: {
    fontSize: 1,
    mb: 2,
  },
  footer: {
    fontSize: 1,
    color: darken('muted', 0.25),
    textAlign: 'center',
    lineHeight: 1.1,
    'a, a:link': {
      color: darken('muted', 0.25),
    },
    'a:hover, a:active, a:visited': {
      color: darken('muted', 0.25),
      textDecoration: 'none',
    },
  },
};
