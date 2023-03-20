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
import { useTranslation } from 'next-i18next';
import { Box, Button, Flex, Heading } from 'theme-ui';
import { ModalResponsive } from 'components/modal/modalResponsive';
import { OrganisationAddUpdateForm } from 'layout/pages/organisation/current/organisationAddUpdateForm';
import { OrganisationMembers } from 'layout/pages/organisation/current/organisationMembers';
import useModal from 'components/modal/hooks/useModal';
import { ROLES } from 'projectConfig/constants';
import { Icon } from 'components/shared/icon';
import {
  flexColumnJustifyCenterAlignCenter,
  flexRowJustifyBetweenAlignCenter,
} from 'styles/shortcuts';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useContextMenu } from 'components/contextMenuBar/hooks/useContextMenu';
import { useIsClient } from 'usehooks-ts';

export const OrganisationDetail: React.FC = () => {
  const { t } = useTranslation('common');
  const updateModal = useModal(`EditOrganisationModal`);
  const { selectedOrganisation } = useOrganisation();
  const { handleCloseAll } = useContextMenu();
  const isClient = useIsClient();

  const editOrganisation = () => {
    updateModal.openModal();
    handleCloseAll();
  };

  if (!isClient) return null;

  return (
    <>
      <Heading
        as="h2"
        variant="heading"
        sx={{ display: 'flex', ...flexRowJustifyBetweenAlignCenter() }}
      >
        <Box>
          {selectedOrganisation?.private
            ? t('My personal organisation')
            : selectedOrganisation?.organisationReference.key}
        </Box>
        {selectedOrganisation?.role === ROLES.ORGANISATION_ADMIN &&
          !selectedOrganisation?.private && (
            <Button
              variant="primarySmall"
              title={t('Edit organisation')}
              aria-label={t('Edit organisation')}
              onClick={() => editOrganisation()}
              sx={{ width: 'auto' }}
            >
              {t('Edit organisation')}
            </Button>
          )}
      </Heading>
      {selectedOrganisation?.private && (
        <Flex sx={{ ...flexColumnJustifyCenterAlignCenter(3), mb: 4 }}>
          <Icon name="lock-1-interface-essential" size={24} />
          <Box>
            {t(
              'This organization is only visible to you. You can use this to track private projects that do not require access from others. If you wish to invite people, invite them to an existing organisation or create a new one.'
            )}
          </Box>
        </Flex>
      )}
      {!selectedOrganisation?.private && <OrganisationMembers item={selectedOrganisation} />}

      {selectedOrganisation?.role === ROLES.ORGANISATION_ADMIN ? (
        <>
          <ModalResponsive modalId={updateModal.modalId}>
            <OrganisationAddUpdateForm
              mode="update"
              item={selectedOrganisation}
              onSave={updateModal.closeModal}
              onCancel={updateModal.closeModal}
            />
          </ModalResponsive>
        </>
      ) : null}
    </>
  );
};
