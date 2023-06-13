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
import { Box, Flex, Grid, Heading } from 'theme-ui';
import { useTranslation } from 'next-i18next';
import { UserCard } from 'components/shared/manageUserCard';
import { ManageUserInviteByEmailForm } from 'components/shared/manageUserInviteByEmailForm';
import { FormGroup } from 'components/forms/formGroup';
import { removeProjectUser, useGetProjectUserList } from 'lib/api/lasius/projects/projects';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { ModelsProject, ModelsUserProject } from 'lib/api/lasius';

type Props = {
  item: ModelsProject | ModelsUserProject;
  onSave: () => void;
  onCancel: () => void;
};

export const ManageProjectMembers: React.FC<Props> = ({ item }) => {
  const { t } = useTranslation('common');
  const { selectedOrganisationId } = useOrganisation();
  const projectId = 'id' in item ? item.id : item.projectReference.id;
  const projectOrganisationId =
    'organisationReference' in item ? item.organisationReference.id : selectedOrganisationId;

  const { data } = useGetProjectUserList(selectedOrganisationId, projectId);

  const handleUserInvite = () => {
    //
  };

  const handleUserRemove = async (userId: string) => {
    await removeProjectUser(selectedOrganisationId, projectId, userId);
  };

  return (
    <FormGroup>
      <Box sx={{ width: '100%', position: 'relative' }}>
        <Grid sx={{ gap: 3, gridTemplateColumns: '2fr 1fr' }}>
          <Box>
            <Heading as="h2" variant="headingUnderlined">
              <Flex sx={{ gap: 2 }}>{t('Project members')}</Flex>
              <Box sx={{ fontWeight: 400, fontSize: 1 }}>{data?.length}</Box>
            </Heading>
            <Grid sx={{ gap: 3, gridTemplateColumns: '1fr 1fr 1fr', pb: 3 }}>
              {data?.map((user) => (
                <UserCard user={user} key={user.id} onRemove={() => handleUserRemove(user.id)} />
              ))}
            </Grid>
          </Box>
          <Box>
            <Heading as="h2" variant="headingUnderlined">
              {t('Invite someone')}
            </Heading>
            <ManageUserInviteByEmailForm
              organisation={projectOrganisationId}
              project={projectId}
              onSave={handleUserInvite}
            />
          </Box>
        </Grid>
      </Box>
    </FormGroup>
  );
};
