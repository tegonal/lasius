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

import React, { useEffect, useState } from 'react';
import { Box, Button } from 'theme-ui';
import { useTranslation } from 'next-i18next';
import { CardContainer } from 'components/cardContainer';
import { LoginLayout } from 'layout/pages/login/loginLayout';
import { Logo } from 'components/logo';
import { FormElement } from 'components/forms/formElement';
import { FormBody } from 'components/forms/formBody';
import { BoxInfo } from 'components/shared/notifications/boxInfo';

import { useRouter } from 'next/router';
import { SelectUserOrganisationModal } from 'components/shared/selectUserOrganisationModal';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import {
  acceptInvitation,
  declineInvitation,
} from 'lib/api/lasius/invitations-private/invitations-private';
import {
  ModelsEntityReference,
  ModelsInvitationStatusResponse,
  ModelsJoinOrganisationInvitation,
  ModelsJoinProjectInvitation,
} from 'lib/api/lasius';
import { TegonalFooter } from 'components/shared/tegonalFooter';
import { usePlausible } from 'next-plausible';
import { LasiusPlausibleEvents } from 'lib/telemetry/plausibleEvents';

type Props = {
  invitation: ModelsInvitationStatusResponse;
};

export const InvitationUserConfirm: React.FC<Props> = ({ invitation }) => {
  const { t } = useTranslation('common');
  const router = useRouter();
  const [orgAssignment, setOrgAssignment] = useState<ModelsEntityReference>();
  const { selectedOrganisation, organisations } = useOrganisation();
  const plausible = usePlausible<LasiusPlausibleEvents>();

  useEffect(() => {
    if (
      invitation.invitation.type === 'JoinProjectInvitation' &&
      organisations.find(
        (o) =>
          o.organisationReference.id ===
          (invitation.invitation as ModelsJoinProjectInvitation).sharedByOrganisationReference.id
      )
    ) {
      setOrgAssignment(
        (invitation.invitation as ModelsJoinProjectInvitation).sharedByOrganisationReference
      );
    } else {
      setOrgAssignment(selectedOrganisation?.organisationReference);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [organisations]);

  const handleAcceptInvite = async () => {
    plausible('invitation', {
      props: {
        status: 'accept',
      },
    });
    await acceptInvitation(invitation.invitation.id, {
      organisationReference: orgAssignment,
    });
    await router.push('/');
    await router.reload();
  };

  const handleRejectInvite = async () => {
    plausible('invitation', {
      props: {
        status: 'reject',
      },
    });
    await declineInvitation(invitation.invitation.id);
    await router.push('/');
    await router.reload();
  };

  return (
    <LoginLayout>
      <Logo />
      {invitation.invitation.type === 'JoinOrganisationInvitation' && (
        <BoxInfo>
          {t('You have been invited by {{inviter}} to join organisation {{organisation}}.', {
            inviter: invitation.invitation.createdBy.key,
            organisation: (invitation.invitation as ModelsJoinOrganisationInvitation)
              .organisationReference.key,
          })}
        </BoxInfo>
      )}
      {invitation.invitation.type === 'JoinProjectInvitation' && (
        <BoxInfo>
          {t('You have been invited by {{inviter}} to join project {{project}}.', {
            inviter: invitation.invitation.createdBy.key,
            project: (invitation.invitation as ModelsJoinProjectInvitation).projectReference.key,
          })}
        </BoxInfo>
      )}
      <CardContainer>
        <FormBody>
          {organisations && invitation.invitation.type === 'JoinProjectInvitation' && (
            <>
              <FormElement>
                <Box>{t('Select the organisation you would like to add this project to:')}</Box>
              </FormElement>
              <FormElement>
                <SelectUserOrganisationModal onSelect={setOrgAssignment} selected={orgAssignment} />
              </FormElement>
            </>
          )}
          <FormElement>
            <Button onClick={handleAcceptInvite}>{t('Accept invitation')}</Button>
            <Button variant="secondary" onClick={handleRejectInvite}>
              {t('Reject invitation')}
            </Button>
          </FormElement>
        </FormBody>
      </CardContainer>
      <TegonalFooter />
    </LoginLayout>
  );
};
