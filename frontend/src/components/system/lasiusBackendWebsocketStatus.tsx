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

import { Box } from 'theme-ui';
import React, { useEffect } from 'react';
import { DotGreen } from 'components/shared/dots/dotGreen';
import { DotOrange } from 'components/shared/dots/dotOrange';
import { DotRed } from 'components/shared/dots/dotRed';
import { CONNECTION_STATUS } from 'projectConfig/constants';
import { useTranslation } from 'next-i18next';
import { useLasiusWebsocket } from 'lib/api/hooks/useLasiusWebsocket';
import { logger } from 'lib/logger';
import { ToolTip } from 'components/shared/toolTip';
import { useInterval, useIsClient } from 'usehooks-ts';

export const LasiusBackendWebsocketStatus: React.FC = () => {
  const { t } = useTranslation('common');
  const { connectionStatus, sendJsonMessage } = useLasiusWebsocket();
  const [status, setStatus] = React.useState<CONNECTION_STATUS>(CONNECTION_STATUS.DISCONNECTED);
  const isClient = useIsClient();

  useEffect(() => {
    if (
      connectionStatus === CONNECTION_STATUS.DISCONNECTED ||
      connectionStatus === CONNECTION_STATUS.ERROR
    ) {
      logger.log('[AppWebsocketStatus][Disconnected]');
    } else {
      logger.log('[AppWebsocketStatus]', connectionStatus);
    }
    setStatus(connectionStatus);
  }, [connectionStatus]);

  //  In an effort to keep the websocket connection alive, we send a ping message every 5 seconds
  useInterval(() => {
    if (connectionStatus === CONNECTION_STATUS.CONNECTED) {
      logger.log('[AppWebsocketStatus][SendingPing]');
      sendJsonMessage({ type: 'HelloServer', client: 'lasius-nextjs-frontend' }, false);
    }
  }, 5000);

  if (!isClient) return null;

  return (
    <Box sx={{ label: 'WebsocketStatus', position: 'fixed', inset: '8px 8px auto auto' }}>
      {status === CONNECTION_STATUS.CONNECTED && (
        <ToolTip toolTipContent={t('Websocket connected')}>
          <DotGreen />
        </ToolTip>
      )}
      {status === CONNECTION_STATUS.CONNECTING && (
        <ToolTip toolTipContent={t('Websocket connecting')}>
          <DotOrange />
        </ToolTip>
      )}
      {status === CONNECTION_STATUS.ERROR && (
        <ToolTip toolTipContent={t('Unable to connect to websocket')}>
          <DotRed />
        </ToolTip>
      )}
      {status === CONNECTION_STATUS.DISCONNECTED && (
        <ToolTip toolTipContent={t('Unable to connect to websocket')}>
          <DotRed />
        </ToolTip>
      )}
    </Box>
  );
};
