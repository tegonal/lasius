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

import React from 'react';

import { useFormContext } from 'react-hook-form';
import { InputDatepickerStoreProvider } from 'components/forms/input/datePicker/store/store';
import {
  InputDatePickerComponent,
  InputDatePickerComponentProps,
} from 'components/forms/input/datePicker/inputDatePickerComponent';

export const InputDatePicker: React.FC<InputDatePickerComponentProps> = (props) => {
  const parentFormContext = useFormContext();
  if (!parentFormContext) return null;

  const parentFormValue = parentFormContext.getValues(props.name);

  return (
    <InputDatepickerStoreProvider
      initialDate={parentFormValue}
      name={props.name}
      parentFormContext={parentFormContext}
    >
      <InputDatePickerComponent {...props} />
    </InputDatepickerStoreProvider>
  );
};
