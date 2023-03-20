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

import React from 'react';
import { Box, Flex } from 'theme-ui';
import { useTranslation } from 'next-i18next';
import { flexColumnJustifyCenterAlignCenter, fullWidthHeight } from 'styles/shortcuts';
import { Icon } from 'components/shared/icon';

export const DataFetchUnknownError: React.FC = () => {
  const { t } = useTranslation('common');
  return (
    <Flex
      sx={{
        label: 'DataFetchUnknownError',
        ...fullWidthHeight(),
        ...flexColumnJustifyCenterAlignCenter(0),
      }}
    >
      <Flex
        sx={{
          ...flexColumnJustifyCenterAlignCenter(2),
          color: 'containerTextColorMuted',
          fontSize: 1,
        }}
      >
        <Icon name="server-error-desktop-programing-apps-websites" size={24} />
        <Box>{t('So sorry, but the data could not be retrieved due to an error.')}</Box>
      </Flex>
    </Flex>
  );
};
