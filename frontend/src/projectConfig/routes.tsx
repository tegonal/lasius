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

import { IconNames } from 'types/iconNames';
import { NavigationTabContent } from 'components/navigation/desktop/navigationTabContent';
import React from 'react';
import { NavigationLogout } from 'components/navigation/desktop/navigationLogout';
import { ROLES } from 'projectConfig/constants';

const t = (s: string) => s;

export const ROUTES = {
  USER: {
    INDEX: '/user/home',
    THIS_MONTH: '/user/this-month',
    STATS: '/user/stats',
    LISTS: '/user/lists',
    WORKING_HOURS: '/user/working-hours',
  },
  PROJECTS: {
    MY: '/projects/my',
    ALL: '/projects/all',
  },
  ORGANISATION: {
    CURRENT: '/organisation/current',
    LISTS: '/organisation/lists',
    STATS: '/organisation/stats',
  },
  SETTINGS: {
    ACCOUNT: '/settings/account',
    ACCOUNT_SECURITY: '/settings/account-security',
  },
};

export type NavigationRouteType = {
  route: string;
  name: string;
  icon: IconNames;
  restrictTo?: string[];
};

export type NavigationType = {
  level: string;
  component: React.ReactNode;
  icon: IconNames;
  routes: NavigationRouteType[];
  name: string;
}[];

export const NAVIGATION: NavigationType = [
  {
    level: 'user',
    component: <NavigationTabContent branch="user" />,
    icon: 'single-neutral-circle-users',
    name: t('Your time booking view'),
    routes: [
      {
        route: ROUTES.USER.INDEX,
        name: t('Bookings'),
        icon: 'stopwatch-interface-essential',
      },
      {
        route: ROUTES.USER.WORKING_HOURS,
        name: t('Working hours'),
        icon: 'single-neutral-actions-time-users',
      },
      {
        route: ROUTES.USER.THIS_MONTH,
        name: t('This month'),
        icon: 'calendar-1-interface-essential',
      },
      {
        route: ROUTES.USER.STATS,
        name: t('Statistics'),
        icon: 'pie-line-graph-interface-essential',
      },
      {
        route: ROUTES.USER.LISTS,
        name: t('Lists'),
        icon: 'filter-text-interface-essential',
      },
    ],
  },
  {
    level: 'projects',
    component: <NavigationTabContent branch="projects" />,
    icon: 'folder-files-folders',
    name: t('View projects and manage them'),
    routes: [
      {
        route: ROUTES.PROJECTS.MY,
        name: t('My projects'),
        icon: 'folder-files-folders',
      },
      {
        route: ROUTES.PROJECTS.ALL,
        name: t('All projects'),
        icon: 'folder-settings-files-folders',
        restrictTo: [ROLES.ORGANISATION_ADMIN],
      },
    ],
  },
  {
    level: 'organisation',
    component: <NavigationTabContent branch="organisation" />,
    icon: 'human-resources-search-team-work-office-companies',
    name: t('View organisations and manage them'),
    routes: [
      {
        route: ROUTES.ORGANISATION.CURRENT,
        name: t('Current organisation'),
        icon: 'human-resources-search-team-work-office-companies',
      },
      {
        route: ROUTES.ORGANISATION.LISTS,
        name: t('Lists'),
        icon: 'filter-text-interface-essential',
        restrictTo: [ROLES.ORGANISATION_ADMIN],
      },
      {
        route: ROUTES.ORGANISATION.STATS,
        name: t('Statistics'),
        icon: 'pie-line-graph-interface-essential',
        restrictTo: [ROLES.ORGANISATION_ADMIN],
      },
    ],
  },
  {
    level: 'settings',
    component: <NavigationTabContent branch="settings" />,
    icon: 'cog-interface-essential',
    name: t('Change user profile settings'),
    routes: [
      {
        route: ROUTES.SETTINGS.ACCOUNT,
        name: t('Account'),
        icon: 'single-neutral-profile-picture-users',
      },
      {
        route: ROUTES.SETTINGS.ACCOUNT_SECURITY,
        name: t('Security'),
        icon: 'lock-1-interface-essential',
      },
    ],
  },
  {
    level: 'signout',
    component: <NavigationLogout />,
    icon: 'logout-interface-essential',
    routes: [],
    name: t('Logout from Lasius'),
  },
];

export const getNavigation = ({
  id,
  isOrganisationAdministrator,
}: {
  id: string;
  isOrganisationAdministrator: boolean;
}) => {
  const branch = NAVIGATION.filter((item) => item.level === id)[0].routes;
  return branch.filter((item) => {
    return (
      !item.restrictTo ||
      (item.restrictTo?.includes(ROLES.ORGANISATION_ADMIN) && isOrganisationAdministrator)
    );
  });
};
