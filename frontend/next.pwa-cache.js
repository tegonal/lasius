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

// Workbox RuntimeCaching config: https://developers.google.com/web/tools/workbox/reference-docs/latest/module-workbox-build#.RuntimeCachingEntry
module.exports = [
  {
    urlPattern: /\.(?:eot|otf|ttc|ttf|woff|woff2|font.css)$/i,
    handler: 'StaleWhileRevalidate',
    options: {
      cacheName: 'static-font-assets',
      expiration: {
        maxEntries: 4,
        maxAgeSeconds: 7 * 24 * 60 * 60, // 7 days
      },
    },
  },
  {
    urlPattern: /\.(?:jpg|jpeg|gif|png|svg|ico|webp)$/i,
    handler: 'StaleWhileRevalidate',
    options: {
      cacheName: 'static-image-assets',
      expiration: {
        maxEntries: 64,
        maxAgeSeconds: 24 * 60 * 60, // 24 hours
      },
    },
  },
  {
    urlPattern: /\/_next\/image\?url=.+$/i,
    handler: 'StaleWhileRevalidate',
    options: {
      cacheName: 'next-image',
      expiration: {
        maxEntries: 64,
        maxAgeSeconds: 24 * 60 * 60, // 24 hours
      },
    },
  },
  {
    urlPattern: /\.(?:js)$/i,
    handler: 'StaleWhileRevalidate',
    options: {
      cacheName: 'static-js-assets',
      expiration: {
        maxEntries: 32,
        maxAgeSeconds: 24 * 60 * 60, // 24 hours
      },
    },
  },
  {
    urlPattern: /\.(?:css|less)$/i,
    handler: 'StaleWhileRevalidate',
    options: {
      cacheName: 'static-style-assets',
      expiration: {
        maxEntries: 32,
        maxAgeSeconds: 24 * 60 * 60, // 24 hours
      },
    },
  },
  {
    urlPattern: /\/_next\/data\/.+\/.+\.json$/i,
    handler: 'StaleWhileRevalidate',
    options: {
      cacheName: 'next-data',
      expiration: {
        maxEntries: 32,
        maxAgeSeconds: 24 * 60 * 60, // 24 hours
      },
    },
  },
  {
    urlPattern: /\.(?:json|xml|csv)$/i,
    handler: 'NetworkFirst',
    options: {
      cacheName: 'static-data-assets',
      expiration: {
        maxEntries: 32,
        maxAgeSeconds: 24 * 60 * 60, // 24 hours
      },
    },
  },
  {
    urlPattern: ({ url }) => {
      const isSameOrigin = self.origin === url.origin;
      if (!isSameOrigin) return false;
      const { pathname } = url;
      // never cache API calls. we use useSWR for that.
      if (pathname.startsWith('/backend/')) return true;
      if (pathname.startsWith('/api/auth/')) return true;
      if (pathname.startsWith('/api/ping/event')) return true;
      if (pathname.startsWith('/api/')) return true;
      return false;
    },
    handler: 'NetworkOnly',
    method: 'GET',
    options: {
      cacheName: 'onlineApis',
      networkTimeoutSeconds: 10, // fall back to cache if api does not response within 10 seconds
    },
  },
  // {
  //   urlPattern: ({ url }) => {
  //     const isSameOrigin = self.origin === url.origin;
  //     if (!isSameOrigin) return false;
  //     const { pathname } = url;
  //     // Exclude /api/auth/callback/* to fix OAuth workflow in Safari without impact other environment
  //     // Above route is default for next-auth, you may need to change it if your OAuth workflow has a different callback route
  //     // Issue: https://github.com/shadowwalker/next-pwa/issues/131#issuecomment-821894809
  //     if (pathname.startsWith('/backend/')) return false;
  //     if (pathname.startsWith('/api/auth/')) return false;
  //     if (pathname.startsWith('/api/')) return true;
  //     return false;
  //   },
  //   handler: 'NetworkFirst',
  //   method: 'GET',
  //   options: {
  //     cacheName: 'apis',
  //     expiration: {
  //       maxEntries: 16,
  //       maxAgeSeconds: 24 * 60 * 60, // 24 hours
  //     },
  //     networkTimeoutSeconds: 10, // fall back to cache if api does not response within 10 seconds
  //   },
  // },
  {
    urlPattern: ({ url }) => {
      // eslint-disable-next-line no-restricted-globals
      const isSameOrigin = self.origin === url.origin;
      if (!isSameOrigin) return false;
      const { pathname } = url;
      if (
        pathname.startsWith('/backend/') ||
        pathname.startsWith('/api/') ||
        pathname.startsWith('/s/api/')
      )
        return false;
      return true;
    },
    handler: 'NetworkFirst',
    options: {
      cacheName: 'others',
      expiration: {
        maxEntries: 32,
        maxAgeSeconds: 24 * 60 * 60, // 24 hours
      },
      networkTimeoutSeconds: 10,
    },
  },
  {
    urlPattern: ({ url }) => {
      const isSameOrigin = self.origin === url.origin;
      return !isSameOrigin;
    },
    handler: 'NetworkFirst',
    options: {
      cacheName: 'cross-origin',
      expiration: {
        maxEntries: 32,
        maxAgeSeconds: 60 * 60, // 1 hour
      },
      networkTimeoutSeconds: 10,
    },
  },
];
