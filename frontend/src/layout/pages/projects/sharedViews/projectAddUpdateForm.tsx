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
import { Input, Label } from 'theme-ui';
import { useTranslation } from 'next-i18next';
import { Button } from '@theme-ui/components';
import { FormBody } from 'components/forms/formBody';
import { FormElement } from 'components/forms/formElement';
import { FormProvider, useForm } from 'react-hook-form';
import { FormErrorBadge } from 'components/forms/formErrorBadge';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import {
  createProject,
  getGetProjectListKey,
  updateProject,
} from 'lib/api/lasius/projects/projects';
import { ModelsProject, ModelsUserProject } from 'lib/api/lasius';
import { useSWRConfig } from 'swr';
import { getGetUserProfileKey } from 'lib/api/lasius/user/user';
import { useToast } from 'components/toasts/hooks/useToast';

type Props = {
  item?: ModelsProject | ModelsUserProject;
  mode: 'add' | 'update';
  onSave: () => void;
  onCancel: () => void;
};

type FormValues = {
  projectKey: string;
};

export const ProjectAddUpdateForm: React.FC<Props> = ({ item, onSave, onCancel, mode }) => {
  const { t } = useTranslation('common');

  const hookForm = useForm<FormValues>({
    defaultValues: {
      projectKey: '',
    },
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [originalProjectName, setOriginalProjectName] = useState('');
  const { selectedOrganisationId } = useOrganisation();
  const { addToast } = useToast();

  const { mutate } = useSWRConfig();

  const projectId = (item && 'id' in item ? item.id : item?.projectReference.id) || '';
  const projectKey = (item && 'key' in item ? item.key : item?.projectReference.key) || '';
  const projectOrganisationId =
    (item && 'organisationReference' in item
      ? item.organisationReference.id
      : selectedOrganisationId) || selectedOrganisationId;

  useEffect(() => {
    if (item) {
      hookForm.setValue('projectKey', projectKey);
      setOriginalProjectName(projectKey);
    }
  }, [hookForm, item, projectKey]);

  const onSubmit = async () => {
    setIsSubmitting(true);
    const { projectKey } = hookForm.getValues();

    if (mode === 'add' && projectKey) {
      await createProject(projectOrganisationId, {
        key: projectKey,
        bookingCategories: [],
      });
    } else if (mode === 'update' && item) {
      await updateProject(projectOrganisationId, projectId, {
        ...item,
        ...(projectKey !== originalProjectName && { key: projectKey }),
      });
    }
    addToast({ message: t('Project updated'), type: 'SUCCESS' });
    await mutate(getGetProjectListKey(projectOrganisationId));
    await mutate(getGetUserProfileKey());
    setIsSubmitting(false);
    onSave();
  };

  return (
    <FormProvider {...hookForm}>
      <form onSubmit={hookForm.handleSubmit(onSubmit)}>
        <FormBody>
          <FormElement>
            <Label htmlFor="projectKey">{t('Project name')}</Label>
            <Input {...hookForm.register('projectKey', { required: true })} autoComplete="off" />
            <FormErrorBadge error={hookForm.formState.errors.projectKey} />
          </FormElement>
        </FormBody>

        <FormElement>
          <Button type="submit" disabled={isSubmitting} sx={{ position: 'relative', zIndex: 0 }}>
            {t('Save')}
          </Button>
          <Button type="button" variant="secondary" onClick={onCancel}>
            {t('Cancel')}
          </Button>
        </FormElement>
      </form>
    </FormProvider>
  );
};
