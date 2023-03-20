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
import { Strong } from 'components/tags/strong';
import { Box } from 'theme-ui';
import { ModelsBooking, ModelsBookingStub } from 'lib/api/lasius';

type Props = {
  item: ModelsBookingStub | ModelsBooking | undefined;
  variant?: 'compact';
};
export const BookingName: React.FC<Props> = ({ item, variant }) => {
  if (!item?.projectReference?.key) return null;
  return (
    <Box sx={{ lineHeight: 'normal', ...(variant === 'compact' ? { fontSize: 1 } : {}) }}>
      <Strong>{item.projectReference.key}</Strong>
    </Box>
  );
};
