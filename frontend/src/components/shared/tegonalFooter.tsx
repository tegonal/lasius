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

import { Box, Flex } from 'theme-ui';
import { flexColumnJustifyCenterAlignCenter } from 'styles/shortcuts';
import { Link } from '@theme-ui/components';
import { Icon } from 'components/shared/icon';
import { Trans, useTranslation } from 'next-i18next';
import { LasiusBackendStatus } from 'components/system/lasiusBackendStatus';
import React from 'react';

export const TegonalFooter: React.FC = () => {
  const { t } = useTranslation('common');
  return (
    <Flex
      sx={{
        ...flexColumnJustifyCenterAlignCenter(2),
        color: 'gray',
        'a:hover': { color: 'white' },
      }}
    >
      <Box>
        <Link href="https://tegonal.com">
          <Icon name="tegonal-icon" size={24} />
        </Link>
      </Box>
      <Box sx={{ fontSize: 1 }}>
        <Trans
          t={t}
          i18nKey="Developed by <0>Tegonal</0>, released under <1>AGPL 3.0</1>"
          components={[
            <Link key="tegonalLink" target="_blank" href="https://tegonal.com" />,
            <Link
              key="agplLink"
              target="_blank"
              href="https://www.gnu.org/licenses/agpl-3.0.en.html"
            />,
          ]}
        />
      </Box>
      <LasiusBackendStatus />
    </Flex>
  );
};
