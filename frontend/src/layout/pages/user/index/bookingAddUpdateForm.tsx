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

import React, { useEffect, useRef, useState } from 'react';
import { Box } from 'theme-ui';
import { useTranslation } from 'next-i18next';
import { Button } from '@theme-ui/components';
import { FormBody } from 'components/forms/formBody';
import { FormElement } from 'components/forms/formElement';
import { InputDatePicker } from 'components/forms/input/datePicker/inputDatePicker';
import { InputSelectAutocomplete } from 'components/forms/input/inputSelectAutocomplete';
import { InputTagsAutocomplete } from 'components/forms/input/inputTagsAutocomplete';
import {
  addHours,
  getHours,
  getMinutes,
  isAfter,
  isBefore,
  isToday,
  setHours,
  setMinutes,
} from 'date-fns';
import { formatISOLocale } from 'lib/dates';
import { DEFAULT_STRING_VALUE } from 'projectConfig/constants';
import { FormProvider, useForm } from 'react-hook-form';
import { ModelsTags } from 'types/common';
import { useStore } from 'storeContext/store';
import {
  addUserBookingByOrganisation,
  updateUserBooking,
} from 'lib/api/lasius/user-bookings/user-bookings';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useProjects } from 'lib/api/hooks/useProjects';
import { useGetTagsByProject } from 'lib/api/lasius/user-organisations/user-organisations';
import useModal from 'components/modal/hooks/useModal';
import { ModelsBooking } from 'lib/api/lasius';

type Props = {
  item?: ModelsBooking;
  mode: 'add' | 'update';
  onSave: () => void;
  onCancel: () => void;
};

type FormValues = {
  projectId: string;
  start: string;
  end: string;
  tags: ModelsTags[];
};

export const BookingAddUpdateForm: React.FC<Props> = ({ item, mode, onSave, onCancel }) => {
  const { t } = useTranslation('common');
  const {
    state: { calendar },
  } = useStore();
  const hookForm = useForm<FormValues>({
    defaultValues: {
      projectId: '',
      tags: [],
      end: '',
      start: '',
    },
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { selectedOrganisationId } = useOrganisation();
  const { projectSuggestions } = useProjects();
  const { data: projectTags } = useGetTagsByProject(
    selectedOrganisationId,
    hookForm.watch('projectId')
  );
  const { closeModal } = useModal('BookingAddMobileModal');
  const previousEndDate = useRef('');

  useEffect(() => {
    if (item) {
      hookForm.setValue('projectId', item.projectReference.id);
      hookForm.setValue('tags', item.tags);
      hookForm.setValue('start', formatISOLocale(new Date(item.start.dateTime)));
      hookForm.setValue('end', formatISOLocale(new Date(item?.end?.dateTime || '')));
      hookForm.trigger();
    }
    if (mode === 'add') {
      if (!isToday(new Date(calendar.selectedDate))) {
        const end = formatISOLocale(setHours(new Date(calendar.selectedDate), 12));
        hookForm.setValue('start', formatISOLocale(setHours(new Date(calendar.selectedDate), 8)));
        hookForm.setValue('end', end);
        previousEndDate.current = end;
      }

      if (isToday(new Date(calendar.selectedDate))) {
        const end = formatISOLocale(new Date());
        hookForm.setValue('start', formatISOLocale(addHours(new Date(), -1)));
        hookForm.setValue('end', end);
        previousEndDate.current = end;
      }

      hookForm.setValue('projectId', '');
      hookForm.setValue('tags', []);
    }

    //  Register validators with element names
    hookForm.register('start', {
      validate: {
        startBeforeEnd: (v) => isBefore(new Date(v), new Date(hookForm.getValues('end'))),
      },
    });

    hookForm.register('end', {
      validate: {
        endAfterStart: (v) => isAfter(new Date(v), new Date(hookForm.getValues('start'))),
      },
    });
  }, [
    hookForm,
    item?.projectReference.id,
    item?.tags,
    item?.start.dateTime,
    item,
    mode,
    calendar.selectedDate,
  ]);

  const onSubmit = async () => {
    const data = hookForm.getValues();
    const { projectId, tags = [], start, end } = data;

    if (projectId === DEFAULT_STRING_VALUE) {
      return;
    }
    setIsSubmitting(true);

    const payload = {
      ...(item || {}),
      start,
      end,
      projectId,
      tags,
    };

    if (mode === 'add') {
      await addUserBookingByOrganisation(selectedOrganisationId, payload);
    } else if (mode === 'update' && item) {
      await updateUserBooking(selectedOrganisationId, item.id, payload);
    }

    closeModal();
    onSave();

    setIsSubmitting(false);
  };

  useEffect(() => {
    const subscription = hookForm.watch((value, { name }) => {
      switch (name) {
        case 'projectId':
          if (value.projectId) {
            hookForm.setFocus('tags');
            hookForm.trigger();
          }
          break;
        case 'start':
          if (value.start && previousEndDate.current === value.end) {
            const time = [getHours(new Date(value.end)), getMinutes(new Date(value.end))];
            const endDate = formatISOLocale(
              setMinutes(setHours(new Date(value.start), time[0]), time[1])
            );
            hookForm.setValue('end', endDate);
            previousEndDate.current = endDate;
          }
          hookForm.trigger();
          break;
        case 'end':
          hookForm.trigger();
          break;
        default:
          break;
      }
    });
    return () => subscription.unsubscribe();
  }, [hookForm]);

  return (
    <FormProvider {...hookForm}>
      <Box sx={{ width: '100%', position: 'relative' }}>
        <form onSubmit={hookForm.handleSubmit(onSubmit)}>
          <FormBody>
            <FormElement>
              <InputSelectAutocomplete
                name="projectId"
                suggestions={projectSuggestions()}
                required
              />
            </FormElement>
            <FormElement>
              <InputTagsAutocomplete name="tags" suggestions={projectTags} />
            </FormElement>
            <FormElement>
              <InputDatePicker name="start" label={t('Starting time')} withDate />
            </FormElement>
            <FormElement>
              <InputDatePicker name="end" label={t('Ending time')} withDate />
            </FormElement>
            <FormElement>
              <Button type="submit" disabled={isSubmitting}>
                {t('Save')}
              </Button>
              <Button type="button" variant="secondary" onClick={onCancel}>
                {t('Close')}
              </Button>
            </FormElement>
          </FormBody>
        </form>
      </Box>
    </FormProvider>
  );
};
