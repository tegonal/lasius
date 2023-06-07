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
import { AnimatePresence, m } from 'framer-motion';
import { Box, Flex, ThemeUIStyleObject } from 'theme-ui';
import { flexRowJustifyCenterAlignCenter } from 'styles/shortcuts';
import { themeRadii } from 'styles/theme/radii';
import { effects } from 'styles/theme/effects';
import { ZINDEX } from 'styles/themeConstants';
import { Portal } from 'components/portal';
import { useEventListener } from 'usehooks-ts';
import useModal from 'components/modal/hooks/useModal';

const overlayVariants = {
  visible: {
    opacity: 1,
    transition: {
      when: 'beforeChildren',
      duration: 0.3,
      delayChildren: 0.4,
    },
  },
  hidden: {
    opacity: 0,
    transition: {
      when: 'afterChildren',
      duration: 0.3,
      delay: 0.4,
    },
  },
};

const ModalOverlay = m(Flex);
const modalOverlayStyle: ThemeUIStyleObject = {
  label: 'ModalOverlay',
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  background: 'rgba(0, 0, 0, 0.4)',
  ...flexRowJustifyCenterAlignCenter(0),
  backdropFilter: 'blur(5px)',
  zIndex: ZINDEX.MODAL,
};

const ModalContainer = m(Box);
const modalContainerStyle = {
  label: 'ModalContainer',
  maxWidth: ['100%', '100%', 500],
  width: ['100%', '100%', '80%'],
  backgroundColor: 'containerBackground',
  color: 'containerTextColor',
  borderRadius: themeRadii.large,
  p: 4,
  m: [2, 2, 0],
  ...effects.shadows.softShadowOnDark,
};

type Props = {
  children: React.ReactNode;
  open?: boolean;
  autoSize?: boolean;
  minHeight?: string | number;
  modalId: string;
  blockViewport?: boolean;
};

export const ModalResponsive: React.FC<Props> = ({
  children,
  modalId = '',
  minHeight,
  blockViewport = false,
  autoSize = false,
}) => {
  const { closeModal, modalViews, isModalOpen } = useModal(modalId);

  const closeThis = (e: React.MouseEvent<HTMLDivElement, MouseEvent>) => {
    if (blockViewport) return;
    if ((e.target as HTMLDivElement).id === modalId) {
      closeModal();
    }
  };

  const handleEscape = (e: KeyboardEvent) => {
    if (blockViewport) return;
    if (e.key === 'Escape') {
      const latestModal = modalViews.pop();
      if (latestModal?.id === modalId) {
        closeModal();
      }
    }
  };

  useEventListener('keydown', handleEscape);

  const modalContainer = {
    ...modalContainerStyle,
    ...(autoSize && {
      maxWidth: ['100%', '100%', '80%'],
      width: ['100%', '100%', 'auto'],
      height: 'auto',
    }),
    ...(minHeight && { height: minHeight }),
  };

  return (
    <Portal selector="#modal">
      <AnimatePresence>
        {isModalOpen && (
          <ModalOverlay
            sx={modalOverlayStyle}
            initial="hidden"
            animate="visible"
            exit="hidden"
            variants={overlayVariants}
            onClick={(e) => closeThis(e)}
            id={modalId}
          >
            <ModalContainer
              sx={modalContainer}
              initial={{ opacity: 0, y: '100vh' }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: '100vh' }}
              transition={{ ease: 'easeInOut', duration: 0.3 }}
            >
              {children}
            </ModalContainer>
          </ModalOverlay>
        )}
      </AnimatePresence>
    </Portal>
  );
};
