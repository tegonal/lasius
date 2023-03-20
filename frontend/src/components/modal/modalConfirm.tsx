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

import useModal from 'components/modal/hooks/useModal';
import { ModalResponsive } from 'components/modal/modalResponsive';
import { FormElement } from 'components/forms/formElement';
import { Button } from '@theme-ui/components';
import { useTranslation } from 'next-i18next';
import React from 'react';
import { Box } from 'theme-ui';
import { useEffectOnce } from 'usehooks-ts';

type Props = {
  text?: {
    action: string;
    confirm?: string;
    cancel?: string;
  };
  dangerLevel?: 'notification' | 'destructive';
  onCancel?: () => void;
  onConfirm: () => void;
  children?: React.ReactNode;
  hideButtons?: boolean;
};

export const ModalConfirm: React.FC<Props> = ({
  text,
  onConfirm,
  onCancel,
  dangerLevel = 'notification',
  children,
  hideButtons = false,
}) => {
  const { modalId, closeModal, openModal } = useModal('ConfirmationDialog');
  const { t } = useTranslation('common');

  const handleConfirm = () => {
    onConfirm();
    closeModal();
  };

  const handleCancel = () => {
    if (onCancel) onCancel();
    closeModal();
  };

  useEffectOnce(() => {
    openModal();
  });

  return (
    <ModalResponsive modalId={modalId} blockViewport>
      {text && <Box sx={{ mb: 3 }}>{text.action}</Box>}
      {children && <Box sx={{ mb: 3 }}>{children}</Box>}
      {!hideButtons && (
        <FormElement>
          <Button
            variant={dangerLevel === 'notification' ? 'primary' : 'secondary'}
            onClick={handleConfirm}
          >
            {text?.confirm || t('Ok')}
          </Button>
          {onCancel && (
            <Button variant="secondary" onClick={handleCancel}>
              {text?.cancel || t('Cancel')}
            </Button>
          )}
        </FormElement>
      )}
    </ModalResponsive>
  );
};
