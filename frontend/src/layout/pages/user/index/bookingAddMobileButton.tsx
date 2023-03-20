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
import { IndexColumnTabs } from 'layout/pages/user/index/indexColumnTabs';
import { FormElementSpacer } from 'components/forms/formElementSpacer';
import { FormElement } from 'components/forms/formElement';
import useModal from 'components/modal/hooks/useModal';
import { Box, Grid } from 'theme-ui';

export const BookingAddMobileButton: React.FC = () => {
  const { modalId, openModal, closeModal } = useModal('BookingAddMobileModal');
  const { t } = useTranslation('common');

  return (
    <Box sx={{ label: 'BookingAddMobileButton' }}>
      <Button variant="primaryCircle" onClick={openModal}>
        <Icon name="add-interface-essential" size={24} />
      </Button>
      <ModalResponsive minHeight="575px" modalId={modalId}>
        <Grid
          sx={{
            gridTemplateRows: 'auto min-content min-content',
            height: '100%',
            gap: 0,
          }}
        >
          <IndexColumnTabs />
          <FormElementSpacer />
          <FormElement>
            <Button type="button" variant="secondary" onClick={closeModal}>
              {t('Close')}
            </Button>
          </FormElement>
        </Grid>
      </ModalResponsive>
    </Box>
  );
};
