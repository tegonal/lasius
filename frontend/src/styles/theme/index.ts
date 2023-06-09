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

import { themeStyles } from './styles';
import { Theme } from '@theme-ui/css';
import { themeText } from './text';
import { themeBadges } from './badges';
import { themeButtons } from './buttons';
import { themeColors } from './colors';
import { themeRadii } from 'styles/theme/radii';
import { lighten } from '@theme-ui/color';
import { outline, resetAutoFillStyle } from 'styles/shortcuts';

export const theme: Theme = {
  breakpoints: ['540px', '720px', '960px', '1140px', '1440px', '1680px'], // 540px 720px 960px 1140px (bootstrap 4)
  space: [0, 4, 8, 16, 32, 64, 128, 256, 512],
  fonts: {
    body: 'Roboto, "Open Sans", sans-serif',
    heading: 'Roboto, "Open Sans", sans-serif',
    monospace: 'Menlo, monospace',
  },
  fontSizes: ['0.5rem', '0.75rem', '1rem', '1.2rem', '1.5rem', '2rem'],
  fontWeights: {
    body: 400,
    heading: 700,
    bold: 700,
  },
  lineHeights: {
    body: 1.666,
    heading: 1.3333,
  },
  radii: themeRadii,
  colors: themeColors,
  text: themeText,
  badges: themeBadges,
  buttons: themeButtons,
  styles: themeStyles,
  forms: {
    input: {
      border: 'none',
      bg: 'containerBackgroundLighter',
      color: 'containerTextColor',
      '::placeholder': {
        color: 'containerTextColor',
      },
      '&:focus': {
        outline: 'none',
      },
      py: 1,
      ...resetAutoFillStyle(),
      ...outline(),
    },
    select: {
      border: 'none',
      bg: 'containerBackgroundLighter',
      color: 'containerTextColor',
      '::placeholder': {
        color: lighten('containerTextColor', 0.4),
      },
      py: 1,
      ...resetAutoFillStyle(),
      ...outline(),
    },
    label: {
      fontSize: 1,
      fontWeight: 500,
      ml: 1,
    },
  },
  alerts: {
    info: {
      variant: 'alerts.default',
      bg: 'muted',
      color: 'text',
      fontWeight: 400,
    },
    'toast-SUCCESS': {
      variant: 'alerts.default',
      justifyContent: 'space-between',
      gap: 3,
      bg: 'muted',
      color: 'text',
    },
    'toast-WARNING': {
      variant: 'toast-SUCCESS',
      bg: 'warning',
      color: 'negativeText',
    },
    'toast-ERROR': {
      variant: 'toast-SUCCESS',
      bg: 'error',
      color: 'negativeText',
    },
  },
  zIndices: {
    progressbar: 103,
    modal: 60,
    menu: 50,
  },
};
