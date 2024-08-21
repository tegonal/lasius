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

import React, { useEffect } from 'react';

import { Box, Flex, Heading } from 'theme-ui';
import { PageError } from 'dynamicTranslationStrings';
import { usePlausible } from 'next-plausible';
import { LasiusPlausibleEvents } from 'lib/telemetry/plausibleEvents';

export const Error: React.FC<{ statusCode: number }> = ({ statusCode }) => {
  const plausible = usePlausible<LasiusPlausibleEvents>();

  useEffect(() => {
    plausible('error', {
      props: {
        status: statusCode.toString(),
        message: PageError[statusCode.toString()],
      },
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <Flex sx={{ label: 'Error', height: '66vh', justifyContent: 'center', alignItems: 'center' }}>
      <Box
        p={3}
        sx={{
          borderRight: `1px solid`,
          borderRightColor: 'containerBackground',
        }}
      >
        <Heading as="h1" sx={{ p: 0, m: 0, border: 'none' }}>
          {statusCode}
        </Heading>
      </Box>
      <Box p={3}>
        <p>{PageError[statusCode.toString() || 'undefined']}</p>
      </Box>
    </Flex>
  );
};
