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

import React, { useEffect, useState } from 'react';
import { ModalConfirm } from 'components/modal/modalConfirm';
import { useTranslation } from 'next-i18next';

const LasiusPwaUpdater: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const { t } = useTranslation('common');

  const handleConfirm = () => {
    window.workbox.messageSkipWaiting();
    setIsOpen(false);
  };

  useEffect(() => {
    window.workbox.addEventListener('controlling', () => {
      window.location.reload();
    });
    window.workbox.addEventListener('waiting', () => setIsOpen(true));
    window.workbox.register();
  }, []);

  if (!isOpen) return null;

  return (
    <ModalConfirm
      onConfirm={handleConfirm}
      text={{
        action: t('Lasius has been updated. The page will reload after your confirmation.'),
        confirm: t('Reload application'),
      }}
    />
  );
};

export default LasiusPwaUpdater;
