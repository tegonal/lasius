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

import React, { ChangeEventHandler, useEffect, useRef } from 'react';
import { padStart, toNumber } from 'lodash';
import { InputDatePickerActionType } from 'components/forms/input/datePicker/store/inputDatePickerActionType';

type Actions = 'setYear' | 'setMonth' | 'setDay' | 'setHours' | 'setMinutes';

interface Props {
  incrementerValue: number;
  setter: Actions;
  defaultValue: number;
  digits: number;
  dispatch: React.Dispatch<InputDatePickerActionType>;
}

export const useDateFieldInput = ({
  incrementerValue,
  setter,
  dispatch,
  defaultValue,
  digits = 2,
}: Props) => {
  const ref = useRef<HTMLInputElement>(null);
  const [value, setValue] = React.useState(padStart(defaultValue.toString(), digits, '0'));

  const handleClickUp = () => {
    let value = toNumber(ref.current?.value);
    value += incrementerValue;
    const string = padStart(value.toString(), digits, '0');
    setValue(string);
    dispatch({ type: setter, payload: string });
    ref?.current?.dispatchEvent(new Event('mouseup', { bubbles: true }));
  };

  const handleClickDown = () => {
    let value = toNumber(ref.current?.value);
    value -= incrementerValue;
    const string = padStart(value.toString(), digits, '0');
    setValue(string);
    dispatch({ type: setter, payload: string });
    ref?.current?.dispatchEvent(new Event('mouseup', { bubbles: true }));
  };

  const onChange: ChangeEventHandler<HTMLInputElement> = (e) => {
    const value = e.target.value.replace(/\D/g, '');
    setValue(value);
  };

  const onKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace') {
      e.currentTarget.value = '';
      e.preventDefault();
    }
  };

  const onBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    const value = e.target.value.replace(/\D/g, '');
    setValue(padStart(value, digits, '0'));
    dispatch({ type: setter, payload: value });
  };

  const onMouseUp = (e: React.MouseEvent<HTMLInputElement>) => {
    e.currentTarget.select();
  };

  const onFocus = (e: React.FocusEvent<HTMLInputElement>) => {
    e.currentTarget.select();
  };

  useEffect(() => {
    setValue(padStart(defaultValue.toString(), digits, '0'));
  }, [defaultValue, digits]);

  return {
    inputProps: {
      ref,
      onChange,
      onBlur,
      onKeyDown,
      onFocus,
      onMouseUp,
      maxLength: digits,
      value,
      pattern: `[0-9]{${digits}}`,
    },
    handleClickUp,
    handleClickDown,
  };
};
