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
import { FormElement } from 'components/forms/formElement';
import { useTranslation } from 'next-i18next';
import { useForm } from 'react-hook-form';
import { Box, Heading, Input, Label } from 'theme-ui';
import { preventEnterOnForm } from 'components/forms/input/shared/preventEnterOnForm';
import { Button } from '@theme-ui/components';
import { emailValidationPattern } from 'lib/validators';
import { FormErrorBadge } from 'components/forms/formErrorBadge';
import { FormElementSpacer } from 'components/forms/formElementSpacer';
import { FormActions } from 'components/forms/formActions';
import { FormGroup } from 'components/forms/formGroup';
import { FormBodyAsColumns } from 'components/forms/formBodyAsColumns';
import { updateUserProfile } from 'lib/api/lasius/user/user';
import { useProfile } from 'lib/api/hooks/useProfile';
import { useIsClient } from 'usehooks-ts';
import { useToast } from 'components/toasts/hooks/useToast';

type Form = {
  email: string;
  firstName: string;
  lastName: string;
};

export const AccountForm: React.FC = () => {
  const hookForm = useForm<Form>({ defaultValues: { email: '', firstName: '', lastName: '' } });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { firstName, lastName, email, role } = useProfile();
  const isClient = useIsClient();
  const { addToast } = useToast();

  const { t } = useTranslation('common');

  useEffect(() => {
    hookForm.setValue('firstName', firstName);
    hookForm.setValue('lastName', lastName);
    hookForm.setValue('email', email);
  }, [email, firstName, hookForm, lastName]);

  const onSubmit = async () => {
    const data = hookForm.getValues();
    setIsSubmitting(true);
    await updateUserProfile(data);
    addToast({ message: t('Account information updated'), type: 'SUCCESS' });
    setIsSubmitting(false);
  };

  if (!isClient) return null;

  return (
    <Box sx={{ width: '100%', px: 4, pt: 3 }}>
      <Heading as="h2" variant="heading">
        {t('Profile information')}
      </Heading>
      {/* eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions */}
      <form onSubmit={hookForm.handleSubmit(onSubmit)} onKeyDown={(e) => preventEnterOnForm(e)}>
        <FormBodyAsColumns>
          <FormGroup>
            <FormElement>
              <Label htmlFor="role">{t('Role')}</Label>
              <Input readOnly value={role} tabIndex={-1} disabled />
            </FormElement>
            <FormElement>
              <Label htmlFor="firstName">{t('Firstname')}</Label>
              <Input {...hookForm.register('firstName', { required: true })} autoComplete="off" />
              <FormErrorBadge error={hookForm.formState.errors.firstName} />
            </FormElement>
            <FormElement>
              <Label htmlFor="lastName">{t('Lastname')}</Label>
              <Input {...hookForm.register('lastName', { required: true })} autoComplete="off" />
              <FormErrorBadge error={hookForm.formState.errors.lastName} />
            </FormElement>
            <FormElementSpacer />
            <FormElement>
              <Label htmlFor="email">{t('E-Mail')}</Label>
              <Input
                {...hookForm.register('email', { required: true, pattern: emailValidationPattern })}
                autoComplete="off"
              />
              <FormErrorBadge error={hookForm.formState.errors.email} />
            </FormElement>
          </FormGroup>
          <FormActions zIndex={1}>
            <Button type="submit" disabled={isSubmitting}>
              {t('Save changes')}
            </Button>
          </FormActions>
        </FormBodyAsColumns>
      </form>
    </Box>
  );
};
