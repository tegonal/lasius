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

import React, { useState } from 'react';
import { Box, Paragraph } from 'theme-ui';
import { Button } from '@theme-ui/components';
import { useTranslation } from 'next-i18next';
import { FormElement } from 'components/forms/formElement';
import { FormBody } from 'components/forms/formBody';
import { DateRangeFilter } from 'components/shared/dateRangeFilter';
import { ModelsEntityReference } from 'lib/api/lasius';
import { FormProvider, useForm } from 'react-hook-form';
import { ModelsTags } from 'types/common';
import { apiTimespanFromTo } from 'lib/api/apiDateHandling';
import { exportBookingListToCsv } from 'lib/csv';
import { getProjectBookingList } from 'lib/api/lasius/project-bookings/project-bookings';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { getExtendedModelsBookingList } from 'lib/api/functions/getExtendedModelsBookingList';

type Props = {
  item: ModelsEntityReference;
};

type FormValues = {
  project: string;
  tags: ModelsTags[];
  from: string;
  to: string;
  dateRange: string;
};

export const ProjectBookingsCsvExport: React.FC<Props> = ({ item }) => {
  const { t } = useTranslation('common');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const { selectedOrganisationId } = useOrganisation();

  const hookForm = useForm<FormValues>();

  const handleDownload = async () => {
    setIsLoading(true);
    const { from, to } = hookForm.getValues();
    const data = await getProjectBookingList(
      selectedOrganisationId,
      item.id,
      apiTimespanFromTo(from, to)
    );
    const extendedHistory = getExtendedModelsBookingList(data);
    exportBookingListToCsv(extendedHistory);
    setIsLoading(false);
  };

  return (
    <Box sx={{ label: 'AllProjectsExportCSV', width: '100%', maxWidth: 320 }}>
      <FormProvider {...hookForm}>
        <FormBody>
          <FormElement>
            <Paragraph>
              {t(
                'Exporting all bookings for {{project}}. This includes time booked by any user, even those not part of your organisation.',
                { project: item.key }
              )}
            </Paragraph>
          </FormElement>
          <FormElement>
            <DateRangeFilter name="dateRange" />
          </FormElement>
          <FormElement>
            <Button type="button" disabled={isLoading} onClick={handleDownload}>
              {t('Download CSV')}
            </Button>
          </FormElement>
        </FormBody>
      </FormProvider>
    </Box>
  );
};
