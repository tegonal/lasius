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
import { LoginLayout } from 'layout/pages/login/loginLayout';
import { Logo } from 'components/logo';
import { BoxWarning } from 'components/shared/notifications/boxWarning';
import { Button } from 'theme-ui';
import { useRouter } from 'next/router';
import { FormBody } from 'components/forms/formBody';
import { FormElement } from 'components/forms/formElement';
import { useSignOut } from 'components/system/hooks/useSignOut';
import { ModelsInvitationStatusResponse } from 'lib/api/lasius';
import { TegonalFooter } from 'components/shared/tegonalFooter';
import { usePlausible } from 'next-plausible';
import { LasiusPlausibleEvents } from 'lib/telemetry/plausibleEvents';

type Props = {
  invitation: ModelsInvitationStatusResponse;
};

export const InvitationOtherSession: React.FC<Props> = ({ invitation }) => {
  const { t } = useTranslation('common');
  const router = useRouter();
  const { signOut } = useSignOut();

  const plausible = usePlausible<LasiusPlausibleEvents>();

  plausible('invitation', {
    props: {
      status: 'wrongUser',
    },
  });

  const handleSignOut = async () => {
    await signOut();
    await router.reload();
  };

  return (
    <LoginLayout>
      <Logo />
      <BoxWarning>
        {t(
          'This invitation has been created for someone else. Either log out and refresh, or forward the invitation link to the user {{email}}',
          { email: invitation.invitation.invitedEmail }
        )}
      </BoxWarning>
      <FormBody>
        <FormElement>
          <Button onClick={handleSignOut}>{t('Sign out and refresh')}</Button>
        </FormElement>
      </FormBody>
      <TegonalFooter />
    </LoginLayout>
  );
};
