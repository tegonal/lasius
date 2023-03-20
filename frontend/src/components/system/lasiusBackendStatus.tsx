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
import React from 'react';
import { DotGreen } from 'components/shared/dots/dotGreen';
import { DotOrange } from 'components/shared/dots/dotOrange';
import { DotRed } from 'components/shared/dots/dotRed';
import { CONNECTION_STATUS } from 'projectConfig/constants';
import { useTranslation } from 'next-i18next';
import { useLasiusApiStatus } from 'components/system/hooks/useLasiusApiStatus';
import { ToolTip } from 'components/shared/toolTip';

export const LasiusBackendStatus: React.FC = () => {
  const { t } = useTranslation('common');
  const { status } = useLasiusApiStatus();

  return (
    <Box sx={{ label: 'BackendStatus' }}>
      {status === CONNECTION_STATUS.CONNECTED && (
        <ToolTip toolTipContent={t('Connected to backend')}>
          <DotGreen />
        </ToolTip>
      )}
      {status === CONNECTION_STATUS.CONNECTING && (
        <ToolTip toolTipContent={t('Connecting to backend')}>
          <DotOrange />
        </ToolTip>
      )}
      {(status === CONNECTION_STATUS.ERROR || status === CONNECTION_STATUS.DISCONNECTED) && (
        <ToolTip toolTipContent={t('Backend seems to be unreachable')}>
          <DotRed />
        </ToolTip>
      )}
    </Box>
  );
};
