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

import { lighten, transparentize } from '@theme-ui/color';
import { ThemeUIStyleObject } from 'theme-ui';
import { effects } from 'styles/theme/effects';
import { themeRadii } from 'styles/theme/radii';
import {
  flexColumnJustifyCenterAlignCenter,
  flexRowJustifyCenterAlignCenter,
  flexRowJustifyStartAlignCenter,
  outline,
} from 'styles/shortcuts';

const disabled: ThemeUIStyleObject = {
  '&:disabled': {
    ...effects.color.desaturate,
    opacity: 0.9,
    '&:hover': {
      cursor: 'not-allowed',
    },
  },
};

export const themeButtons: Record<string, ThemeUIStyleObject> = {
  primary: {
    fontSize: 2,
    fontWeight: 500,
    color: 'negativeText',
    py: '6px',
    background: 'greenGradient',
    borderRadius: themeRadii.small,
    ...effects.shadows.softShadowOnDark,
    display: 'flex',
    ...flexRowJustifyCenterAlignCenter(2),
    width: '100%',
    '&:hover': {
      background: 'greenGradientHover',
      cursor: 'pointer',
    },
    ...outline(),
    ...disabled,
  },
  primarySmall: {
    variant: 'buttons.primary',
    fontSize: 2,
    py: '4px',
  },
  secondary: {
    variant: 'buttons.primary',
    background: 'grayGradient',
    '&:hover': {
      background: 'grayGradientHover',
      cursor: 'pointer',
    },
  },
  secondarySmall: {
    variant: 'buttons.secondary',
    fontSize: 1,
    py: '4px',
  },
  smallTransparent: {
    color: 'currentcolor',
    bg: 'transparent',
    fontSize: 1,
    lineHeight: 'normal',
    px: 1,
    py: 0,
    '&:hover': {
      color: 'selection',
      cursor: 'pointer',
    },
    ...disabled,
  },
  stopRecording: {
    color: 'negativeText',
    background: 'redGradient',
    width: 'unset',
    height: 'unset',
    mt: 1,
    p: 2,
    display: 'flex',
    borderRadius: 24,
    ...flexColumnJustifyCenterAlignCenter(),
    ...effects.shadows.softShadowOnWhiteSmall,
    '&:hover': {
      background: 'redGradientHover',
      cursor: 'pointer',
      color: 'selection',
    },
    ...disabled,
    minWidth: 'unset',
  },
  warning: {
    color: 'background',
    bg: 'warning',
    '&:hover': {
      bg: lighten('warning', 0.1),
      cursor: 'pointer',
    },
    '&:disabled': {
      bg: lighten('warning', 0.7),
      '&:hover': {
        cursor: 'not-allowed',
      },
    },
    ...disabled,
    ...outline(),
  },
  danger: {
    fontSize: 2,
    fontWeight: 500,
    color: 'negativeText',
    py: '6px',
    background: 'redGradient',
    borderRadius: themeRadii.small,
    ...effects.shadows.softShadowOnDark,
    display: 'flex',
    ...flexRowJustifyCenterAlignCenter(2),
    width: '100%',
    '&:hover': {
      background: 'redGradientHover',
      cursor: 'pointer',
    },
    ...disabled,
    ...outline(),
  },
  icon: {
    p: 1,
    bg: 'transparent',
    display: 'block',
    color: 'currentColor',
    '&:hover': {
      cursor: 'pointer',
      color: 'selection',
    },
    ...disabled,
  },
  headerIcon: {
    p: 1,
    bg: 'transparent',
    color: 'currentColor',
    '&:hover': {
      cursor: 'pointer',
      color: 'selection',
    },
    ...disabled,
    ...outline(),
  },
  contextIcon: {
    p: 1,
    bg: 'transparent',
    display: 'block',
    color: 'currentColor',
    '&:hover': {
      bg: 'negativeText',
      cursor: 'pointer',
      color: 'confirmation',
    },
    ...disabled,
    ...outline(),
  },
  iconText: {
    p: 2,
    bg: 'transparent',
    display: 'flex',
    width: '100%',
    ...flexRowJustifyStartAlignCenter(2),
    color: 'currentColor',
    '&:hover': {
      bg: transparentize('containerBackgroundDarker', 0.5),
      cursor: 'pointer',
      color: 'selection',
    },
    ...disabled,
  },
  iconTextActive: {
    p: 2,
    bg: 'containerBackgroundLighter',
    display: 'flex',
    width: '100%',
    ...flexRowJustifyStartAlignCenter(2),
    color: 'currentColor',
    '&:hover': {
      cursor: 'pointer',
      color: 'currentColor',
    },
    ...disabled,
  },
  iconMuted: {
    width: '100%',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    px: 1,
    py: 1,
    bg: 'transparent',
    color: 'containerTextColorMuted',
    '&:hover': {
      cursor: 'pointer',
      color: 'selection',
    },
    ...disabled,
    ...outline(),
  },
  primaryCircle: {
    width: '64px',
    height: '64px',
    p: 0,
    fontSize: 2,
    fontWeight: 500,
    color: 'negativeText',
    background: 'greenGradient',
    borderRadius: '32px',
    ...effects.shadows.softShadowOnDark,
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    '&:hover': {
      background: 'greenGradientHover',
      cursor: 'pointer',
    },
    ...disabled,
    ...outline(),
  },
  secondaryCircle: {
    width: '48px',
    height: '48px',
    p: 0,
    fontSize: 2,
    fontWeight: 500,
    color: 'negativeText',
    background: 'grayGradient',
    borderRadius: '24px',
    ...effects.shadows.softShadowOnDark,
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    '&:hover': {
      background: 'grayGradientHover',
      cursor: 'pointer',
    },
    ...disabled,
    ...outline(),
  },
  tabIcon: {
    p: 1,
    bg: 'transparent',
    color: 'currentcolor',
    width: 64,
    height: 64,
    display: 'flex',
    ...flexRowJustifyCenterAlignCenter(0),
    '&:hover': {
      cursor: 'pointer',
      color: 'selection',
    },
    '&:selected': {
      color: 'currentcolor',
    },
    ...disabled,
  },
  tab: {
    p: 1,
    bg: 'transparent',
    color: 'currentcolor',
    display: 'flex',
    ...flexRowJustifyCenterAlignCenter(0),
    '&:hover': {
      cursor: 'pointer',
      color: 'selection',
    },
    ...disabled,
  },
  tabSelected: {
    p: 1,
    bg: 'transparent',
    color: 'selection',
    display: 'flex',
    ...flexRowJustifyCenterAlignCenter(0),
    '&:hover': {
      cursor: 'pointer',
      color: 'selection',
    },
    ...disabled,
  },
};
