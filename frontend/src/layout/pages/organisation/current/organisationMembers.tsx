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
import { ModelsUserOrganisation } from 'lib/api/lasius';
import { UserCard } from 'components/shared/manageUserCard';
import { ManageUserInviteByEmailForm } from 'components/shared/manageUserInviteByEmailForm';
import { FormGroup } from 'components/forms/formGroup';
import { ROLES } from 'projectConfig/constants';
import {
  removeOrganisationUser,
  useGetOrganisationUserList,
} from 'lib/api/lasius/organisations/organisations';
import { useProfile } from 'lib/api/hooks/useProfile';
import { isAdminOfCurrentOrg } from 'lib/api/functions/isAdminOfCurrentOrg';

type Props = {
  item: ModelsUserOrganisation | undefined;
};

export const OrganisationMembers: React.FC<Props> = ({ item }) => {
  const { t } = useTranslation('common');
  const { data: userList } = useGetOrganisationUserList(item?.organisationReference.id || '');
  const { profile } = useProfile();
  const amIAdmin = isAdminOfCurrentOrg(profile);
  const handleUserInvite = () => {
    //
  };

  const handleUserRemove = (userId: string) => {
    (async () => {
      if (item) {
        await removeOrganisationUser(item.organisationReference.id, userId);
      }
    })();
  };

  const canAdmin = () => item?.role === ROLES.ORGANISATION_ADMIN && !item?.private;

  return (
    <FormGroup>
      <Box sx={{ width: '100%', position: 'relative' }}>
        <Grid sx={{ gap: 3, gridTemplateColumns: canAdmin() ? '2fr 1fr' : '1fr' }}>
          <Box>
            <Heading as="h2" variant="headingUnderlined">
              <Flex sx={{ gap: 2 }}>{t('Members')}</Flex>
              <Box sx={{ fontWeight: 400, fontSize: 1 }}>{userList?.length}</Box>
            </Heading>
            <Grid sx={{ gap: 3, gridTemplateColumns: '1fr 1fr 1fr', pb: 3 }}>
              {userList?.map((user) => (
                <UserCard
                  canRemove={amIAdmin}
                  user={user}
                  key={user.id}
                  onRemove={() => handleUserRemove(user.id)}
                />
              ))}
            </Grid>
          </Box>
          {canAdmin() && (
            <Box>
              <Heading as="h2" variant="headingUnderlined">
                {t('Invite someone')}
              </Heading>
              <ManageUserInviteByEmailForm
                organisation={item?.organisationReference.id}
                onSave={handleUserInvite}
              />
            </Box>
          )}
        </Grid>
      </Box>
    </FormGroup>
  );
};
