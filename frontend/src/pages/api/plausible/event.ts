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

import { NextApiRequest, NextApiResponse } from 'next';
import { userAgent } from 'next/server';
import { logger } from 'lib/logger';
import { LASIUS_TELEMETRY_PLAUSIBLE_HOST } from 'projectConfig/constants';

export type PlausibleEventPayload = {
  /** Event name */
  readonly n: string;
  /** Page URL */
  readonly u: Location['href'];
  /** Domain */
  readonly d: Location['hostname'];
  /** Referrer */
  readonly r: Document['referrer'] | null;
  /** Screen width */
  readonly w: Window['innerWidth'];
  /** Hash mode */
  readonly h: 1 | 0;
  /** Props, stringified JSON */
  readonly p?: string;
};

const handler = async (request: NextApiRequest, res: NextApiResponse) => {
  const body = request.body.json() as PlausibleEventPayload;
  logger.info('Plausible event', {
    LASIUS_TELEMETRY_PLAUSIBLE_HOST,
    body,
    headers: request.headers,
  });
  const payload: RequestInit = {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'User-Agent': request.headers['user-agent'] || '',
      'X-Forwarded-For': request.headers['x-forwarded-for']?.toString() || '',
    },
    body: JSON.stringify({ ...body, r: body.r ? body.r : request.headers['referer'] || '' }),
  };
  logger.info('Plausible event', { LASIUS_TELEMETRY_PLAUSIBLE_HOST, payload });
  const response = await fetch(
    `https://${LASIUS_TELEMETRY_PLAUSIBLE_HOST}/api/event`,
    payload
  ).catch((e) => {
    logger.error('error', e);
  });
  if (response) {
    res.status(response.status).json(response.body);
  } else {
    res.status(500).json({ error: 'error' });
  }
};

export default handler;
