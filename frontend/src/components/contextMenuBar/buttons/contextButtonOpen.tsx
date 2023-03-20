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

import { useContextMenu } from 'components/contextMenuBar/hooks/useContextMenu';
import { Button } from '@theme-ui/components';
import { Icon } from 'components/shared/icon';
import { useTranslation } from 'next-i18next';
import React from 'react';
import { useEventListener } from 'usehooks-ts';

type Props = {
  hash: string;
};

//  Should not be wrapped in ContextButtonWrapper
export const ContextButtonOpen: React.FC<Props> = ({ hash }) => {
  const { handleOpenContextMenu, handleCloseContextMenu } = useContextMenu();
  const { t } = useTranslation('common');

  useEventListener('keydown', (event: KeyboardEvent) => {
    if (event.key === 'Escape') {
      handleCloseContextMenu(hash);
    }
  });

  return (
    <Button
      sx={{ label: 'ContextButtonOpen' }}
      variant="icon"
      title={t('Open context menu')}
      aria-label={t('Open context menu')}
      onClick={() => handleOpenContextMenu(hash)}
    >
      <Icon name="navigation-menu-vertical-interface-essential" size={24} />
    </Button>
  );
};
