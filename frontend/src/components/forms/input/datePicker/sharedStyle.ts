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

export const datePickerTimeInputStyle: ThemeUIStyleObject = {
  gridTemplateRows: '1fr 1fr 1fr',
  width: '1.5rem',
  gap: 0,
};

export const datePickerDateInputStyle: ThemeUIStyleObject = {
  gridTemplateRows: '1fr 1fr 1fr',
  width: '1.5rem',
  gap: 0,
};

export const datePickerYearInputStyle: ThemeUIStyleObject = {
  gridTemplateRows: '1fr 1fr 1fr',
  width: '2.8rem',
  gap: 0,
};

export const datePickerTimeSeparatorStyle: ThemeUIStyleObject = {
  gridTemplateRows: '1fr',
  gap: 0,
  userSelect: 'none',
  textAlign: 'center',
  px: '1px',
};

export const datePickerDigitInputStyle: ThemeUIStyleObject = {
  width: '100%',
  textAlign: 'center',
  p: '1px',
  '&:focus': {
    background: 'selection',
    outline: 'none',
    borderRadius: '2px',
  },
  '&::-webkit-outer-spin-button, ::-webkit-inner-spin-button': {
    WebkitAppearance: 'none',
    margin: 0,
  },
  '&[type=number]': {
    MozAppearance: 'textfield',
  },
};
