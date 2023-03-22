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

import { LASIUS_TELEMETRY_MATOMO_ID } from 'projectConfig/constants';
import { TelemetryEvent } from 'lib/telemetry/telemetryEvent';

export const matomoEventUrl = (event: TelemetryEvent) => {
  // see https://developer.matomo.org/api-reference/tracking-api
  const matomoUrlParams = new URLSearchParams({
    idsite: LASIUS_TELEMETRY_MATOMO_ID,
    rec: '1',
    action_name: event.join(' / '),
    cookie: '0',
    rand: `${window.crypto.getRandomValues(new Uint32Array(1))[0]}`,
    res: `${window.screen.width}x${window.screen.height}`,
  });
  return `/api/ping/event?${matomoUrlParams.toString()}`;
};
