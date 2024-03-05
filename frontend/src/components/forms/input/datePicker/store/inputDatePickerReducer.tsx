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
import { padStart, toNumber } from 'lodash';
import { dateToObj } from 'components/forms/input/datePicker/store/dateToObj';
import { formatISOLocale } from 'lib/dates';
import { Store } from 'components/forms/input/datePicker/store/store';
import { InputDatePickerActionType } from 'components/forms/input/datePicker/store/inputDatePickerActionType';

export const inputDatePickerReducer = (state: Store, action: InputDatePickerActionType) => {
  if (!action.payload || !action.type) {
    logger.info('date state remains unchanged');
    return state;
  }
  logger.info('inputDatePickerReducer', { action });
  let newState: Partial<Store> = state;
  switch (action.type) {
    case 'setDateFromIsoString':
      newState = { ...dateToObj(new Date(action.payload)) };
      break;
    case 'setYear':
      newState = { years: action.payload };
      break;
    case 'setMonth':
      newState = { months: padStart((toNumber(action.payload) - 1).toString(), 2, '0') };
      break;
    case 'setDay':
      newState = { days: action.payload };
      break;
    case 'setHours':
      newState = { hours: action.payload };
      break;
    case 'setMinutes':
      newState = { minutes: action.payload };
      break;
    case 'setSeconds':
      newState = { seconds: action.payload };
      break;
    default:
      throw new Error('Invalid action type');
  }

  const merged = { ...state, ...newState };

  newState.date = new Date(
    toNumber(merged.years),
    toNumber(merged.months),
    toNumber(merged.days),
    toNumber(merged.hours),
    toNumber(merged.minutes),
    toNumber(merged.seconds)
  );

  newState = { ...newState, ...dateToObj(newState.date) };

  if (newState.date) {
    const isoString = formatISOLocale(newState.date);
    logger.info('inputDatePickerReducer', { newDate: isoString });
    return { ...state, ...newState, isoString };
  }

  return state;
};
