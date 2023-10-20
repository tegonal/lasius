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

/**
 * Generated by orval v6.17.0 🍺
 * Do not edit manually.
 * Lasius API
 * Track your time
 * OpenAPI spec version: 1.0.4+1-15ad669d+20231019-0610
 */
import useSwr from 'swr';
import type { SWRConfiguration, Key } from 'swr';
import type { ModelsInvitationStatusResponse, ModelsInvitation, ModelsUserRegistration } from '..';
import { lasiusAxiosInstance } from '../../lasiusAxiosInstance';
import type { ErrorType, BodyType } from '../../lasiusAxiosInstance';

// eslint-disable-next-line
  type SecondParameter<T extends (...args: any) => any> = T extends (
  config: any,
  args: infer P
) => any
  ? P
  : never;

/**
 * @summary get status of an invitation
 */
export const getInvitationStatus = (
  invitationId: string,
  options?: SecondParameter<typeof lasiusAxiosInstance>
) => {
  return lasiusAxiosInstance<ModelsInvitationStatusResponse>(
    { url: `/invitations/${invitationId}/status`, method: 'get' },
    options
  );
};

export const getGetInvitationStatusKey = (invitationId: string) =>
  [`/invitations/${invitationId}/status`] as const;

export type GetInvitationStatusQueryResult = NonNullable<
  Awaited<ReturnType<typeof getInvitationStatus>>
>;
export type GetInvitationStatusQueryError = ErrorType<void>;

/**
 * @summary get status of an invitation
 */
export const useGetInvitationStatus = <TError = ErrorType<void>>(
  invitationId: string,
  options?: {
    swr?: SWRConfiguration<Awaited<ReturnType<typeof getInvitationStatus>>, TError> & {
      swrKey?: Key;
      enabled?: boolean;
    };
    request?: SecondParameter<typeof lasiusAxiosInstance>;
  }
) => {
  const { swr: swrOptions, request: requestOptions } = options ?? {};

  const isEnabled = swrOptions?.enabled !== false && !!invitationId;
  const swrKey =
    swrOptions?.swrKey ?? (() => (isEnabled ? getGetInvitationStatusKey(invitationId) : null));
  const swrFn = () => getInvitationStatus(invitationId, requestOptions);

  const query = useSwr<Awaited<ReturnType<typeof swrFn>>, TError>(swrKey, swrFn, swrOptions);

  return {
    swrKey,
    ...query,
  };
};

/**
 * @summary --------please annotate------
 */
export const registerInvitationUser = (
  invitationId: string,
  modelsUserRegistration: BodyType<ModelsUserRegistration>,
  options?: SecondParameter<typeof lasiusAxiosInstance>
) => {
  return lasiusAxiosInstance<ModelsInvitation>(
    {
      url: `/invitations/${invitationId}/register`,
      method: 'post',
      headers: { 'Content-Type': 'application/json' },
      data: modelsUserRegistration,
    },
    options
  );
};
