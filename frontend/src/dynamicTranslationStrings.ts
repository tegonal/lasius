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

/*
Dynamic translation strings, to be picked up by the extractor:
 */

const t = (s: string) => s;

export const LoginError = {
  usernameOrPasswordWrong: t('Wrong password or e-mail. Try again.'),
  oneOfCredentialsMissing: t('Username or password missing'),
};

export const FormError = {
  required: t('Required'),
  pattern: t('Wrong format'),
  isEmailAddress: t('Should be a valid e-mail address'),
  notEnoughCharactersPassword: t('Not enough characters (min. 8)'),
  noUppercase: t('Missing uppercase character'),
  noSpecialCharacters: t('Missing special character'),
  notEqualPassword: t("Passwords don't match"),
  startInPast: t('Must be in the past'),
  endAfterStart: t('Must be after start'),
  startBeforeEnd: t('Must be before end'),
  noNumber: t('Missing number digit'),
  toAfterFrom: t('Must be after the "from" date'),
  fromBeforeTo: t('Must be before the "to" date'),
};

export const PageError: Record<string, string> = {
  404: t('Page not found'),
  500: t('Internal server error'),
  401: t('Unauthorized'),
  undefined: t('Something went wrong'),
};

export const UserRoles = {
  ProjectAdministrator: t('Administrator'),
  ProjectMember: t('Member'),
  OrganisationAdministrator: t('Administrator'),
  OrganisationMember: t('Member'),
};
