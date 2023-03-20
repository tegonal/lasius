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
import { Grid, Heading } from 'theme-ui';
import { useTranslation } from 'next-i18next';
import { FormElement } from 'components/forms/formElement';
import { Button } from '@theme-ui/components';
import { exportBookingListToCsv } from 'lib/csv';
import { ExtendedHistoryBooking } from 'types/booking';

type Props = {
  bookings: ExtendedHistoryBooking[];
};
export const BookingHistoryExport: React.FC<Props> = ({ bookings }) => {
  const { t } = useTranslation('common');

  const exportCurrentList = () => {
    exportBookingListToCsv(bookings);
  };

  return (
    <FormElement>
      <Heading variant="headingUnderlinedMuted">{t('Export data')}</Heading>
      <Grid sx={{ gap: 2, gridTemplateColumns: '1fr 1fr' }}>
        <Button
          type="button"
          disabled={bookings.length < 1}
          onClick={() => exportCurrentList()}
          variant="secondarySmall"
        >
          {t('Export')}
        </Button>
      </Grid>
    </FormElement>
  );
};
