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

import {
  DEV,
  LASIUS_TELEMETRY_MATOMO_HOST,
  LASIUS_TELEMETRY_MATOMO_ID,
} from 'projectConfig/constants';
import { logger } from 'lib/logger';
import { matomoEventUrl } from 'lib/telemetry/matomoEventUrl';

type TelemetryComponent = string;
type TelemetryAction = string;
type TelemetryName = string;
export type TelemetryEvent = [TelemetryComponent, TelemetryAction, TelemetryName];

export const telemetryEvent = async (event: TelemetryEvent) => {
  const url = matomoEventUrl(event);
  if (DEV) {
    logger.info('[Telemetry]', url);
    return Promise.resolve();
  }
  if (!LASIUS_TELEMETRY_MATOMO_ID || !LASIUS_TELEMETRY_MATOMO_HOST) {
    logger.info('[Telemetry] Not enabled');
    return Promise.resolve();
  }
  return fetch(matomoEventUrl(event), { method: 'GET' });
};
