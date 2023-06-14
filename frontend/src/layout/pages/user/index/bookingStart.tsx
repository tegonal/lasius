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
import { InputSelectAutocomplete } from 'components/forms/input/inputSelectAutocomplete';
import { InputTagsAutocomplete } from 'components/forms/input/inputTagsAutocomplete';
import { Icon } from 'components/shared/icon';
import { FormProvider, useForm } from 'react-hook-form';
import { ModelsTags } from 'types/common';
import { startUserBookingCurrent } from 'lib/api/lasius/user-bookings/user-bookings';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useProjects } from 'lib/api/hooks/useProjects';
import { useGetTagsByProject } from 'lib/api/lasius/user-organisations/user-organisations';
import useModal from 'components/modal/hooks/useModal';
import { formatISOLocale } from 'lib/dates';
import { roundToNearestMinutes } from 'date-fns';

type FormValues = {
  projectId: string;
  tags: ModelsTags[];
};

export const BookingStart: React.FC = () => {
  const { t } = useTranslation('common');
  const hookForm = useForm<FormValues>({
    mode: 'onSubmit',
    defaultValues: { projectId: '', tags: [] },
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { projectSuggestions } = useProjects();
  const { selectedOrganisationId } = useOrganisation();
  const { data: projectTags } = useGetTagsByProject(
    selectedOrganisationId,
    hookForm.watch('projectId')
  );
  const { closeModal } = useModal('BookingAddMobileModal');

  const resetComponent = () => {
    hookForm.setValue('projectId', '');
    hookForm.setValue('tags', []);
  };

  const onSubmit = async () => {
    const data = hookForm.getValues();
    setIsSubmitting(true);
    const { projectId, tags = [] } = data;
    if (projectId) {
      await startUserBookingCurrent(selectedOrganisationId, {
        projectId,
        tags,
        start: formatISOLocale(roundToNearestMinutes(new Date(), { roundingMethod: 'floor' })),
      });
      closeModal();
      resetComponent();
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hookForm]);

  return (
    <Box sx={{ label: '', width: '100%', position: 'relative' }}>
      <FormProvider {...hookForm}>
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
              <InputTagsAutocomplete suggestions={projectTags} name="tags" />
            </FormElement>
            <FormElement>
              <Button type="submit" disabled={isSubmitting}>
                <Icon name="stopwatch-interface-essential" size={24} />
                {t('Start booking')}
              </Button>
            </FormElement>
          </FormBody>
        </form>
      </FormProvider>
    </Box>
  );
};
