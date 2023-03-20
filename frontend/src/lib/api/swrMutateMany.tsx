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

import { useSWRConfig } from 'swr';

export const useSwrMutateMany = () => {
  const { cache, mutate } = useSWRConfig();
  return (matcher: { test: (arg0: any) => any }, ...args: any) => {
    if (!(cache instanceof Map)) {
      throw new Error('matchMutate requires the cache provider to be a Map instance');
    }

    const keys = [];

    // eslint-disable-next-line no-restricted-syntax
    for (const key of cache.keys()) {
      if (matcher.test(key)) {
        keys.push(key);
      }
    }

    const mutations = keys.map((key) => mutate(key, ...args));
    return Promise.all(mutations);
  };
};
