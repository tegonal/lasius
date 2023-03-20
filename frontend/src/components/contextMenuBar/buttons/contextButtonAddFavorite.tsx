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
import { ModelsBooking } from 'lib/api/lasius';
import { ContextButtonWrapper } from 'components/contextMenuBar/contextButtonWrapper';
import { ContextCompactButtonWrapper } from 'components/contextMenuBar/contextCompactButtonWrapper';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';

type Props = {
  item: ModelsBooking;
  variant?: 'default' | 'compact';
};
export const ContextButtonAddFavorite: React.FC<Props> = ({ item, variant }) => {
  const { actionAddBookingToFavorites } = useContextMenu();
  const { t } = useTranslation('common');
  const { selectedOrganisationId } = useOrganisation();
  const Wrapper = variant === 'compact' ? ContextCompactButtonWrapper : ContextButtonWrapper;
  return (
    <Wrapper>
      <Button
        variant="contextIcon"
        title={t('Add as favorite')}
        aria-label={t('Add as favorite')}
        onClick={() => actionAddBookingToFavorites(selectedOrganisationId, item)}
      >
        <Icon name="rating-star-add-social-medias-rewards-rating" size={24} />
      </Button>
    </Wrapper>
  );
};
