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

import { useEffect, useState } from 'react';
import useWebSocket, { ReadyState } from 'react-use-websocket';
import { CONNECTION_STATUS, IS_SERVER, LASIUS_API_WEBSOCKET_URL } from 'projectConfig/constants';
import parseJson from 'parse-json';
import { logger } from 'lib/logger';
import { useSession } from 'next-auth/react';
import useIsWindowFocused from 'lib/hooks/useIsWindowFocused';

export const useLasiusWebsocket = () => {
  const { data } = useSession();
  const token = data?.user?.xsrfToken;
  const isWindowFocused = useIsWindowFocused();

  if (!token) logger.warn('[useLasiusWebsocket][tokenUndefined]');

  const [messageHistory, setMessageHistory] = useState<MessageEvent<any>[]>([]);

  const { sendJsonMessage, lastMessage, readyState } = useWebSocket(
    IS_SERVER ? null : `${LASIUS_API_WEBSOCKET_URL}/messagingSocket?auth=${token}`,
    {
      share: true,
      shouldReconnect: (closeEvent) => {
        logger.warn('[useLasiusWebsocket][shouldReconnect]', closeEvent);
        return true;
      },
      retryOnError: true,
      reconnectInterval: 1000,
      reconnectAttempts: 30,
    }
  );

  useEffect(() => {
    if (isWindowFocused && readyState === ReadyState.OPEN) {
      logger.info('[useLasiusWebsocket][onReturn][connected]');
    }
    if (isWindowFocused && readyState !== ReadyState.OPEN) {
      logger.info('[useLasiusWebsocket][onReturn][disconnected]');
      setMessageHistory([]);
    }
  }, [isWindowFocused, readyState]);

  useEffect(() => {
    if (lastMessage !== null) {
      setMessageHistory((prev) => [...prev, lastMessage]);
    }
  }, [lastMessage, setMessageHistory]);

  const connectionStatus = {
    [ReadyState.CONNECTING]: CONNECTION_STATUS.CONNECTING,
    [ReadyState.OPEN]: CONNECTION_STATUS.CONNECTED,
    [ReadyState.CLOSING]: CONNECTION_STATUS.DISCONNECTED,
    [ReadyState.CLOSED]: CONNECTION_STATUS.ERROR,
    [ReadyState.UNINSTANTIATED]: CONNECTION_STATUS.ERROR,
  }[readyState];

  return {
    sendJsonMessage,
    lastMessage: lastMessage?.data ? parseJson(lastMessage?.data) : null,
    connectionStatus,
    messageHistory,
  };
};
