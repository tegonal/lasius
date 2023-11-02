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
 * OpenAPI spec version: 1.0.4+7-a1eb9022+20231108-2147
 */
import useSwr from 'swr';
import type { SWRConfiguration, Key } from 'swr';
import type { ModelsInvitation, ModelsAcceptInvitationRequest } from '..';
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
 * @summary get detail of an invitation
 */
export const getInvitation = (
  invitationId: string,
  options?: SecondParameter<typeof lasiusAxiosInstance>
) => {
  return lasiusAxiosInstance<ModelsInvitation>(
    { url: `/invitations/${invitationId}`, method: 'get' },
    options
  );
};

export const getGetInvitationKey = (invitationId: string) =>
  [`/invitations/${invitationId}`] as const;

export type GetInvitationQueryResult = NonNullable<Awaited<ReturnType<typeof getInvitation>>>;
export type GetInvitationQueryError = ErrorType<void>;

/**
 * @summary get detail of an invitation
 */
export const useGetInvitation = <TError = ErrorType<void>>(
  invitationId: string,
  options?: {
    swr?: SWRConfiguration<Awaited<ReturnType<typeof getInvitation>>, TError> & {
      swrKey?: Key;
      enabled?: boolean;
    };
    request?: SecondParameter<typeof lasiusAxiosInstance>;
  }
) => {
  const { swr: swrOptions, request: requestOptions } = options ?? {};

  const isEnabled = swrOptions?.enabled !== false && !!invitationId;
  const swrKey =
    swrOptions?.swrKey ?? (() => (isEnabled ? getGetInvitationKey(invitationId) : null));
  const swrFn = () => getInvitation(invitationId, requestOptions);

  const query = useSwr<Awaited<ReturnType<typeof swrFn>>, TError>(swrKey, swrFn, swrOptions);

  return {
    swrKey,
    ...query,
  };
};

export const acceptInvitation = (
  invitationId: string,
  modelsAcceptInvitationRequest: BodyType<ModelsAcceptInvitationRequest>,
  options?: SecondParameter<typeof lasiusAxiosInstance>
) => {
  return lasiusAxiosInstance<ModelsInvitation>(
    {
      url: `/invitations/${invitationId}/accept`,
      method: 'post',
      headers: { 'Content-Type': 'application/json' },
      data: modelsAcceptInvitationRequest,
    },
    options
  );
};

export const declineInvitation = (
  invitationId: string,
  options?: SecondParameter<typeof lasiusAxiosInstance>
) => {
  return lasiusAxiosInstance<ModelsInvitation>(
    { url: `/invitations/${invitationId}/decline`, method: 'post' },
    options
  );
};
