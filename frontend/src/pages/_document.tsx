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

import { InitializeColorMode } from 'theme-ui';
import NextDocument, { DocumentContext, Head, Html, Main, NextScript } from 'next/document';
import React from 'react';

import { googleFontLoaderString } from 'styles/themeConstants';
import { BUILD_ID, ENVIRONMENT } from 'projectConfig/constants';

class MyDocument extends NextDocument {
  static async getInitialProps(ctx: DocumentContext) {
    return NextDocument.getInitialProps(ctx);
  }

  render() {
    return (
      <Html>
        <Head>
          <meta httpEquiv="Content-Type" content="text/html; charset=utf-8" />
          <link rel="manifest" href="/manifest.json" />
          <meta name="application-name" content="Lasius Timetracking" />
          <meta name="apple-mobile-web-app-capable" content="yes" />
          <meta name="apple-mobile-web-app-status-bar-style" content="default" />
          <meta name="apple-mobile-web-app-title" content="Lasius Timetracking" />
          <meta name="description" content="Lasius Timetracking by https://tegonal.com" />
          <link rel="icon" type="image/png" href="/icon-16x16.png" sizes="16x16" />
          <link rel="icon" type="image/png" href="/icon-32x32.png" sizes="32x32" />
          <link rel="icon" type="image/png" href="/icon-32x32.png" sizes="96x96" />
          <link rel="apple-touch-icon" href="/icon-310x310.png" />
          <link rel="apple-touch-icon" sizes="180x180" href="/icon-310x310.png" />
          <link rel="apple-touch-icon" sizes="152x152" href="/icon-310x310.png" />
          <link rel="apple-touch-icon" sizes="167x167" href="/icon-310x310.png" />
          <meta httpEquiv="X-UA-Compatible" content="IE=Edge,chrome=1" />
          <meta name="build-id" content={`${BUILD_ID}`} />
          <meta name="environment" content={`${ENVIRONMENT}`} />
          <link href={googleFontLoaderString} rel="stylesheet" />
        </Head>
        <body id="body">
          <svg style={{ display: 'none' }}>
            <use xlinkHref="/symbols.svg#default" />
          </svg>
          <noscript>
            <p>Please enable JavaScript in your browser settings and reload this page.</p>
          </noscript>
          <InitializeColorMode />
          <Main />
          <div id="modal" />
          <NextScript />
        </body>
      </Html>
    );
  }
}

export default MyDocument;
