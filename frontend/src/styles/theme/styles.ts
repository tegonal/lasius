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

import { ThemeStyles } from '@theme-ui/css';
import { themeMenu } from './menu';
import { themeComponentStyles } from 'styles/theme/themeComponentStyles';
import { themeRadii } from 'styles/theme/radii';

export const themeStyles: ThemeStyles = {
  root: {
    fontFamily: 'body',
    lineHeight: 'body',
    fontWeight: 'body',
    fontSize: '16px',
    letterSpacing: 0.2,
    MozOsxFontSmoothing: 'grayscale',
    WebkitFontSmoothing: 'antialiased',
  },
  h1: {
    color: 'text',
    fontFamily: 'heading',
    lineHeight: 'heading',
    fontWeight: 'heading',
    fontSize: [4, 5],
    paddingTop: [4, 5],
    paddingBottom: [3, 4],
    letterSpacing: 0.15,
  },
  h2: {
    color: 'text',
    fontFamily: 'heading',
    lineHeight: 'heading',
    fontWeight: 'heading',
    fontSize: [3, 4],
    paddingTop: 3,
    paddingBottom: 3,
    letterSpacing: 0.15,
  },
  h3: {
    color: 'text',
    fontFamily: 'heading',
    lineHeight: 'heading',
    fontWeight: 'heading',
    fontSize: 3,
    paddingTop: 3,
    paddingBottom: 3,
    letterSpacing: 0.15,
  },
  h4: {
    color: 'text',
    fontFamily: 'heading',
    lineHeight: 'heading',
    fontWeight: 'heading',
    fontSize: 2,
    paddingTop: 3,
    paddingBottom: 3,
    letterSpacing: 0.15,
  },
  h5: {
    color: 'text',
    fontFamily: 'heading',
    lineHeight: 'heading',
    fontWeight: 'heading',
    fontSize: 1,
    paddingTop: 3,
    paddingBottom: 3,
    letterSpacing: 0.15,
  },
  h6: {
    color: 'text',
    fontFamily: 'heading',
    lineHeight: 'heading',
    fontWeight: 'heading',
    fontSize: 0,
  },
  p: {
    color: 'text',
    fontFamily: 'body',
    fontWeight: 'body',
    lineHeight: 'body',
    paddingBottom: 3,
    fontSize: [2, 3],
  },
  a: {
    color: 'primary',
    '&:visited, &:link': {
      color: 'inherit',
      outline: 0,
    },
    '&:active, &:hover': {
      color: 'primary',
      textDecoration: 'none',
      cursor: 'pointer',
      WebkitTouchCallout: 'none',
      msTouchSelect: 'none',
    },
  },
  pre: {
    fontFamily: 'monospace',
    overflowX: 'auto',
    code: {
      color: 'inherit',
    },
  },
  code: {
    fontFamily: 'monospace',
    fontSize: 1,
    background: 'containerBackgroundLighter',
    p: 2,
    borderRadius: themeRadii.small,
  },
  table: {
    width: '100%',
    borderCollapse: 'separate',
    borderSpacing: 0,
  },
  th: {
    textAlign: 'left',
    borderBottomStyle: 'solid',
  },
  td: {
    textAlign: 'left',
    borderBottomStyle: 'solid',
  },
  img: {
    maxWidth: '100%',
  },
  hr: {
    borderColor: 'soft',
  },
  ...themeComponentStyles,
  ...themeMenu,
};
