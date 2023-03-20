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

import React, { useRef } from 'react';
import { Flex } from 'theme-ui';
import { themeRadii } from 'styles/theme/radii';
import { effects } from 'styles/theme/effects';
import { useOnClickOutside } from 'usehooks-ts';
import { useContextMenu } from 'components/contextMenuBar/hooks/useContextMenu';

type Props = {
  children: React.ReactNode;
};

export const ContextCompactBar: React.FC<Props> = ({ children }) => {
  const ref = useRef(null);
  const { handleCloseAll } = useContextMenu();

  const handleClickOutside = () => {
    handleCloseAll();
  };

  useOnClickOutside(ref, handleClickOutside);

  return (
    <Flex
      ref={ref}
      sx={{
        label: 'ContextCompactBar',
        pl: 2,
        color: 'negativeText',
        background: 'greenGradient',
        borderBottomLeftRadius: themeRadii.large,
        borderTopLeftRadius: themeRadii.large,
        ...effects.shadows.softShadowOnDark,
      }}
    >
      {children}
    </Flex>
  );
};
