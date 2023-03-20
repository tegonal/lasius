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

import { logger } from 'lib/logger';
import { useStore } from 'storeContext/store';
import { useMemo } from 'react';

/**
 * Prepares a modal or uses an existing one by Id
 * @param modalId
 */
export const useModal = (modalId: string) => {
  const {
    state: { modalViews },
    dispatch,
  } = useStore();

  const isModalOpen = useMemo(() => {
    const modal = modalViews?.find((m) => m.id === modalId);
    return modal?.isOpen;
  }, [modalViews, modalId]);

  const addModal = (open: boolean) => {
    if (!modalViews?.find((m) => m.id === modalId)) {
      dispatch({
        type: 'modalview.add',
        payload: {
          id: modalId,
          isOpen: open,
        },
      });
    }
  };

  const openModal = () => {
    if (!isModalOpen) {
      addModal(true);
      dispatch({
        type: 'modalview.update',
        payload: {
          id: modalId,
          isOpen: true,
        },
      });
    }
  };

  const closeModal = () => {
    if (!modalViews?.find((m) => m.id === modalId)) {
      logger.error(`[useModal][closeModal][${modalId}][notExists]`);
    }
    dispatch({
      type: 'modalview.update',
      payload: {
        id: modalId,
        isOpen: false,
      },
    });
    dispatch({
      type: 'modalview.remove',
      payload: {
        id: modalId,
        isOpen: false,
      },
    });
  };

  return { modalId, openModal, closeModal, modalViews, isModalOpen, addModal };
};

export default useModal;
