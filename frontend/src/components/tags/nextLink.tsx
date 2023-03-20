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

/* eslint-disable jsx-a11y/anchor-is-valid */
import NextLink from 'next/link';
import { Box, Link } from '@theme-ui/components';
import React, { memo } from 'react';

interface AnchorType {
  href: string;
  locale: string;
  target?: string;
}

/** Generates NextJS box link (<div></div>) */
export const NextLinkBox: React.FC<React.PropsWithChildren<AnchorType>> = memo(
  ({ children, href, locale }) => {
    if (!href) return <>{children}</>;
    return (
      <NextLink href={href} locale={locale} passHref={false}>
        <Box
          sx={{
            '&:hover, &:active, &:focus': {
              cursor: 'pointer',
            },
          }}
        >
          {children}
        </Box>
      </NextLink>
    );
  }
);

/** Generates NextJS text link (<a></a> with href) */
export const NextLinkText: React.FC<React.PropsWithChildren<AnchorType>> = memo(
  ({ children, href, locale, target }) => {
    if (!href) return <>{children}</>;
    return (
      <NextLink href={href} locale={locale} passHref legacyBehavior>
        <Link href="" target={target}>
          {children}
        </Link>
      </NextLink>
    );
  }
);
