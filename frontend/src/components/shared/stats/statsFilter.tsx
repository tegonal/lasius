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
import { Box, Heading } from 'theme-ui';
import { Button } from '@theme-ui/components';
import { useTranslation } from 'next-i18next';
import { FormElement } from 'components/forms/formElement';
import { FormBody } from 'components/forms/formBody';
import { DateRangeFilter } from 'components/shared/dateRangeFilter';
import { dateOptions } from 'lib/dateOptions';
import { useFormContext } from 'react-hook-form';

export const StatsFilter: React.FC = () => {
  const { t } = useTranslation('common');
  const parentFormContext = useFormContext();

  const resetForm = () => {
    const { from, to } = dateOptions[0].dateRangeFn(new Date());
    parentFormContext.setValue('from', from);
    parentFormContext.setValue('to', to);
    parentFormContext.setValue('dateRange', dateOptions[0].name);
  };

  return (
    <Box sx={{ width: '100%' }}>
      <Heading variant="headingUnderlinedMuted">{t('Filter')}</Heading>
      <FormBody>
        <DateRangeFilter name="dateRange" />
        <FormElement>
          <Button type="button" onClick={resetForm} variant="secondary">
            {t('Reset')}
          </Button>
        </FormElement>
      </FormBody>
    </Box>
  );
};
