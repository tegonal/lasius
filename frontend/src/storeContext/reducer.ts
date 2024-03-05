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

import { logger } from 'lib/logger';
import { initialState, setLocalStorage, Store } from 'storeContext/store';
import { ModalViewType, TabViewType, ToastViewType } from 'types/dynamicViews';
import { compact } from 'lodash';

export type ActionType =
  | { type: 'reset' }
  | { type: 'calendar.setSelectedDate'; payload: string }
  | { type: 'context.open'; payload: string }
  | { type: 'context.close' }
  | { type: 'tabview.add'; payload: TabViewType }
  | { type: 'tabview.remove'; payload: TabViewType }
  | { type: 'tabview.update'; payload: TabViewType }
  | { type: 'modalview.add'; payload: ModalViewType }
  | { type: 'modalview.remove'; payload: ModalViewType }
  | { type: 'modalview.update'; payload: ModalViewType }
  | { type: 'toastview.add'; payload: ToastViewType }
  | { type: 'toastview.remove'; payload: ToastViewType };

export const storeReducer = (state: Store, action: ActionType) => {
  logger.info({ action });

  let newState: Partial<Store>;

  switch (action.type) {
    case 'reset':
      newState = initialState;
      break;
    case 'calendar.setSelectedDate':
      newState = { calendar: { ...state.calendar, selectedDate: action.payload } };
      break;
    case 'context.open':
      newState = { contextMenuOpen: action.payload };
      break;
    case 'context.close':
      newState = { contextMenuOpen: '' };
      break;
    case 'tabview.add':
      newState = { tabViews: [...state.tabViews, action.payload] };
      break;
    case 'tabview.remove':
      newState = {
        tabViews: compact(
          state.tabViews.map((tab) => {
            if (tab.id === action.payload.id) {
              return undefined;
            }
            return tab;
          })
        ),
      };
      break;
    case 'tabview.update':
      newState = {
        tabViews: compact(
          state.tabViews.map((tab) => {
            if (tab.id === action.payload.id) {
              return action.payload;
            }
            return tab;
          })
        ),
      };
      break;
    case 'modalview.add':
      newState = { modalViews: [...state.modalViews, action.payload] };
      break;
    case 'modalview.remove':
      newState = {
        modalViews: compact(
          state.modalViews.map((tab) => {
            if (tab.id === action.payload.id) {
              return undefined;
            }
            return tab;
          })
        ),
      };
      break;
    case 'modalview.update':
      newState = {
        modalViews: compact(
          state.modalViews.map((tab) => {
            if (tab.id === action.payload.id) {
              return action.payload;
            }
            return tab;
          })
        ),
      };
      break;
    case 'toastview.add':
      if (!state.toastViews.find((t) => t.id === action.payload.id)) {
        newState = { toastViews: [...state.toastViews, action.payload] };
        break;
      }
      newState = state;
      break;
    case 'toastview.remove':
      newState = {
        toastViews: compact(
          state.toastViews.map((toast) => {
            if (toast.id === action.payload.id) {
              return undefined;
            }
            return toast;
          })
        ),
      };
      break;

    default:
      throw new Error('Invalid action type');
  }

  if (newState) {
    const merged = { ...state, ...newState };
    setLocalStorage(merged);
    return merged;
  }

  return state;
};
