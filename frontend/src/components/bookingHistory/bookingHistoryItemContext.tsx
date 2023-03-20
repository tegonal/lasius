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
import { ContextCompactBody } from 'components/contextMenuBar/contextCompactBody';
import { ContextCompactAnimatePresence } from 'components/contextMenuBar/contextCompactAnimatePresence';
import { ContextCompactBar } from 'components/contextMenuBar/contextCompactBar';
import { ContextCompactButtonWrapper } from 'components/contextMenuBar/contextCompactButtonWrapper';
import { useTranslation } from 'next-i18next';
import useModal from 'components/modal/hooks/useModal';
import { BookingAddUpdateForm } from 'layout/pages/user/index/bookingAddUpdateForm';
import { useContextMenu } from 'components/contextMenuBar/hooks/useContextMenu';
import { ContextButtonOpen } from 'components/contextMenuBar/buttons/contextButtonOpen';
import { ContextButtonClose } from 'components/contextMenuBar/buttons/contextButtonClose';
import { ContextButtonStartBooking } from 'components/contextMenuBar/buttons/contextButtonStartBooking';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { deleteUserBooking } from 'lib/api/lasius/user-bookings/user-bookings';
import { ModelsBooking } from 'lib/api/lasius';

type Props = {
  item: ModelsBooking;
  allowDelete?: boolean;
  allowEdit?: boolean;
};

export const BookingHistoryItemContext: React.FC<Props> = ({
  item,
  allowDelete = false,
  allowEdit = false,
}) => {
  const { modalId, openModal, closeModal } = useModal(`EditModal-${item.id}`);
  const { t } = useTranslation('common');
  const { actionAddBookingToFavorites, handleCloseAll, currentOpenContextMenuId } =
    useContextMenu();
  const { selectedOrganisationId } = useOrganisation();

  const deleteItem = async () => {
    await deleteUserBooking(selectedOrganisationId, item.id);
    handleCloseAll();
  };

  const updateItem = () => {
    openModal();
    handleCloseAll();
  };

  return (
    <>
      <ContextCompactBody>
        <ContextButtonOpen hash={item.id} />
        <AnimatePresence>
          {currentOpenContextMenuId === item.id && (
            <ContextCompactAnimatePresence>
              <ContextCompactBar>
                <ContextButtonStartBooking variant="compact" item={item} />
                {allowEdit && (
                  <ContextCompactButtonWrapper>
                    <Button
                      title={t('Edit booking')}
                      aria-label={t('Edit booking')}
                      variant="contextIcon"
                      onClick={() => updateItem()}
                    >
                      <Icon name="time-clock-file-edit-interface-essential" size={24} />
                    </Button>
                  </ContextCompactButtonWrapper>
                )}
                <ContextCompactButtonWrapper>
                  <Button
                    title={t('Add as favorite')}
                    aria-label={t('Add as favorite')}
                    variant="contextIcon"
                    onClick={() => actionAddBookingToFavorites(selectedOrganisationId, item)}
                  >
                    <Icon name="rating-star-add-social-medias-rewards-rating" size={24} />
                  </Button>
                </ContextCompactButtonWrapper>
                {allowDelete && (
                  <ContextCompactButtonWrapper>
                    <Button
                      title={t('Delete booking')}
                      aria-label={t('Delete booking')}
                      variant="contextIcon"
                      onClick={() => deleteItem()}
                    >
                      <Icon name="remove-circle-interface-essential" size={24} />
                    </Button>
                  </ContextCompactButtonWrapper>
                )}
                <ContextButtonClose variant="compact" />
              </ContextCompactBar>
            </ContextCompactAnimatePresence>
          )}
        </AnimatePresence>
      </ContextCompactBody>
      <ModalResponsive modalId={modalId}>
        <BookingAddUpdateForm mode="update" item={item} onSave={closeModal} onCancel={closeModal} />
      </ModalResponsive>
    </>
  );
};
