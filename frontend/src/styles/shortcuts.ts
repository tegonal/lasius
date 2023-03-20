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

export const outlineStyle: ThemeUIStyleObject = {
  outline: '1px dashed',
  outlineOffset: 2,
  outlineColor: 'selection',
};

export const outline = (): ThemeUIStyleObject => ({
  '&:focus': {
    ...outlineStyle,
  },
});

export const fullWidthHeight = (): ThemeUIStyleObject => ({
  width: '100%',
  height: '100%',
});

type gapResponsive = number | number[];

export const flexRowJustifyStartAlignCenter = (gap?: gapResponsive): ThemeUIStyleObject => ({
  flexDirection: 'row',
  alignItems: 'center',
  justifyContent: 'flex-start',
  gap: gap || 0,
});

export const flexRowJustifyEndAlignCenter = (gap?: gapResponsive): ThemeUIStyleObject => ({
  flexDirection: 'row',
  alignItems: 'center',
  justifyContent: 'flex-end',
  gap: gap || 0,
});

export const flexRowJustifyStartAlignStart = (gap?: gapResponsive): ThemeUIStyleObject => ({
  flexDirection: 'row',
  alignItems: 'flex-start',
  justifyContent: 'flex-start',
  gap: gap || 0,
});

export const flexRowJustifyBetweenAlignCenter = (gap?: gapResponsive): ThemeUIStyleObject => ({
  flexDirection: 'row',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: gap || 0,
});

export const flexRowJustifyCenterAlignCenter = (gap?: gapResponsive): ThemeUIStyleObject => ({
  flexDirection: 'row',
  alignItems: 'center',
  justifyContent: 'center',
  gap: gap || 0,
});

export const flexColumnJustifyStartAlignStart = (gap?: gapResponsive): ThemeUIStyleObject => ({
  flexDirection: 'column',
  alignItems: 'flex-start',
  justifyContent: 'flex-start',
  gap: gap || 0,
});

export const flexColumnJustifyCenterAlignEnd = (gap?: gapResponsive): ThemeUIStyleObject => ({
  flexDirection: 'column',
  alignItems: 'flex-end',
  justifyContent: 'center',
  gap: gap || 0,
});

export const flexColumnJustifyCenterAlignCenter = (gap?: gapResponsive): ThemeUIStyleObject => ({
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  gap: gap || 0,
});

export const flexColumnJustifySpaceAroundAlignStart = (
  gap?: gapResponsive
): ThemeUIStyleObject => ({
  flexDirection: 'column',
  alignItems: 'flex-start',
  justifyContent: 'center',
  gap: gap || 0,
});

export const borderRadiusBottom = (radius?: string): ThemeUIStyleObject => ({
  borderBottomLeftRadius: radius || themeRadii.medium,
  borderBottomRightRadius: radius || themeRadii.medium,
});

export const borderRadiusTop = (radius?: string): ThemeUIStyleObject => ({
  borderTopLeftRadius: radius || themeRadii.medium,
  borderTopRightRadius: radius || themeRadii.medium,
});

export const borderRadiusLeft = (radius?: string): ThemeUIStyleObject => ({
  borderTopLeftRadius: radius || themeRadii.medium,
  borderBottomLeftRadius: radius || themeRadii.medium,
});

export const borderRadiusRight = (radius?: string): ThemeUIStyleObject => ({
  borderTopRightRadius: radius || themeRadii.medium,
  borderBottomRightRadius: radius || themeRadii.medium,
});

export const clickableStyle = (color?: string): ThemeUIStyleObject => ({
  '&:hover': {
    color: color || 'selection',
    cursor: 'pointer',
  },
});

export const scrollbarStyle = (): ThemeUIStyleObject => ({
  '&::-webkit-scrollbar': {
    width: '6px',
  },
  '&::-webkit-scrollbar-thumb': {
    background: 'containerTextColorMuted',
    borderRadius: '5px',
  },
  '&::-webkit-scrollbar-track': {
    background: 'transparent',
  },
});

export const layoutColumnStyle: ThemeUIStyleObject = {
  background: 'containerBackground',
  color: 'containerTextColor',
  height: '100%',
  width: '100%',
  borderLeft: '1px solid',
  borderColor: 'muted',
};

export const resetAutoFillStyle = (): ThemeUIStyleObject => ({
  '&:-webkit-autofill, &:autofill': {
    color: 'inherit',
    background: 'inherit',
  },
});
