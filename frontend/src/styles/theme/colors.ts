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

import { ColorMode } from '@theme-ui/css';
import { darken, lighten, transparentize } from 'polished';

export const colorPalette1 = ['#256917', '#9fab76', '#716e20', '#9c3f31', '#723431'];
export const colorPalette2 = ['#3C3852', '#368b84', '#8aa251', '#bc6f23', '#DA651D'];
export const colorPalette3 = ['#363d6b', '#3371a4', '#44bf79', '#b2a149', '#D9832D'];

export const organisationAvatarPalette = ['#4a4343', '#52fbba', '#fffa45', '#ff1d00', '#723431'];
export const projectAvatarPalette = ['#32911b', '#a9bb5a', '#8e8a1e', '#b03727', '#723431'];
export const userAvatarPalette = ['#212020', '#0f455b', '#224431', '#836c02', '#D9832D'];

const paper = 'rgba(250, 250, 250, 1)';
const ink = 'rgba(32, 32, 32, 1)';
const marker = 'rgba(0, 210, 255, 1)';

export const themeColors: ColorMode = {
  text: ink,
  negativeText: paper,
  negativeTextMuted: transparentize(0.5, paper),
  background: paper,
  primary: '#0F1916',
  secondary: '#224938',
  accent: '#C3CA9A',
  muted: lighten(0.35, ink),
  confirmation: '#00D208',
  selection: marker,
  soft: darken(0.05, paper),
  warning: '#FF9213',
  error: '#E80000',
  greenGradient: 'linear-gradient(180deg, #00D208 0%, #00A207 100%)',
  greenGradientHover: 'linear-gradient(180deg, #00BA07 0%, #008105 100%)',
  redGradient: 'linear-gradient(180deg, #FF1A1A 0%, #B90000 100%)',
  redGradientHover: 'linear-gradient(180deg, #E61717 0%, #870000 100%)',
  grayGradient: 'linear-gradient(180deg, #666 0%, #555 100%)',
  grayGradientHover: 'linear-gradient(180deg, #444 0%, #333 100%)',
  transparentShadow: 'rgba(0, 0, 0, 0.1)',
  containerBackground: lighten(0.2, ink),
  containerBackgroundDarker: lighten(0.13, ink),
  containerBackgroundLighter: lighten(0.25, ink),
  containerTextColor: lighten(0.75, ink),
  containerTextColorMuted: lighten(0.5, ink),
  tagBackground: lighten(0.01, ink),
  tagText: lighten(0.7, ink),
  accentColors: colorPalette3,
  modes: {
    dark: {
      selection: 'rgba(0, 210, 255, 1)',
      background: '#111',
      text: 'rgba(250, 250, 250, 1)',
      muted: '#444',
      containerBackground: 'rgba(32, 32, 32, 1)',
      containerBackgroundDarker: 'rgba(12, 12, 12, 1)',
      containerBackgroundLighter: 'rgba(64, 64, 64, 1)',
      containerTextColor: 'rgba(250, 250, 250, 1)',
      containerTextColorMuted: 'rgba(250, 250, 250, 0.4)',
      tagBackground: 'rgba(90, 90, 90, 1)',
      tagText: 'rgba(190, 190, 190, 1)',
    },
    light: {
      selection: 'rgba(0, 150, 215, 1)',
      background: 'rgba(240, 240, 240, 1)',
      text: '#080808',
      muted: '#ddd',
      containerBackground: 'rgba(240, 240, 240, 1)',
      containerBackgroundDarker: 'rgba(228, 228, 228, 1)',
      containerBackgroundLighter: 'rgba(250, 250, 250, 1)',
      containerTextColor: 'rgba(12, 12, 12, 1)',
      containerTextColorMuted: 'rgba(12, 12, 12, 0.3)',
      tagBackground: 'rgba(220, 220, 220, 1)',
      tagText: 'rgba(110, 110, 110, 1)',
    },
  },
};

export const themeColorModes = ['default', 'light', 'dark'];
