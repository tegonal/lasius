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

import React, { useId } from 'react';
import { Flex } from 'theme-ui';
import { flexColumnJustifyCenterAlignCenter, fullWidthHeight } from 'styles/shortcuts';
import { AnimatePresence, m } from 'framer-motion';
import { Loading } from 'components/shared/fetchState/loading';
import { DataFetchEmpty } from 'components/shared/fetchState/dataFetchEmpty';

type Props = {
  noData: boolean;
};

export const PlaceholderLoader: React.FC<Props> = ({ noData }) => {
  const id = useId();
  return (
    <Flex
      sx={{
        label: 'PlaceholderLoader',
        ...fullWidthHeight(),
        ...flexColumnJustifyCenterAlignCenter(0),
      }}
    >
      <AnimatePresence initial={false} mode="wait">
        <m.div
          key={id}
          animate={{ opacity: 1, z: 0 }}
          initial={{ opacity: 0, z: 20 }}
          exit={{ opacity: 0, z: -20 }}
          transition={{ duration: 0.1 }}
        >
          {noData ? <DataFetchEmpty /> : <Loading />}
        </m.div>
      </AnimatePresence>
    </Flex>
  );
};
