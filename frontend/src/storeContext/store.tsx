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

import React, { createContext, useMemo, useReducer } from 'react';
import { ActionType, storeReducer } from 'storeContext/reducer';
import { formatISOLocale } from 'lib/dates';
import { ModalViewType, TabViewType, ToastViewType } from 'types/dynamicViews';
import { IS_BROWSER } from 'projectConfig/constants';

export type Store = {
  calendar: {
    selectedDate: string;
  };
  contextMenuOpen: string;
  tabViews: TabViewType[];
  modalViews: ModalViewType[];
  toastViews: ToastViewType[];
};

export type StoreType = {
  state: Store;
  dispatch: React.Dispatch<ActionType>;
};

export const initialState: Store = {
  calendar: {
    selectedDate: formatISOLocale(new Date()),
  },
  contextMenuOpen: '',
  tabViews: [],
  modalViews: [],
  toastViews: [],
};

export const StoreContext = createContext<StoreType>({} as StoreType);

type StoreProps = {
  children: React.ReactNode;
};

const getLocalStorage = (): Store => {
  const value = IS_BROWSER && window.localStorage.getItem('lasius.uiState');
  return value ? JSON.parse(value) : null;
};

export const setLocalStorage = (state: Store) =>
  IS_BROWSER && window.localStorage.setItem('lasius.uiState', JSON.stringify(state));

export const initializer = (initialValue = initialState) => {
  const persisted = getLocalStorage();
  return persisted
    ? {
        ...persisted,
        modalViews: [],
        toastViews: [],
      }
    : initialValue;
};

export const StoreContextProvider: React.FC<StoreProps> = ({ children }) => {
  const [state, dispatch] = useReducer(storeReducer, initialState, initializer);

  const store = useMemo(() => ({ state, dispatch }), [state, dispatch]);

  return <StoreContext.Provider value={store}>{children}</StoreContext.Provider>;
};

export const useStore = () => React.useContext(StoreContext);
