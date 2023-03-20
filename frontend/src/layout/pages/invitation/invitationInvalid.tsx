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
import { TegonalFooter } from 'components/shared/tegonalFooter';

export const InvitationInvalid: React.FC = () => {
  const { t } = useTranslation('common');
  return (
    <LoginLayout>
      <Logo />
      <BoxWarning>
        {t(
          'This invitation is no longer valid. It is best to contact the person who sent you the invitation link to get a new one.'
        )}
      </BoxWarning>
      <TegonalFooter />
    </LoginLayout>
  );
};
