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
import { Box, Button } from 'theme-ui';
import { Icon } from 'components/shared/icon';
import { AnimatePresence } from 'framer-motion';

import { useTranslation } from 'next-i18next';
import { ContextCompactAnimatePresence } from 'components/contextMenuBar/contextCompactAnimatePresence';
import { ContextCompactBar } from 'components/contextMenuBar/contextCompactBar';
import { ContextCompactButtonWrapper } from 'components/contextMenuBar/contextCompactButtonWrapper';
import { ContextCompactBody } from 'components/contextMenuBar/contextCompactBody';
import { ModelsUserProject } from 'lib/api/lasius';
import useModal from 'components/modal/hooks/useModal';
import { ModalResponsive } from 'components/modal/modalResponsive';
import { ProjectAddUpdateForm } from 'layout/pages/projects/sharedViews/projectAddUpdateForm';
import { FormElement } from 'components/forms/formElement';
import { ProjectBookingsCsvExport } from '../sharedViews/projectBookingsCsvExport';
import { ContextButtonOpen } from 'components/contextMenuBar/buttons/contextButtonOpen';
import { ContextButtonClose } from 'components/contextMenuBar/buttons/contextButtonClose';
import { useContextMenu } from 'components/contextMenuBar/hooks/useContextMenu';
import { ManageProjectMembers } from 'layout/pages/projects/sharedViews/manageMembers';
import { ContextButtonLeaveProject } from 'components/contextMenuBar/buttons/contextButtonLeaveProject';
import { ContextButtonDeactivateProject } from 'components/contextMenuBar/buttons/contextButtonDeactivateProject';
import { ProjectAddUpdateTagsForm } from 'layout/pages/projects/sharedViews/projectAddUpdateTagsForm';

type Props = {
  item: ModelsUserProject;
};

export const MyProjectsListItemAdministratorContext: React.FC<Props> = ({ item }) => {
  const updateModal = useModal(`EditProjectModal-${item.projectReference.id}`);
  const manageModal = useModal(`ManageProjectMembersModal-${item.projectReference.id}`);
  const statsModal = useModal(`StatsModal-${item.projectReference.id}`);
  const exportModal = useModal(`ExportModal-${item.projectReference.id}`);
  const tagModal = useModal(`TagModal-${item.projectReference.id}`);
  const { handleCloseAll, currentOpenContextMenuId } = useContextMenu();

  const { t } = useTranslation('common');

  const showStats = () => {
    statsModal.openModal();
    handleCloseAll();
  };

  const showExport = () => {
    exportModal.openModal();
    handleCloseAll();
  };

  const manageMembers = () => {
    manageModal.openModal();
    handleCloseAll();
  };

  const manageTags = () => {
    tagModal.openModal();
    handleCloseAll();
  };

  const updateProject = () => {
    updateModal.openModal();
    handleCloseAll();
  };

  return (
    <>
      <ContextCompactBody>
        <ContextButtonOpen hash={item.projectReference.id} />
        <AnimatePresence>
          {currentOpenContextMenuId === item.projectReference.id && (
            <ContextCompactAnimatePresence>
              <ContextCompactBar>
                <ContextCompactButtonWrapper>
                  <Button
                    variant="contextIcon"
                    title={t('Manage members')}
                    aria-label={t('Manage members')}
                    onClick={() => manageMembers()}
                  >
                    <Icon name="human-resources-search-team-work-office-companies" size={24} />
                  </Button>
                </ContextCompactButtonWrapper>
                <ContextCompactButtonWrapper>
                  <Button
                    variant="contextIcon"
                    title={t('Show statistics')}
                    aria-label={t('Show statistics')}
                    onClick={() => showStats()}
                  >
                    <Icon name="pie-line-graph-interface-essential" size={24} />
                  </Button>
                </ContextCompactButtonWrapper>
                <ContextCompactButtonWrapper>
                  <Button
                    variant="contextIcon"
                    title={t('Get billing reports as CSV')}
                    aria-label={t('Get billing reports as CSV')}
                    onClick={() => showExport()}
                  >
                    <Icon name="filter-text-interface-essential" size={24} />
                  </Button>
                </ContextCompactButtonWrapper>
                <ContextCompactButtonWrapper>
                  <Button
                    variant="contextIcon"
                    title={t('Edit project')}
                    aria-label={t('Edit project')}
                    onClick={() => updateProject()}
                  >
                    <Icon name="pencil-2-interface-essential" size={24} />
                  </Button>
                </ContextCompactButtonWrapper>
                <ContextCompactButtonWrapper>
                  <Button
                    variant="contextIcon"
                    title={t('Edit tags')}
                    aria-label={t('Edit tags')}
                    onClick={() => manageTags()}
                  >
                    <Icon name="tags-double-interface-essential" size={24} />
                  </Button>
                </ContextCompactButtonWrapper>
                <ContextButtonDeactivateProject item={item} variant="compact" />
                <ContextButtonLeaveProject item={item} variant="compact" />
                <ContextButtonClose variant="compact" />
              </ContextCompactBar>
            </ContextCompactAnimatePresence>
          )}
        </AnimatePresence>
      </ContextCompactBody>
      <ModalResponsive modalId={updateModal.modalId}>
        <ProjectAddUpdateForm
          mode="update"
          item={item}
          onSave={updateModal.closeModal}
          onCancel={updateModal.closeModal}
        />
      </ModalResponsive>
      <ModalResponsive modalId={tagModal.modalId} autoSize>
        <ProjectAddUpdateTagsForm
          mode="update"
          item={item}
          onSave={tagModal.closeModal}
          onCancel={tagModal.closeModal}
        />
      </ModalResponsive>
      <ModalResponsive modalId={manageModal.modalId} autoSize>
        <ManageProjectMembers
          item={item}
          onSave={manageModal.closeModal}
          onCancel={manageModal.closeModal}
        />
        <FormElement>
          <Button type="button" variant="secondary" onClick={manageModal.closeModal}>
            {t('Cancel')}
          </Button>
        </FormElement>
      </ModalResponsive>
      <ModalResponsive modalId={statsModal.modalId} autoSize>
        <Box>Placeholder</Box>
        <FormElement>
          <Button type="button" variant="secondary" onClick={statsModal.closeModal}>
            {t('Cancel')}
          </Button>
        </FormElement>
      </ModalResponsive>
      <ModalResponsive modalId={exportModal.modalId} autoSize>
        <ProjectBookingsCsvExport item={item.projectReference} />
        <FormElement>
          <Button type="button" variant="secondary" onClick={exportModal.closeModal}>
            {t('Cancel')}
          </Button>
        </FormElement>
      </ModalResponsive>
    </>
  );
};
