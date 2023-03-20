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
import { ModalResponsive } from 'components/modal/modalResponsive';
import { Button } from '@theme-ui/components';
import { useTranslation } from 'next-i18next';
import { Icon } from 'components/shared/icon';
import { FormElement } from 'components/forms/formElement';
import { FormElementSpacer } from 'components/forms/formElementSpacer';
import useModal from 'components/modal/hooks/useModal';
import { FormGroup } from 'components/forms/formGroup';
import { MODAL_SELECT_ORGANISATION } from 'components/shared/selectUserOrganisation';
import { useSignOut } from 'components/system/hooks/useSignOut';

export const MobileNavigationButton: React.FC = () => {
  const { modalId, openModal, closeModal } = useModal('MobileNavigationButtonModal');
  const { t } = useTranslation('common');
  const organisationSelectModal = useModal(MODAL_SELECT_ORGANISATION);
  const { signOut } = useSignOut();

  return (
    <>
      <Button variant="secondaryCircle" onClick={openModal}>
        <Icon name="navigation-menu-vertical-interface-essential" size={24} />
      </Button>
      <ModalResponsive modalId={modalId}>
        <FormGroup>
          <FormElement>
            <Button
              onClick={organisationSelectModal.openModal}
              aria-label={t('Switch organisation')}
            >
              {t('Switch organisation')}
            </Button>
          </FormElement>
          <FormElement>
            <Button onClick={signOut} aria-label={t('Sign out')}>
              {t('Sign out')}
            </Button>
          </FormElement>
          <FormElementSpacer />
          <FormElement>
            <Button type="button" variant="secondary" onClick={closeModal} aria-label={t('Cancel')}>
              {t('Cancel')}
            </Button>
          </FormElement>
        </FormGroup>
      </ModalResponsive>
    </>
  );
};
