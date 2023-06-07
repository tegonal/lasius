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

import getConfig from 'next/config';

export const IS_BROWSER: boolean = typeof window !== 'undefined';
export const IS_SERVER: boolean = typeof window === 'undefined';
export const SOCIAL_MEDIA_CARD_IMAGE_URL = '/social-card.png';

const { publicRuntimeConfig } = getConfig();

export const {
  BUILD_ID,
  ENVIRONMENT,
  LASIUS_API_WEBSOCKET_URL,
  LASIUS_API_URL,
  LASIUS_API_URL_INTERNAL,
  LASIUS_TELEMETRY_PLAUSIBLE_HOST,
  LASIUS_TELEMETRY_PLAUSIBLE_SOURCE_DOMAIN,
  LASIUS_DEMO_MODE,
} = publicRuntimeConfig as { [key: string]: string };

export const DEV = ENVIRONMENT !== 'production';

export const DEFAULT_STRING_VALUE = 'default';
export const DEFAULT_STRING_VALUE_ALL = 'all';

export const TIME = {
  MINUTE: 'minute',
  HOUR: 'hour',
  DAY: 'day',
  MONTH: 'month',
  YEAR: 'year',
};

export const NO_DATA_AVAILABLE = 'nodata';
export type NO_DATA_AVAILABLE = typeof NO_DATA_AVAILABLE;

export const DATA_LOADING = 'dataloading';
export type DATA_LOADING = typeof DATA_LOADING;

export const ROLES = {
  USER: 'FreeUser',
  ORGANISATION_ADMIN: 'OrganisationAdministrator',
  ORGANISATION_MEMBER: 'OrganisationMember',
  PROJECT_MEMBER: 'ProjectMember',
  PROJECT_ADMIN: 'ProjectAdministrator',
};
export type ROLES = typeof ROLES;

export const COOKIE_NAMES = {
  XSRF_TOKEN: 'XSRF-TOKEN',
};
export type COOKIE_NAMES = typeof COOKIE_NAMES;

export const WEBSOCKET_EVENT = {
  CurrentOrganisationTimeBookings: 'CurrentOrganisationTimeBookings',
  CurrentUserTimeBookingEvent: 'CurrentUserTimeBookingEvent',
  FavoriteAdded: 'FavoriteAdded',
  FavoriteRemoved: 'FavoriteRemoved',
  HelloClient: 'HelloClient',
  LatestTimeBooking: 'LatestTimeBooking',
  UserTimeBookingByProjectEntryAdded: 'UserTimeBookingByProjectEntryAdded',
  UserTimeBookingByProjectEntryRemoved: 'UserTimeBookingByProjectEntryRemoved',
  UserTimeBookingHistoryEntryAdded: 'UserTimeBookingHistoryEntryAdded',
  UserTimeBookingHistoryEntryChanged: 'UserTimeBookingHistoryEntryChanged',
  UserTimeBookingHistoryEntryRemoved: 'UserTimeBookingHistoryEntryRemoved',
  UserTimeBookingByTagEntryAdded: 'UserTimeBookingByTagEntryAdded',
  UserTimeBookingByTagEntryRemoved: 'UserTimeBookingByTagEntryRemoved',
};

export enum CONNECTION_STATUS {
  UNKNOWN = 'UNKNOWN',
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  NOT_AUTHENTICATED = 'NOT_AUTHENTICATED',
  DISCONNECTED = 'DISCONNECTED',
  ERROR = 'ERROR',
}
