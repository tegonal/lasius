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
import { Button } from '@theme-ui/components';
import useModal from 'components/modal/hooks/useModal';
import { ModalResponsive } from 'components/modal/modalResponsive';
import { FormElement } from 'components/forms/formElement';
import { useTranslation } from 'next-i18next';
import { Box } from 'theme-ui';
import { AvatarOrganisation } from 'components/shared/avatar/avatarOrganisation';
import { SelectUserOrganisationModal } from 'components/shared/selectUserOrganisationModal';
import { useIsClient } from 'usehooks-ts';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';

export const MODAL_SELECT_ORGANISATION = 'SelectOrganisationModal';

export const SelectUserOrganisation: React.FC = () => {
  const { t } = useTranslation('common');
  const { modalId, openModal, closeModal } = useModal(MODAL_SELECT_ORGANISATION);
  const { selectedOrganisationKey, selectedOrganisation } = useOrganisation();
  const isClient = useIsClient();

  if (!isClient) return null;
  return (
    <>
      <Button
        variant="iconMuted"
        sx={{ label: 'SelectUserOrganisation', width: 'auto', gap: 3, display: ['none', 'flex'] }}
        onClick={openModal}
      >
        <AvatarOrganisation name={selectedOrganisationKey || ''} size={24} />
        <Box>
          {selectedOrganisation?.private ? t('My personal organisation') : selectedOrganisationKey}
        </Box>
      </Button>
      <ModalResponsive modalId={modalId}>
        <SelectUserOrganisationModal />
        <FormElement>
          <Button variant="secondary" onClick={closeModal}>
            {t('Cancel')}
          </Button>
        </FormElement>
      </ModalResponsive>
    </>
  );
};
