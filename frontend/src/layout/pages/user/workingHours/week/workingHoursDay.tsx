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

import React, { useEffect } from 'react';
import { Box, Flex } from 'theme-ui';
import { FormatDate } from 'components/shared/formatDate';
import { InputDatePicker } from 'components/forms/input/datePicker/inputDatePicker';
import { IsoDateString } from 'lib/api/apiDateHandling';
import { Button } from '@theme-ui/components';
import { Icon } from 'components/shared/icon';
import useModal from 'components/modal/hooks/useModal';
import { ModalResponsive } from 'components/modal/modalResponsive';
import { FormGroup } from 'components/forms/formGroup';
import { FormElement } from 'components/forms/formElement';
import { useTranslation } from 'next-i18next';
import { flexColumnJustifyCenterAlignCenter } from 'styles/shortcuts';
import { ModelsWorkingHoursWeekdays } from 'types/common';
import { FormProvider, useForm } from 'react-hook-form';
import { ModelsUserOrganisation } from 'lib/api/lasius';
import { updateWorkingHoursByOrganisation } from 'lib/api/lasius/user-organisations/user-organisations';
import { plannedWorkingHoursStub } from 'lib/stubPlannedWorkingHours';
import { useSWRConfig } from 'swr';
import { getGetUserProfileKey } from 'lib/api/lasius/user/user';
import { useToast } from 'components/toasts/hooks/useToast';
import { getHours, getMinutes } from 'date-fns';
import { round } from 'lodash';

type Props = {
  organisation: ModelsUserOrganisation | undefined;
  item: {
    day: ModelsWorkingHoursWeekdays;
    date: IsoDateString;
    value: IsoDateString;
    displayValue: string;
  };
};

export const WorkingHoursDay: React.FC<Props> = ({ item, organisation }) => {
  const { modalId, openModal, closeModal } = useModal(
    `EditWorkingHoursModal-${organisation?.organisationReference.id}-${item.day}`
  );
  const { t } = useTranslation('common');
  const hookForm = useForm();
  const { mutate } = useSWRConfig();
  const { addToast } = useToast();

  useEffect(() => {
    hookForm.setValue(item.day, item.value);
  }, [hookForm, item.day, item.value]);

  const onSubmit = async () => {
    const date = new Date(hookForm.getValues()[item.day]);
    const hours = round(getHours(date) + getMinutes(date) / 60, 2);
    await updateWorkingHoursByOrganisation(organisation?.organisationReference.id || '', {
      ...organisation,
      plannedWorkingHours: {
        ...(organisation?.plannedWorkingHours || plannedWorkingHoursStub),
        [item.day]: hours,
      },
    });
    await mutate(getGetUserProfileKey());
    addToast({ message: t('Working hours updated'), type: 'SUCCESS' });
    closeModal();
  };

  return (
    <FormProvider {...hookForm}>
      <Box sx={{ textAlign: 'center', lineHeight: 'normal' }}>
        <Box>
          <Flex sx={{ ...flexColumnJustifyCenterAlignCenter() }}>
            <Button
              variant="icon"
              sx={{
                display: 'flex',
                ...flexColumnJustifyCenterAlignCenter(2),
                textAlign: 'center',
              }}
              onClick={openModal}
            >
              <Box>
                <FormatDate date={item.date} format="dayNameShort" />
              </Box>
              <Box>{item.displayValue}</Box>
              <Icon name="pencil-2-interface-essential" size={18} />
            </Button>
          </Flex>
        </Box>
      </Box>
      <ModalResponsive autoSize modalId={modalId}>
        <FormGroup>
          <FormElement>
            <Box sx={{ textAlign: 'center' }}>
              <FormatDate date={item.date} format="dayNameShort" />
            </Box>
          </FormElement>
          <FormElement>
            <InputDatePicker name={item.day} withDate={false} />
          </FormElement>
          <FormElement>
            <Button onClick={() => onSubmit()}>{t('Save')}</Button>
          </FormElement>
        </FormGroup>
      </ModalResponsive>
    </FormProvider>
  );
};
