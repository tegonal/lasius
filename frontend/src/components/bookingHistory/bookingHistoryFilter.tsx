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
import { Box, Heading } from 'theme-ui';
import { Button } from '@theme-ui/components';
import { useTranslation } from 'next-i18next';
import { FormElement } from 'components/forms/formElement';
import { useFormContext } from 'react-hook-form';
import { FormBody } from 'components/forms/formBody';
import { InputTagsAutocomplete } from 'components/forms/input/inputTagsAutocomplete';
import { InputSelectAutocomplete } from 'components/forms/input/inputSelectAutocomplete';
import { DateRangeFilter } from 'components/shared/dateRangeFilter';
import { dateOptions } from 'lib/dateOptions';
import { useProjects } from 'lib/api/hooks/useProjects';
import { useGetTagsByProject } from 'lib/api/lasius/user-organisations/user-organisations';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';

export const BookingHistoryFilter: React.FC = () => {
  const { t } = useTranslation('common');
  const { projectSuggestions } = useProjects();
  const formContext = useFormContext();
  const { selectedOrganisationId } = useOrganisation();
  const { data: projectTags } = useGetTagsByProject(
    selectedOrganisationId,
    formContext.watch('projectId')
  );

  const resetForm = () => {
    const { from, to } = dateOptions[0].dateRangeFn(new Date());
    formContext.setValue('from', from);
    formContext.setValue('to', to);
    formContext.setValue('dateRange', dateOptions[0].name);
    formContext.setValue('projectId', '');
    formContext.setValue('tags', []);
  };

  useEffect(() => {
    const subscription = formContext.watch((value, { name }) => {
      switch (name) {
        case 'projectId':
          formContext.setFocus('tags');
          break;
        default:
          break;
      }
    });
    return () => subscription.unsubscribe();
  }, [formContext]);

  return (
    <Box sx={{ label: 'ListsFilter', width: '100%' }}>
      <Heading variant="headingUnderlinedMuted">{t('Filter')}</Heading>
      <FormBody>
        <FormElement>
          <InputSelectAutocomplete suggestions={projectSuggestions()} name="projectId" />
        </FormElement>
        <FormElement>
          <InputTagsAutocomplete name="tags" suggestions={projectTags} />
        </FormElement>
        <DateRangeFilter name="dateRange" />
        <FormElement>
          <Button type="button" onClick={resetForm} variant="secondary">
            {t('Reset')}
          </Button>
        </FormElement>
      </FormBody>
    </Box>
  );
};
