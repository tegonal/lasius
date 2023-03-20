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

import 'react-i18next';
import en from '../public/locales/en/common.json';

declare module '*.svg' {
  const content: any;
  export default content;
}

declare module 'console' {
  export = typeof import('console');
}

declare module 'i18next' {
  interface CustomTypeOptions {
    // custom namespace type if you changed it
    defaultNS: 'common';
    // custom resources type
    resources: {
      common: typeof en;
    };
  }
}

declare global {
  interface Window {
    workbox: {
      messageSkipWaiting(): void;
      register(): void;
      addEventListener(name: string, callback: () => unknown);
    };
  }
}
