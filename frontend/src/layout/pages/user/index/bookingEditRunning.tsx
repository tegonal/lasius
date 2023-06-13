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
import { Box } from 'theme-ui';
import { useTranslation } from 'next-i18next';
import { Button } from '@theme-ui/components';
import { FormBody } from 'components/forms/formBody';
import { FormElement } from 'components/forms/formElement';
import { InputDatePicker } from 'components/forms/input/datePicker/inputDatePicker';
import { InputSelectAutocomplete } from 'components/forms/input/inputSelectAutocomplete';
import { InputTagsAutocomplete } from 'components/forms/input/inputTagsAutocomplete';
import { DEFAULT_STRING_VALUE } from 'projectConfig/constants';
import { FormProvider, useForm } from 'react-hook-form';
import { ModelsTags } from 'types/common';
import { formatISOLocale } from 'lib/dates';
import { addSeconds, isFuture } from 'date-fns';
import { logger } from 'lib/logger';
import { updateUserBooking } from 'lib/api/lasius/user-bookings/user-bookings';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useProjects } from 'lib/api/hooks/useProjects';
import { useGetTagsByProject } from 'lib/api/lasius/user-organisations/user-organisations';
import { ModelsBooking } from 'lib/api/lasius';
import { IconNames } from 'types/iconNames';
import { useGetBookingLatest } from 'lib/api/hooks/useGetBookingLatest';
import { useStore } from 'storeContext/store';

type Props = {
  item: ModelsBooking;
  onSave: () => void;
  onCancel: () => void;
};

type FormValues = {
  projectId: string;
  start: string;
  tags: ModelsTags[];
};

export const BookingEditRunning: React.FC<Props> = ({ item, onSave, onCancel }) => {
  const { t } = useTranslation('common');
  const hookForm = useForm<FormValues>({
    defaultValues: {
      projectId: '',
      tags: [],
      start: '',
    },
  });
  const {
    state: { calendar },
  } = useStore();
  const { data: latestBooking } = useGetBookingLatest(calendar.selectedDate);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const { selectedOrganisationId } = useOrganisation();
  const { projectSuggestions } = useProjects();
  const { data: projectTags } = useGetTagsByProject(
    selectedOrganisationId,
    hookForm.watch('projectId')
  );

  useEffect(() => {
    if (item) {
      hookForm.setValue('projectId', item.projectReference.id);
      hookForm.setValue('tags', item.tags);
      hookForm.setValue('start', formatISOLocale(new Date(item.start.dateTime)));
      hookForm.trigger();
    }
  }, [hookForm, item.projectReference.id, item.tags, item.start.dateTime, item]);

  const onSubmit = async (data: any) => {
    const { projectId, tags = [], start } = data;

    if (projectId === DEFAULT_STRING_VALUE) {
      return;
    }
    setIsSubmitting(true);

    const payload = {
      ...item,
      start,
      end: undefined,
      projectId,
      tags,
    };
    try {
      await updateUserBooking(selectedOrganisationId, item.id, payload);
      onSave();
    } catch (error) {
      logger.warn(error);
    }
    setIsSubmitting(false);
  };

  useEffect(() => {
    const subscription = hookForm.watch((value, { name }) => {
      switch (name) {
        case 'projectId':
          if (value.projectId) {
            hookForm.setFocus('tags');
          }
          break;
        default:
          break;
      }
    });
    return () => subscription.unsubscribe();
  }, [hookForm]);

  const presetStart = latestBooking
    ? {
        presetLabel: t('Use end time of latest booking as start time for this one'),
        presetDate: formatISOLocale(addSeconds(new Date(latestBooking?.end?.dateTime || ''), 1)),
        presetIcon: 'move-left-1' as IconNames,
      }
    : {};

  return (
    <Box sx={{ width: '100%', position: 'relative' }}>
      <FormProvider {...hookForm}>
        <form onSubmit={hookForm.handleSubmit(onSubmit)}>
          <FormBody>
            <FormElement>
              <InputSelectAutocomplete
                suggestions={projectSuggestions()}
                name="projectId"
                required
              />
            </FormElement>
            <FormElement>
              <InputTagsAutocomplete name="tags" suggestions={projectTags} />
            </FormElement>
            <FormElement>
              <InputDatePicker
                name="start"
                label={t('Starts')}
                withDate={false}
                rules={{
                  validate: {
                    startInPast: (v) => !isFuture(new Date(v)),
                  },
                }}
                {...presetStart}
              />
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
      </FormProvider>
    </Box>
  );
};
