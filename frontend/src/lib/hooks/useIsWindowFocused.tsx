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

import { useCallback, useEffect, useState } from 'react';
import { debounce, noop } from 'lodash';

export default function useIsWindowFocused(): boolean {
  const [windowIsActive, setWindowIsActive] = useState(true);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const handleActivity = useCallback(
    debounce(
      (e: { type: string }) => {
        if (e?.type === 'focus') {
          return setWindowIsActive(true);
        }
        if (e?.type === 'blur') {
          return setWindowIsActive(false);
        }
        if (e?.type === 'visibilitychange') {
          if (document.hidden) {
            return setWindowIsActive(false);
          }
          return setWindowIsActive(true);
        }
        return noop();
      },
      100,
      { leading: false }
    ),
    []
  );

  useEffect(() => {
    document.addEventListener('visibilitychange', handleActivity);
    document.addEventListener('blur', handleActivity);
    window.addEventListener('blur', handleActivity);
    window.addEventListener('focus', handleActivity);
    document.addEventListener('focus', handleActivity);

    return () => {
      window.removeEventListener('blur', handleActivity);
      document.removeEventListener('blur', handleActivity);
      window.removeEventListener('focus', handleActivity);
      document.removeEventListener('focus', handleActivity);
      document.removeEventListener('visibilitychange', handleActivity);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return windowIsActive;
}
