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
import { proxy } from 'lib/telemetry/matomo-proxy';

export default function stats(req: NextApiRequest, res: NextApiResponse): Promise<void> {
  return new Promise((resolve, reject) => {
    switch (true) {
      case req.url?.includes('/api/ping/script.js'):
        req.url = req.url?.replace('/api/ping/script.js', '/matomo.js');
        break;
      case req.url?.includes('/api/ping/event'):
        req.url = req.url?.replace('/api/ping/event', '/matomo.php');
        break;
      default:
        break;
    }

    proxy.once('error', (err: any) => {
      console.error(err);
      reject(err);
    });

    proxy.web(req, res);
  });
}

export const config = {
  api: {
    bodyParser: false,
  },
};
