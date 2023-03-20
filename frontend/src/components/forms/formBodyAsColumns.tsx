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
import { Flex } from 'theme-ui';
import { flexRowJustifyStartAlignStart } from 'styles/shortcuts';

type Props = {
  children: React.ReactNode;
};

/**
 * For two columns, with last column sized down for save buttons etc.
 * @param children
 * @constructor
 */
export const FormBodyAsColumns: React.FC<Props> = ({ children }) => {
  return (
    <Flex
      sx={{
        label: 'FormBodyAsColumns',
        ...flexRowJustifyStartAlignStart(4),
        width: '100%',
        '>div:not(:last-child)': {
          width: 'auto',
          flexGrow: 1,
        },
        '>div:last-child': {
          width: '28%',
          flexShrink: 0,
          pt: 0,
        },
      }}
    >
      {children}
    </Flex>
  );
};
