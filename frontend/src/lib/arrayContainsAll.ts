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

/**
 * Returns true if the array contains all elements of the other array.
 * @param needle
 * @param haystack
 */
export const arrayContainsAll = (needle: string | any[], haystack: string | any[]) => {
  for (let i = 0; i < needle.length; i += 1) {
    if (haystack.indexOf(needle[i]) === -1) {
      return false;
    }
  }
  return true;
};
