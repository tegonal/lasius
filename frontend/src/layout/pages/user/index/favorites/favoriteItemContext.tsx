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
import { Button } from 'theme-ui';
import { Icon } from 'components/shared/icon';
import { AnimatePresence } from 'framer-motion';
import { useTranslation } from 'next-i18next';
import { ContextCompactBody } from 'components/contextMenuBar/contextCompactBody';
import { ContextCompactBar } from 'components/contextMenuBar/contextCompactBar';
import { ContextCompactButtonWrapper } from 'components/contextMenuBar/contextCompactButtonWrapper';
import { ContextCompactAnimatePresence } from 'components/contextMenuBar/contextCompactAnimatePresence';
import {
  deleteFavoriteBooking,
  getGetFavoriteBookingListKey,
} from 'lib/api/lasius/user-favorites/user-favorites';
import { useSWRConfig } from 'swr';
import { ContextButtonOpen } from 'components/contextMenuBar/buttons/contextButtonOpen';
import { ContextButtonClose } from 'components/contextMenuBar/buttons/contextButtonClose';
import { ContextButtonStartBooking } from 'components/contextMenuBar/buttons/contextButtonStartBooking';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { useContextMenu } from 'components/contextMenuBar/hooks/useContextMenu';
import { ModelsBookingStub } from 'lib/api/lasius';
import { stringHash } from 'lib/stringHash';

type Props = {
  item: ModelsBookingStub;
};

export const FavoriteItemContext: React.FC<Props> = ({ item }) => {
  const { t } = useTranslation('common');
  const { mutate } = useSWRConfig();

  const itemHash = stringHash(item);
  const { selectedOrganisationId } = useOrganisation();
  const { currentOpenContextMenuId, handleCloseAll } = useContextMenu();

  const deleteFavorite = async () => {
    const {
      projectReference: { id: projectId },
      tags,
    } = item;
    await deleteFavoriteBooking(selectedOrganisationId, { projectId, tags });
    await mutate(getGetFavoriteBookingListKey(selectedOrganisationId));
    handleCloseAll();
  };

  return (
    <>
      <ContextCompactBody>
        <ContextButtonOpen hash={itemHash} />
        <AnimatePresence>
          {currentOpenContextMenuId === itemHash && (
            <ContextCompactAnimatePresence>
              <ContextCompactBar>
                <ContextButtonStartBooking variant="compact" item={item} />
                <ContextCompactButtonWrapper>
                  <Button
                    variant="contextIcon"
                    title={t('Delete favorite')}
                    aria-label={t('Delete favorite')}
                    onClick={() => deleteFavorite()}
                  >
                    <Icon name="bin-2-alternate-interface-essential" size={24} />
                  </Button>
                </ContextCompactButtonWrapper>
                <ContextButtonClose variant="compact" />
              </ContextCompactBar>
            </ContextCompactAnimatePresence>
          )}
        </AnimatePresence>
      </ContextCompactBody>
    </>
  );
};
