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
 * OpenAPI spec version: 1.0.3+0-fdc226b5+20230829-1502
 */
import useSwr from 'swr';
import type { SWRConfiguration, Key } from 'swr';
import type { ModelsApplicationConfig } from '..';
import { lasiusAxiosInstance } from '../../lasiusAxiosInstance';
import type { ErrorType } from '../../lasiusAxiosInstance';

// eslint-disable-next-line
  type SecondParameter<T extends (...args: any) => any> = T extends (
  config: any,
  args: infer P
) => any
  ? P
  : never;

/**
 * @summary Get application config
 */
export const getConfiguration = (options?: SecondParameter<typeof lasiusAxiosInstance>) => {
  return lasiusAxiosInstance<ModelsApplicationConfig>({ url: `/config`, method: 'get' }, options);
};

export const getGetConfigurationKey = () => [`/config`] as const;

export type GetConfigurationQueryResult = NonNullable<Awaited<ReturnType<typeof getConfiguration>>>;
export type GetConfigurationQueryError = ErrorType<unknown>;

/**
 * @summary Get application config
 */
export const useGetConfiguration = <TError = ErrorType<unknown>>(options?: {
  swr?: SWRConfiguration<Awaited<ReturnType<typeof getConfiguration>>, TError> & {
    swrKey?: Key;
    enabled?: boolean;
  };
  request?: SecondParameter<typeof lasiusAxiosInstance>;
}) => {
  const { swr: swrOptions, request: requestOptions } = options ?? {};

  const isEnabled = swrOptions?.enabled !== false;
  const swrKey = swrOptions?.swrKey ?? (() => (isEnabled ? getGetConfigurationKey() : null));
  const swrFn = () => getConfiguration(requestOptions);

  const query = useSwr<Awaited<ReturnType<typeof swrFn>>, TError>(swrKey, swrFn, swrOptions);

  return {
    swrKey,
    ...query,
  };
};

export const messagingSocket = (options?: SecondParameter<typeof lasiusAxiosInstance>) => {
  return lasiusAxiosInstance<void>({ url: `/messagingSocket`, method: 'get' }, options);
};

export const getMessagingSocketKey = () => [`/messagingSocket`] as const;

export type MessagingSocketQueryResult = NonNullable<Awaited<ReturnType<typeof messagingSocket>>>;
export type MessagingSocketQueryError = ErrorType<unknown>;

export const useMessagingSocket = <TError = ErrorType<unknown>>(options?: {
  swr?: SWRConfiguration<Awaited<ReturnType<typeof messagingSocket>>, TError> & {
    swrKey?: Key;
    enabled?: boolean;
  };
  request?: SecondParameter<typeof lasiusAxiosInstance>;
}) => {
  const { swr: swrOptions, request: requestOptions } = options ?? {};

  const isEnabled = swrOptions?.enabled !== false;
  const swrKey = swrOptions?.swrKey ?? (() => (isEnabled ? getMessagingSocketKey() : null));
  const swrFn = () => messagingSocket(requestOptions);

  const query = useSwr<Awaited<ReturnType<typeof swrFn>>, TError>(swrKey, swrFn, swrOptions);

  return {
    swrKey,
    ...query,
  };
};
