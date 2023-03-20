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

import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Button, Input, Label } from 'theme-ui';
import { isEmailAddress } from 'lib/validators';
import { GetServerSideProps, NextPage } from 'next';
import { FormErrorBadge } from 'components/forms/formErrorBadge';
import { getCsrfToken, signIn } from 'next-auth/react';
import { CardContainer } from 'components/cardContainer';
import { LoginLayout } from 'layout/pages/login/loginLayout';
import { Logo } from 'components/logo';
import { FormElement } from 'components/forms/formElement';
import { FormBody } from 'components/forms/formBody';
import { BoxWarning } from 'components/shared/notifications/boxWarning';
import { useRouter } from 'next/router';
import { serverSideTranslations } from 'next-i18next/serverSideTranslations';
import { useTranslation } from 'next-i18next';
import { LoginError } from 'dynamicTranslationStrings';
import { logger } from 'lib/logger';
import { TegonalFooter } from 'components/shared/tegonalFooter';
import { BoxInfo } from 'components/shared/notifications/boxInfo';

const Login: NextPage<{ csrfToken: string }> = ({ csrfToken }) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<keyof typeof LoginError>();
  const { t } = useTranslation('common');
  const router = useRouter();
  const { invitationId = null, email = null, registered = null } = router.query;

  const {
    register,
    handleSubmit,
    setFocus,
    getValues,
    setValue,
    formState: { errors },
  } = useForm({
    mode: 'onChange',
    defaultValues: { email: email || '', password: '', csrfToken },
  });

  useEffect(() => {
    setFocus('email');
  }, [setFocus]);

  const onSubmit = async () => {
    const data = getValues();
    setIsSubmitting(true);

    const res = await signIn('credentials', {
      redirect: false,
      email: data.email,
      password: data.password,
      callbackUrl: '/user/home',
    });

    if (res?.status === 401) {
      setError('usernameOrPasswordWrong');
      setValue('password', '');
      setFocus('email');
      logger.log(res);
    }

    if (!res?.error && invitationId) {
      await router.replace(`/join/${invitationId}`);
      return;
    }

    if (!res?.error && res?.url) {
      await router.push(res.url);
    }

    setIsSubmitting(false);
  };

  return (
    <LoginLayout>
      <Logo />
      {error && <BoxWarning>{LoginError[error]}</BoxWarning>}
      {registered && (
        <BoxInfo>
          {t(
            'Thank you for registering. You can now log in with your e-mail address and password. Welcome to Lasius!'
          )}
        </BoxInfo>
      )}
      <CardContainer>
        <form onSubmit={handleSubmit(onSubmit)}>
          <FormBody>
            <FormElement>
              <input
                {...register('csrfToken', { required: true })}
                type="hidden"
                defaultValue={csrfToken}
              />
              <Label htmlFor="email">{t('E-Mail')}</Label>
              <Input
                {...register('email', {
                  required: true,
                  validate: { isEmailAddress: (v) => isEmailAddress(v.toString()) },
                })}
                autoComplete="off"
                autoFocus
                type="email"
              />
              <FormErrorBadge error={errors.email} />
            </FormElement>
            <FormElement>
              <Label htmlFor="password">{t('Password')}</Label>
              <Input
                {...register('password', { required: true })}
                type="password"
                autoComplete="off"
              />
              <FormErrorBadge error={errors.password} />
            </FormElement>
            <FormElement>
              <Button disabled={isSubmitting} type="submit">
                {t('Sign in')}
              </Button>
            </FormElement>
          </FormBody>
        </form>
      </CardContainer>
      <TegonalFooter />
    </LoginLayout>
  );
};

export const getServerSideProps: GetServerSideProps = async (context) => {
  const { locale = '' } = context;
  return {
    props: {
      csrfToken: await getCsrfToken(context),
      ...(await serverSideTranslations(locale, ['common'])),
    },
  };
};

export default Login;
