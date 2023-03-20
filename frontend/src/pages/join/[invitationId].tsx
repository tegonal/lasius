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

import { GetServerSideProps, NextPage } from 'next';
import { useRouter } from 'next/router';
import { useSession } from 'next-auth/react';
import { InvitationUserRegister } from 'layout/pages/invitation/invitationUserRegister';
import { InvitationInvalid } from 'layout/pages/invitation/invitationInvalid';
import { InvitationUserConfirm } from 'layout/pages/invitation/invitationUserConfirm';
import { serverSideTranslations } from 'next-i18next/serverSideTranslations';
import { InvitationOtherSession } from 'layout/pages/invitation/InvitationOtherSession';
import { getInvitationStatus } from 'lib/api/lasius/invitations-public/invitations-public';
import { useAsync } from 'react-async-hook';

const Join: NextPage = () => {
  const router = useRouter();
  const session = useSession();
  const { invitationId = '' } = router.query as { invitationId: string };

  const invitationStatus = useAsync((id: string) => getInvitationStatus(id), [invitationId]);

  if (invitationStatus.loading) return null;

  if (invitationStatus.status === 'error') {
    return <InvitationInvalid />;
  }

  const invitation = invitationStatus.result;

  if (
    invitation &&
    session.status !== 'authenticated' &&
    invitationStatus.result?.status === 'UnregisteredUser'
  ) {
    // handle user setup and confirm invitation
    return <InvitationUserRegister invitation={invitation} />;
  }

  if (invitation && session.status === 'authenticated') {
    // check if invitation matches current users session
    if (session.data.user && session.data.user.email !== invitation.invitation.invitedEmail) {
      return <InvitationOtherSession invitation={invitation} />;
    }

    // handle invite for logged in user, confirm
    return <InvitationUserConfirm invitation={invitation} />;
  }

  if (
    session.status !== 'authenticated' &&
    invitation?.invitation?.id &&
    invitation?.invitation?.invitedEmail
  ) {
    // login user and handle invitation logic again
    router.replace(
      `/login?invitationId=${invitation.invitation?.id}&email=${invitation.invitation?.invitedEmail}`
    );
  }

  return null;
};

export const getServerSideProps: GetServerSideProps = async (context) => {
  const { locale = '' } = context;
  return {
    props: {
      ...(await serverSideTranslations(locale, ['common'])),
    },
  };
};

export default Join;
