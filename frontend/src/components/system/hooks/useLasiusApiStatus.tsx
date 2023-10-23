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
import { CONNECTION_STATUS, IS_BROWSER } from 'projectConfig/constants';
import { useInterval } from 'usehooks-ts';
import { API_STATUS_INTERVAL } from 'projectConfig/intervals';
import { lasiusAxiosInstance } from 'lib/api/lasiusAxiosInstance';
import { useBrowserConnectionStatus } from 'components/system/hooks/useBrowserConnectionStatus';
import { getGetConfigurationKey } from 'lib/api/lasius/general/general';
import { logger } from 'lib/logger';
import { LasiusPlausibleEvents } from 'lib/telemetry/plausibleEvents';
import { usePlausible } from 'next-plausible';

const testApiConnection = async () => {
  try {
    await lasiusAxiosInstance({
      url: getGetConfigurationKey().toString(),
      method: 'get',
    });
    return CONNECTION_STATUS.CONNECTED;
  } catch (error) {
    logger.logEverywhere(error);
    if ((error as any)?.response?.status === 401) {
      return CONNECTION_STATUS.NOT_AUTHENTICATED;
    }
    return CONNECTION_STATUS.DISCONNECTED;
  }
};

export const useLasiusApiStatus = () => {
  const [status, setStatus] = React.useState(CONNECTION_STATUS.CONNECTED);
  const { status: browserStatus } = useBrowserConnectionStatus();
  const plausible = usePlausible<LasiusPlausibleEvents>();

  const handleOnline = () => {
    setStatus(CONNECTION_STATUS.CONNECTED);
  };

  const handleOffline = () => {
    setStatus(CONNECTION_STATUS.DISCONNECTED);
    if (browserStatus === CONNECTION_STATUS.CONNECTED) {
      if (IS_BROWSER) {
        plausible('error', { props: { status: 'apiConnection', message: 'connectionLost' } });
      }
    }
  };

  useInterval(async () => {
    if (browserStatus === CONNECTION_STATUS.CONNECTED) {
      const status = await testApiConnection();
      if (status === CONNECTION_STATUS.CONNECTED) handleOnline();
      if (status === CONNECTION_STATUS.DISCONNECTED) handleOffline();
    }
  }, API_STATUS_INTERVAL);

  return { status };
};
