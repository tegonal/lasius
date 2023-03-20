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

import React, { useEffect } from 'react';
import { useSession } from 'next-auth/react';
import axios from 'axios';
import cookie from 'js-cookie';
import { COOKIE_NAMES } from 'projectConfig/constants';
import { logger } from 'lib/logger';

export const CookieCutter: React.FC = () => {
  const session = useSession();

  // Set the token for client side requests to use
  useEffect(() => {
    const token = session?.data?.user.xsrfToken;
    logger.log('AxiosConfig', token);
    if (token) {
      cookie.set(COOKIE_NAMES.XSRF_TOKEN, token, { sameSite: 'strict', path: '/' });
      axios.defaults.headers.common['X-XSRF-TOKEN'] = token;
    } else {
      // legacy
      cookie.remove(COOKIE_NAMES.XSRF_TOKEN, { sameSite: 'strict', path: '/' });
      cookie.remove(COOKIE_NAMES.XSRF_TOKEN);
      delete axios.defaults.headers.common['X-XSRF-TOKEN'];
    }
  }, [session]);

  return null;
};
