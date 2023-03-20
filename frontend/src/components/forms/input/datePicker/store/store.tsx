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

import React, { createContext, useEffect, useMemo, useReducer } from 'react';
import {
  datePickerCreateInitialState,
  initializerType,
} from 'components/forms/input/datePicker/store/datePickerCreateInitialState';
import { inputDatePickerReducer } from 'components/forms/input/datePicker/store/inputDatePickerReducer';
import { InputDatePickerActionType } from 'components/forms/input/datePicker/store/inputDatePickerActionType';

export type Store = initializerType & {
  date: Date;
  years: string;
  months: string;
  days: string;
  hours: string;
  minutes: string;
  isoString: string;
};

export type InputDatePickerContextType = {
  state: Store;
  dispatch: React.Dispatch<InputDatePickerActionType>;
};

export const InputDatepickerContext = createContext<InputDatePickerContextType>(
  {} as InputDatePickerContextType
);

type StoreProps = {
  children: React.ReactNode;
} & initializerType;

export const InputDatepickerStoreProvider: React.FC<StoreProps> = ({
  children,
  initialDate,
  parentFormContext,
  name,
}) => {
  const [state, dispatch] = useReducer(
    inputDatePickerReducer,
    { initialDate, parentFormContext, name },
    (s) => datePickerCreateInitialState(s)
  );

  const store = useMemo(() => ({ state, dispatch }), [state, dispatch]);

  const updateParentFormContext = () => {
    const { isoString, parentFormContext, name } = store.state;
    parentFormContext.setValue(name, isoString);
  };

  const parentWatcher = parentFormContext.watch(name);

  useEffect(() => {
    if (state.isoString !== parentWatcher) {
      dispatch({ type: 'setDateFromIsoString', payload: parentWatcher });
    }
    // This is correct, we only want to update the store when the parentwatcher changes
    // eslint-disable-next-line
  }, [name, parentWatcher]);

  useEffect(() => {
    updateParentFormContext();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [store.state.isoString]);

  return (
    <InputDatepickerContext.Provider value={store}>{children}</InputDatepickerContext.Provider>
  );
};
