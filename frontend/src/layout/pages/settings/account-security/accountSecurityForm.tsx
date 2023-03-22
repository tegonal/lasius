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

import React, { useState } from 'react';
import { FormElement } from 'components/forms/formElement';
import { useTranslation } from 'next-i18next';
import { useForm } from 'react-hook-form';
import { Box, Heading, Input, Label } from 'theme-ui';
import { preventEnterOnForm } from 'components/forms/input/shared/preventEnterOnForm';
import { Button } from '@theme-ui/components';
import { FormErrorBadge } from 'components/forms/formErrorBadge';
import { FormActions } from 'components/forms/formActions';
import { FormGroup } from 'components/forms/formGroup';
import { FormBodyAsColumns } from 'components/forms/formBodyAsColumns';
import { FormElementSpacer } from 'components/forms/formElementSpacer';
import { FormErrorsMultiple } from 'components/forms/formErrorsMultiple';
import { Icon } from 'components/shared/icon';
import { updateUserPassword } from 'lib/api/lasius/user/user';
import { useProfile } from 'lib/api/hooks/useProfile';
import { useIsClient } from 'usehooks-ts';
import { useToast } from 'components/toasts/hooks/useToast';
import { LASIUS_DEMO_MODE } from 'projectConfig/constants';

type Form = {
  password: string;
  newPassword: string;
  confirmPassword: string;
};
export const AccountSecurityForm: React.FC = () => {
  const [showPasswords, setShowPasswords] = useState<boolean>(false);
  const hookForm = useForm<Form>({
    defaultValues: { password: '', newPassword: '', confirmPassword: '' },
    mode: 'onChange',
    criteriaMode: 'all',
  });
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const { profile } = useProfile();
  const isClient = useIsClient();
  const { addToast } = useToast();

  const { t } = useTranslation('common');

  const resetForm = () => {
    if (profile) {
      hookForm.reset({ password: '', newPassword: '', confirmPassword: '' });
    }
  };

  const onSubmit = async (data: any) => {
    if (LASIUS_DEMO_MODE === 'true') {
      addToast({ message: t('Profile changes are not allowed in demo mode'), type: 'ERROR' });
      resetForm();
      setIsSubmitting(false);
      return;
    }
    setIsSubmitting(true);
    const { password, newPassword } = data;
    const payload = {
      password,
      newPassword,
    };
    await updateUserPassword(payload);
    addToast({ message: t('Password updated'), type: 'SUCCESS' });
    resetForm();
    setIsSubmitting(false);
  };

  const handleTogglePasswordsVisible = (e: { preventDefault: () => void }) => {
    e.preventDefault();
    setShowPasswords(!showPasswords);
  };

  if (!isClient) return null;

  return (
    <Box sx={{ width: '100%', px: 4, pt: 3 }}>
      <Heading as="h2" variant="heading">
        {t('Account Security')}
      </Heading>
      {/* eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions */}
      <form onSubmit={hookForm.handleSubmit(onSubmit)} onKeyDown={(e) => preventEnterOnForm(e)}>
        <FormBodyAsColumns>
          <FormGroup>
            <FormElement>
              <Label htmlFor="password">{t('Password')}</Label>
              <Input
                {...hookForm.register('password', { required: true })}
                autoComplete="off"
                type={showPasswords ? 'text' : 'password'}
              />
              <FormErrorBadge error={hookForm.formState.errors.password} />
            </FormElement>
            <FormElementSpacer />
            <FormElement>
              <Label htmlFor="newPassword">{t('New password')}</Label>
              <Input
                {...hookForm.register('newPassword', {
                  required: true,
                  validate: {
                    notEnoughCharactersPassword: (value: string) => value.length > 8,
                    // notEqualPassword: (value: string) => value !== hookForm.getValues('password'),
                    noUppercase: (value: string) => /(?=.*[A-Z])/.test(value),
                    noNumber: (value: string) => /\d/.test(value),
                    // noSpecialCharacters: (value: string) =>
                    //   /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]+/.test(value),
                  },
                })}
                autoComplete="off"
                type={showPasswords ? 'text' : 'password'}
              />
              <FormErrorsMultiple errors={hookForm.formState.errors.newPassword} />
            </FormElement>
            <FormElement>
              <Label htmlFor="email">{t('Confirm new password')}</Label>
              <Input
                {...hookForm.register('confirmPassword', {
                  required: true,
                  validate: {
                    notEqualPassword: (value: string) =>
                      value === hookForm.getValues('newPassword'),
                  },
                })}
                autoComplete="off"
                type={showPasswords ? 'text' : 'password'}
              />
              <FormErrorBadge error={hookForm.formState.errors.confirmPassword} />
            </FormElement>
            <FormElement>
              <Button onClick={handleTogglePasswordsVisible} variant="iconText">
                <Icon
                  name={
                    showPasswords ? 'view-1-interface-essential' : 'view-off-interface-essential'
                  }
                  size={24}
                />
                {showPasswords ? (
                  <Box>{t('Hide passwords')}</Box>
                ) : (
                  <Box>{t('Show passwords')}</Box>
                )}
              </Button>
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
