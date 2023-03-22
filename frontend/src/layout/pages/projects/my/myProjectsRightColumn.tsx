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
import { Box, Heading, Paragraph } from 'theme-ui';
import { useTranslation } from 'next-i18next';
import { FormElement } from 'components/forms/formElement';
import { Button } from '@theme-ui/components';
import { ModalResponsive } from 'components/modal/modalResponsive';
import useModal from 'components/modal/hooks/useModal';
import { ProjectAddUpdateForm } from 'layout/pages/projects/sharedViews/projectAddUpdateForm';
import { useContextMenu } from 'components/contextMenuBar/hooks/useContextMenu';

export const MyProjectsRightColumn: React.FC = () => {
  const { t } = useTranslation('common');

  const { modalId, openModal, closeModal } = useModal('AddMyProjectModal');
  const { handleCloseAll } = useContextMenu();

  const addProject = () => {
    openModal();
    handleCloseAll();
  };

  return (
    <Box sx={{ width: '100%', px: 4, pt: 3 }}>
      <Heading as="h2" variant="heading">
        {t('My projects')}
      </Heading>
      <Paragraph variant="infoText">
        {t(
          'Projects where you are a member and can book time. Restricted by the currently selected organisation.'
        )}
      </Paragraph>
      <FormElement>
        <Button onClick={() => addProject()}>{t('Create a project')}</Button>
      </FormElement>
      <ModalResponsive modalId={modalId}>
        <ProjectAddUpdateForm mode="add" onSave={closeModal} onCancel={closeModal} />
      </ModalResponsive>
    </Box>
  );
};
