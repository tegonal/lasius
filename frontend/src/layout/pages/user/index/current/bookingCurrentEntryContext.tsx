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
import { Button } from 'theme-ui';
import { Icon } from 'components/shared/icon';
import useModal from 'components/modal/hooks/useModal';
import { ContextBody } from 'components/contextMenuBar/contextBody';
import { ContextAnimatePresence } from 'components/contextMenuBar/contextAnimatePresence';
import { ContextCompactBar } from 'components/contextMenuBar/contextCompactBar';
import { ContextButtonWrapper } from 'components/contextMenuBar/contextButtonWrapper';
import { AnimatePresence } from 'framer-motion';
import { useTranslation } from 'next-i18next';
import { ModalResponsive } from 'components/modal/modalResponsive';
import { BookingEditRunning } from '../bookingEditRunning';
import { ModelsBooking } from 'lib/api/lasius';
import { ContextButtonOpen } from 'components/contextMenuBar/buttons/contextButtonOpen';
import { ContextButtonClose } from 'components/contextMenuBar/buttons/contextButtonClose';
import { ContextButtonAddFavorite } from 'components/contextMenuBar/buttons/contextButtonAddFavorite';
import { useContextMenu } from 'components/contextMenuBar/hooks/useContextMenu';

type Props = {
  item: ModelsBooking;
};

export const BookingCurrentEntryContext: React.FC<Props> = ({ item }) => {
  const { t } = useTranslation('common');
  const { modalId, openModal, closeModal } = useModal('BookingEditCurrentModal');
  const { handleCloseAll, currentOpenContextMenuId } = useContextMenu();

  const editCurrentBooking = () => {
    openModal();
    handleCloseAll();
  };

  return (
    <>
      <ContextBody>
        <ContextButtonOpen hash={item.id} />
        <AnimatePresence>
          {currentOpenContextMenuId === item.id && (
            <ContextAnimatePresence>
              <ContextCompactBar>
                <ContextButtonWrapper>
                  <Button
                    variant="contextIcon"
                    title={t('Edit booking')}
                    aria-label={t('Edit booking')}
                    onClick={editCurrentBooking}
                  >
                    <Icon name="time-clock-file-edit-interface-essential" size={24} />
                  </Button>
                </ContextButtonWrapper>
                <ContextButtonAddFavorite item={item} />
                <ContextButtonClose />
              </ContextCompactBar>
            </ContextAnimatePresence>
          )}
        </AnimatePresence>
      </ContextBody>
      <ModalResponsive modalId={modalId}>
        <BookingEditRunning item={item} onSave={closeModal} onCancel={closeModal} />
      </ModalResponsive>
    </>
  );
};
