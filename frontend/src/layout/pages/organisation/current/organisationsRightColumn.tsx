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
import { Box, Paragraph } from 'theme-ui';
import { useTranslation } from 'next-i18next';
import { FormElement } from 'components/forms/formElement';
import { Button } from '@theme-ui/components';
import { OrganisationAddUpdateForm } from 'layout/pages/organisation/current/organisationAddUpdateForm';
import { ModalResponsive } from 'components/modal/modalResponsive';
import useModal from 'components/modal/hooks/useModal';
import { useContextMenu } from 'components/contextMenuBar/hooks/useContextMenu';
import { isAdminOfCurrentOrg } from 'lib/api/functions/isAdminOfCurrentOrg';
import { useProfile } from 'lib/api/hooks/useProfile';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';

export const OrganisationsRightColumn: React.FC = () => {
  const { t } = useTranslation('common');
  const { modalId, openModal, closeModal } = useModal('AddOrganisationModal');
  const { handleCloseAll } = useContextMenu();
  const { profile } = useProfile();
  const { selectedOrganisation } = useOrganisation();
  const amIAdmin = isAdminOfCurrentOrg(profile);

  const addOrganisation = () => {
    openModal();
    handleCloseAll();
  };

  return (
    <Box sx={{ width: '100%', px: 4, pt: 5 }}>
      {!selectedOrganisation?.private && (
        <>
          {amIAdmin ? (
            <Paragraph variant="infoText">
              {t(
                'You are an administrator of this organisation. You can add and remove members and change the organisation name, or create a new one.'
              )}
            </Paragraph>
          ) : (
            <Paragraph variant="infoText">
              {t(
                "You are a member of this organisation and don't have the rights to add or remove members. Get in touch with an organisation administrator if you would like to invite someone."
              )}
            </Paragraph>
          )}
        </>
      )}
      <Paragraph variant="infoText">
        {t('Add a new organisation by clicking on the button below.')}
      </Paragraph>
      <FormElement>
        <Button onClick={() => addOrganisation()}>{t('Create organisation')}</Button>
      </FormElement>
      <ModalResponsive modalId={modalId}>
        <OrganisationAddUpdateForm mode="add" onSave={closeModal} onCancel={closeModal} />
      </ModalResponsive>
    </Box>
  );
};
