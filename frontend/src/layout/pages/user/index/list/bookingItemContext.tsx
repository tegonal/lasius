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
import { AnimatePresence } from 'framer-motion';
import { ModalResponsive } from 'components/modal/modalResponsive';
import { useTranslation } from 'next-i18next';
import { ContextAnimatePresence } from 'components/contextMenuBar/contextAnimatePresence';
import { ContextCompactBar } from 'components/contextMenuBar/contextCompactBar';
import { ContextBody } from 'components/contextMenuBar/contextBody';
import { ContextButtonWrapper } from 'components/contextMenuBar/contextButtonWrapper';
import useModal from 'components/modal/hooks/useModal';
import { BookingAddUpdateForm } from 'layout/pages/user/index/bookingAddUpdateForm';
import { useContextMenu } from 'components/contextMenuBar/hooks/useContextMenu';
import { ContextButtonOpen } from 'components/contextMenuBar/buttons/contextButtonOpen';
import { ContextButtonClose } from 'components/contextMenuBar/buttons/contextButtonClose';
import { ContextButtonStartBooking } from 'components/contextMenuBar/buttons/contextButtonStartBooking';
import { ContextButtonAddFavorite } from 'components/contextMenuBar/buttons/contextButtonAddFavorite';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { ModelsBooking } from 'lib/api/lasius';

type Props = {
  item: ModelsBooking;
};

export const BookingItemContext: React.FC<Props> = ({ item }) => {
  const { modalId, openModal, closeModal } = useModal(`EditModal-${item.id}`);
  const { t } = useTranslation('common');
  const { actionDeleteBooking, currentOpenContextMenuId, handleCloseAll } = useContextMenu();
  const { selectedOrganisationId } = useOrganisation();

  const updateItem = () => {
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
                <ContextButtonStartBooking item={item} />
                <ContextButtonWrapper>
                  <Button
                    variant="contextIcon"
                    title={t('Edit booking')}
                    aria-label={t('Edit booking')}
                    onClick={() => updateItem()}
                  >
                    <Icon name="time-clock-file-edit-interface-essential" size={24} />
                  </Button>
                </ContextButtonWrapper>
                <ContextButtonAddFavorite item={item} />
                <ContextButtonWrapper>
                  <Button
                    variant="contextIcon"
                    title={t('Delete booking')}
                    aria-label={t('Delete booking')}
                    onClick={() => actionDeleteBooking(selectedOrganisationId, item)}
                  >
                    <Icon name="bin-2-alternate-interface-essential" size={24} />
                  </Button>
                </ContextButtonWrapper>
                <ContextButtonClose />
              </ContextCompactBar>
            </ContextAnimatePresence>
          )}
        </AnimatePresence>
      </ContextBody>
      <ModalResponsive modalId={modalId}>
        <BookingAddUpdateForm
          mode="update"
          itemUpdate={item}
          onSave={closeModal}
          onCancel={closeModal}
        />
      </ModalResponsive>
    </>
  );
};
