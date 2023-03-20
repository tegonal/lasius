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
import { Box, Input, Label } from 'theme-ui';
import { useTranslation } from 'next-i18next';
import { Button } from '@theme-ui/components';
import { FormBody } from 'components/forms/formBody';
import { FormElement } from 'components/forms/formElement';
import { FormProvider, useForm } from 'react-hook-form';
import { FormErrorBadge } from 'components/forms/formErrorBadge';
import { ModelsUserOrganisation } from 'lib/api/lasius';
import { createOrganisation, updateOrganisation } from 'lib/api/lasius/organisations/organisations';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useToast } from 'components/toasts/hooks/useToast';
import { useSWRConfig } from 'swr';
import { getGetUserProfileKey } from 'lib/api/lasius/user/user';

type Props = {
  item?: ModelsUserOrganisation;
  mode: 'add' | 'update';
  onSave: () => void;
  onCancel: () => void;
};

type Form = {
  organisationName: string;
};
export const OrganisationAddUpdateForm: React.FC<Props> = ({ item, onSave, onCancel, mode }) => {
  const hookForm = useForm<Form>();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { selectedOrganisationKey, setSelectedOrganisation } = useOrganisation();
  const { addToast } = useToast();
  const { t } = useTranslation('common');
  const { mutate } = useSWRConfig();

  useEffect(() => {
    if (selectedOrganisationKey && mode === 'update') {
      hookForm.setValue('organisationName', selectedOrganisationKey);
    }
  }, [hookForm, mode, selectedOrganisationKey]);

  const onSubmit = async () => {
    setIsSubmitting(true);
    const { organisationName } = hookForm.getValues();
    if (mode === 'add' && organisationName) {
      const newOrg = await createOrganisation({ key: organisationName });
      addToast({ message: t('Organisation created'), type: 'SUCCESS' });
      if (newOrg) {
        await setSelectedOrganisation({
          id: newOrg.id,
          key: newOrg.key,
        });
      }
    } else if (item) {
      await updateOrganisation(item.organisationReference.id, {
        ...item,
        key: organisationName,
      });
      addToast({ message: t('Organisation updated'), type: 'SUCCESS' });
    }
    setIsSubmitting(false);
    await mutate(getGetUserProfileKey());
    onSave();
  };

  return (
    <FormProvider {...hookForm}>
      <Box sx={{ width: '100%', position: 'relative' }}>
        <form onSubmit={hookForm.handleSubmit(onSubmit)}>
          <FormBody>
            <FormElement>
              <Label htmlFor="organisationName">{t('Organisation name')}</Label>
              <Input
                {...hookForm.register('organisationName', { required: true })}
                autoComplete="off"
              />
              <FormErrorBadge error={hookForm.formState.errors.organisationName} />
            </FormElement>
            <FormElement>
              <Button
                type="submit"
                disabled={isSubmitting}
                sx={{ position: 'relative', zIndex: 0 }}
              >
                {t('Save')}
              </Button>
              <Button type="button" variant="secondary" onClick={onCancel}>
                {t('Cancel')}
              </Button>
            </FormElement>
          </FormBody>
        </form>
      </Box>
    </FormProvider>
  );
};
