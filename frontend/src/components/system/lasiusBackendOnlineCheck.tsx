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
import useModal from 'components/modal/hooks/useModal';
import { ModalResponsive } from 'components/modal/modalResponsive';
import { useTranslation } from 'next-i18next';
import { CONNECTION_STATUS } from 'projectConfig/constants';
import { Box } from 'theme-ui';
import { useLasiusApiStatus } from 'components/system/hooks/useLasiusApiStatus';
import { logger } from 'lib/logger';

export const LasiusBackendOnlineCheck: React.FC = () => {
  const { t } = useTranslation('common');
  const { modalId, openModal, closeModal, isModalOpen } = useModal('BackendOfflineNoticeModal');
  const { status } = useLasiusApiStatus();

  useEffect(() => {
    if (status !== CONNECTION_STATUS.CONNECTED && !isModalOpen) {
      logger.info('LasiusBackendOnlineCheck', status);
      openModal();
    }
    if (status === CONNECTION_STATUS.CONNECTED && isModalOpen) {
      logger.info('LasiusBackendOnlineCheck', status);
      closeModal();
    }
  }, [closeModal, isModalOpen, openModal, status]);

  return (
    <ModalResponsive modalId={modalId} blockViewport>
      <Box>
        {t('Lasius is currently offline or undergoing maintenance. We will be back momentarily.')}
      </Box>
    </ModalResponsive>
  );
};
