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

import React, { useContext } from 'react';
import { Flex, Grid } from 'theme-ui';
import { ButtonCalendar } from '../shared/buttonCalendar';
import {
  flexColumnJustifyCenterAlignCenter,
  flexRowJustifyCenterAlignCenter,
} from 'styles/shortcuts';
import useModal from 'components/modal/hooks/useModal';
import { ModalResponsive } from 'components/modal/modalResponsive';
import { FormElement } from 'components/forms/formElement';
import { CalendarDisplay } from 'components/forms/input/calendar/calendarDisplay';
import { formatISOLocale } from 'lib/dates';
import { Button } from '@theme-ui/components';
import { useTranslation } from 'next-i18next';
import { ISODateString } from 'next-auth';
import { InputDatepickerContext } from 'components/forms/input/datePicker/store/store';
import { DatePickerFieldDays } from 'components/forms/input/datePicker/datePickerFieldDays';
import { DatePickerFieldMonths } from 'components/forms/input/datePicker/datePickerFieldMonths';
import { DatePickerFieldYears } from 'components/forms/input/datePicker/datePickerFieldYears';
import { DatePickerFieldSeparator } from 'components/forms/input/datePicker/datePickerFieldSeparator';

export const DatePickerCalendar: React.FC = () => {
  const { t } = useTranslation('common');
  const { state, dispatch } = useContext(InputDatepickerContext);
  const { modalId, openModal, closeModal } = useModal(
    `inputDateTimePickerCalendar-${state.isoString}`
  );

  const changeDate = (selectedDate: ISODateString) => {
    dispatch({ type: 'setDateFromIsoString', payload: selectedDate });
    closeModal();
  };

  return (
    <>
      <Grid
        sx={{
          label: 'DateField',
          gridTemplateColumns: 'repeat(6, auto)',
          gap: 0,
        }}
      >
        <DatePickerFieldDays />
        <DatePickerFieldSeparator separator="." />
        <DatePickerFieldMonths />
        <DatePickerFieldSeparator separator="." />
        <DatePickerFieldYears />
        <Flex sx={{ pl: 3, ...flexRowJustifyCenterAlignCenter() }}>
          <ButtonCalendar onClick={openModal} />
        </Flex>
        <ModalResponsive autoSize modalId={modalId}>
          <FormElement>
            <Flex sx={{ ...flexColumnJustifyCenterAlignCenter(0) }}>
              <CalendarDisplay
                onChange={changeDate}
                value={formatISOLocale(state.date || new Date())}
              />
            </Flex>
          </FormElement>
          <FormElement>
            <Button variant="secondary" onClick={closeModal}>
              {t('Cancel')}
            </Button>
          </FormElement>
        </ModalResponsive>
      </Grid>
    </>
  );
};
