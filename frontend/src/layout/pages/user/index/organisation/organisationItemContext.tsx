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
import { AnimatePresence } from 'framer-motion';
import { ContextCompactBody } from 'components/contextMenuBar/contextCompactBody';
import { ContextCompactBar } from 'components/contextMenuBar/contextCompactBar';
import { ContextCompactAnimatePresence } from 'components/contextMenuBar/contextCompactAnimatePresence';
import { ModelsCurrentUserTimeBooking } from 'lib/api/lasius';
import { ContextButtonOpen } from 'components/contextMenuBar/buttons/contextButtonOpen';
import { ContextButtonClose } from 'components/contextMenuBar/buttons/contextButtonClose';
import { ContextButtonStartBooking } from 'components/contextMenuBar/buttons/contextButtonStartBooking';
import { useContextMenu } from 'components/contextMenuBar/hooks/useContextMenu';
import { stringHash } from 'lib/stringHash';

type Props = {
  item: ModelsCurrentUserTimeBooking;
};

export const OrganisationItemContext: React.FC<Props> = ({ item }) => {
  const itemHash = stringHash(item);
  const { currentOpenContextMenuId } = useContextMenu();

  return (
    <>
      <ContextCompactBody>
        <ContextButtonOpen hash={itemHash} />
        <AnimatePresence>
          {currentOpenContextMenuId === itemHash && (
            <ContextCompactAnimatePresence>
              <ContextCompactBar>
                <ContextButtonStartBooking variant="compact" item={item} />
                <ContextButtonClose variant="compact" />
              </ContextCompactBar>
            </ContextCompactAnimatePresence>
          )}
        </AnimatePresence>
      </ContextCompactBody>
    </>
  );
};
