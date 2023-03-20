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

import { Badge } from '@theme-ui/components';
import { useTranslation } from 'next-i18next';
import React from 'react';
import { Flex } from 'theme-ui';
import { flexRowJustifyStartAlignCenter } from 'styles/shortcuts';
import { ErrorSign } from 'components/shared/errorSign';
import { FormError } from 'dynamicTranslationStrings';
import { logger } from 'lib/logger';

type Props = { errors: { [x: string]: { types: { [x: string]: boolean } } } };

export const FormErrorsSummary: React.FC<Props> = ({ errors }) => {
  const { t } = useTranslation('common');
  logger.warn('[form][FormErrorsSummary]', errors);
  return (
    <>
      {Object.keys(errors).map((field) => (
        <Flex
          key={field}
          sx={{ ...flexRowJustifyStartAlignCenter(2), py: 2, flexWrap: 'wrap', maxWidth: '100%' }}
        >
          {Object.keys(errors[field].types).map((key) => (
            <Badge key={key} variant="warning">
              <ErrorSign />
              {/*
          // eslint-disable-next-line @typescript-eslint/ban-ts-comment
  // @ts-ignore */}
              {t(FormError[key])}
            </Badge>
          ))}
        </Flex>
      ))}
    </>
  );
};
