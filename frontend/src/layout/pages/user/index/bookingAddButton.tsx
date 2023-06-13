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

import React from 'react';
import { ModalResponsive } from 'components/modal/modalResponsive';
import { BookingAddUpdateForm } from 'layout/pages/user/index/bookingAddUpdateForm';
import { Button } from '@theme-ui/components';
import { useTranslation } from 'next-i18next';
import useModal from 'components/modal/hooks/useModal';

export const BookingAddButton: React.FC = () => {
  const { modalId, openModal, closeModal } = useModal('BookingCreateExtendedModal');
  const { t } = useTranslation('common');
  return (
    <>
      <Button onClick={openModal} variant="secondary">
        {t('Create a booking')}
      </Button>
      <ModalResponsive modalId={modalId}>
        <BookingAddUpdateForm mode="add" onSave={closeModal} onCancel={closeModal} />
      </ModalResponsive>
    </>
  );
};
