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
import React from 'react';
import { useTranslation } from 'next-i18next';
import { ContextButtonWrapper } from 'components/contextMenuBar/contextButtonWrapper';
import { ContextCompactButtonWrapper } from 'components/contextMenuBar/contextCompactButtonWrapper';

type Props = {
  variant?: 'default' | 'compact';
};
export const ContextButtonClose: React.FC<Props> = ({ variant = 'default' }) => {
  const { handleCloseAll } = useContextMenu();
  const { t } = useTranslation('common');
  const Wrapper = variant === 'compact' ? ContextCompactButtonWrapper : ContextButtonWrapper;
  return (
    <Wrapper>
      <Button
        sx={{ label: 'contextButtonClose' }}
        variant="contextIcon"
        title={t('Close context menu')}
        aria-label={t('Close context menu')}
        onClick={handleCloseAll}
      >
        <Icon name="remove-circle-interface-essential" size={24} />
      </Button>
    </Wrapper>
  );
};
