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

// custom-instance.ts
import Axios from 'axios';
import axios, { AxiosError, AxiosRequestConfig } from 'axios';
import { IS_BROWSER } from 'projectConfig/constants';
import { logger } from 'lib/logger';
import { signOut } from 'next-auth/react';
import clientAxiosInstance from 'lib/api/ClientAxiosInstance';
import { removeAccessibleCookies } from 'lib/removeAccessibleCookies';

// add a second `options` argument here if you want to pass extra options to each generated query
export const lasiusAxiosInstance = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig
): Promise<T> => {
  const defaultHeaders = axios.defaults.headers.common;
  const newConfig = {
    ...config,
    headers: { ...defaultHeaders, ...config.headers },
  };
  logger.log(newConfig, options);

  const source = Axios.CancelToken.source();
  const promise = clientAxiosInstance({
    ...newConfig,
    ...options,
    cancelToken: source.token,
  })
    .then(({ data }) => data)
    .catch(async (error) => {
      logger.log('error', error);
      if (Axios.isCancel(error)) {
        logger.log('[lasiusAxiosInstance][RequestCanceled]', error.message);
      } else if (error.response.status === 401) {
        logger.log('[lasiusAxiosInstance][Unauthorized]', {
          path: error.request.pathname,
          message: error.data,
        });
        if (
          IS_BROWSER &&
          window.location.pathname !== '/auth/signin' &&
          window.location.pathname !== '/login' &&
          window.location.pathname !== '/'
        ) {
          await removeAccessibleCookies();
          await signOut();
        } else {
          logger.log('[lasiusAxiosInstance][Unauthorized]', error);
          throw new Error(error);
        }
      } else {
        throw error;
      }
    });

  // eslint-disable-next-line @typescript-eslint/ban-ts-comment
  // @ts-ignore
  promise.cancel = () => {
    source.cancel('Query was cancelled');
  };

  return promise;
};

// In some case with react-query and swr you want to be able to override the return error type so you can also do it here like this
export type ErrorType<Error> = AxiosError<Error>;
// // In case you want to wrap the body type (optional)
// // (if the custom instance is processing data before sending it, like changing the case for example)
export type BodyType<BodyData> = BodyData;
