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

import { BareFetcher, Key, SWRConfiguration, SWRHook } from 'swr';
import { logger } from 'lib/logger';

export const swrLogger = (useSWRNext: SWRHook) => {
  return (key: Key, fetcher: BareFetcher | null, config: SWRConfiguration) => {
    let nextFetcher = fetcher;

    if (fetcher) {
      nextFetcher = (...args: unknown[]) => {
        const started = Date.now();
        const label = typeof key === 'function' ? key() : Array.isArray(key) ? key.join(', ') : key;
        logger.info('SWR Request', label);
        const response = fetcher(...args);
        if (response instanceof Promise) {
          return response.then((result) => {
            logger.info('SWR Request complete', label, 'elapsed', Date.now() - started, 'ms');
            return result;
          });
        }
        logger.info('SWR Request complete', label, 'elapsed', Date.now() - started, 'ms');
        return response;
      };
    }
    return useSWRNext(key, nextFetcher, config);
  };
};
