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

import { useContextMenu } from 'components/contextMenuBar/hooks/useContextMenu';
import { Button } from '@theme-ui/components';
import { Icon } from 'components/shared/icon';
import React from 'react';
import { useTranslation } from 'next-i18next';
import { ContextButtonWrapper } from 'components/contextMenuBar/contextButtonWrapper';
import { ContextCompactButtonWrapper } from 'components/contextMenuBar/contextCompactButtonWrapper';
import { ModalConfirm } from 'components/modal/modalConfirm';
import { removeProjectUser } from 'lib/api/lasius/projects/projects';
import { ModelsUserProject } from 'lib/api/lasius';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useProfile } from 'lib/api/hooks/useProfile';

type Props = {
  variant?: 'default' | 'compact';
  item: ModelsUserProject;
};
export const ContextButtonLeaveProject: React.FC<Props> = ({ variant = 'default', item }) => {
  const { handleCloseAll } = useContextMenu();
  const { t } = useTranslation('common');
  const [showDialog, setShowDialog] = React.useState(false);
  const { selectedOrganisationId } = useOrganisation();
  const { userId } = useProfile();

  const handleConfirm = async () => {
    await removeProjectUser(selectedOrganisationId, item.projectReference.id, userId);
    handleCloseAll();
  };

  const handleCancel = async () => {
    setShowDialog(false);
  };

  const Wrapper = variant === 'compact' ? ContextCompactButtonWrapper : ContextButtonWrapper;

  return (
    <Wrapper>
      <Button
        variant="contextIcon"
        title={t('Leave this project')}
        aria-label={t('Leave this project')}
        onClick={() => setShowDialog(true)}
      >
        <Icon name="logout-2-interface-essential" size={24} />
      </Button>
      {showDialog && (
        <ModalConfirm
          text={{ action: t('Are you sure you want to leave this project?') }}
          onConfirm={handleConfirm}
          onCancel={handleCancel}
        />
      )}
    </Wrapper>
  );
};
