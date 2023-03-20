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

import { Badge, Box, useColorMode } from 'theme-ui';
import { useRouter } from 'next/router';
import React, { useEffect, useState } from 'react';
import { BUILD_ID, DEV } from 'projectConfig/constants';
import { useBreakpointIndex } from '@theme-ui/match-media';
import { useTranslation } from 'next-i18next';
import { useIsClient } from 'usehooks-ts';

export const DevInfoBadge: React.FC = () => {
  const { locale } = useRouter();
  const { t } = useTranslation('common');
  const [mode] = useColorMode();
  const [colorMode, setColorMode] = useState<string>('default');
  const breakpointIndex = useBreakpointIndex({ defaultIndex: 0 });
  const isClient = useIsClient();

  useEffect(() => {
    setColorMode(mode);
  }, [mode]);

  if (!isClient || !DEV) return null;

  const info = `(${breakpointIndex}) | ${locale} | ${BUILD_ID} | ${colorMode} | ${t('Lasius')}`;

  return (
    <Box sx={{ label: 'DevelopmentInfo', position: 'fixed', bottom: 2, left: 2 }}>
      <Box sx={{ display: ['block', 'none', 'none', 'none'] }}>
        <Badge>&lt; sm | {info}</Badge>
      </Box>
      <Box sx={{ display: ['none', 'block', 'none', 'none'] }}>
        <Badge>sm &gt; &lt; md | {info}</Badge>
      </Box>
      <Box sx={{ display: ['none', 'none', 'block', 'none'] }}>
        <Badge>md &gt; &lt; lg | {info}</Badge>
      </Box>
      <Box sx={{ display: ['none', 'none', 'none', 'block'] }}>
        <Badge>lg &gt; | {info}</Badge>
      </Box>
    </Box>
  );
};
