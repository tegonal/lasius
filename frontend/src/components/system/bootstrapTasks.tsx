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
import { useRouter } from 'next/router';
import { logger } from 'lib/logger';
import { useContextMenu } from 'components/contextMenuBar/hooks/useContextMenu';

export const BootstrapTasks: React.FC = () => {
  const { events } = useRouter();
  const { handleCloseAll } = useContextMenu();

  const handleRouteChangeComplete = (newRoute: any) => {
    logger.info('[BootstrapTasks][onRouteChangeComplete]', { newRoute });
    handleCloseAll();
  };

  const handleRouteChangeError = (errorRoute: string) => {
    logger.error('[BootstrapTasks][onRouteChangeError]', errorRoute);
  };

  useEffect(() => {
    logger.info('[initializer][loadOnRefresh]');
    events.on('routeChangeComplete', handleRouteChangeComplete);
    events.on('routeChangeError', handleRouteChangeError);

    return () => {
      events.off('routeChangeStart', handleRouteChangeComplete);
      events.off('routeChangeError', handleRouteChangeError);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return null;
};
