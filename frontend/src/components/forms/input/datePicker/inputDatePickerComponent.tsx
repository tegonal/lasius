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
import { flexRowJustifyStartAlignCenter } from 'styles/shortcuts';
import { Box, Flex } from 'theme-ui';

import { useFormContext } from 'react-hook-form';
import { FormErrorBadge } from 'components/forms/formErrorBadge';
import { DatePickerFieldHours } from 'components/forms/input/datePicker/datePickerFieldHours';
import { DatePickerFieldMinutes } from 'components/forms/input/datePicker/datePickerFieldMinutes';
import { DatePickerFieldSeparator } from 'components/forms/input/datePicker/datePickerFieldSeparator';
import { DatePickerCalendar } from 'components/forms/input/datePicker/datePickerCalendar';
import { IsoDateString } from 'lib/api/apiDateHandling';
import { Button } from '@theme-ui/components';
import { ToolTip } from 'components/shared/toolTip';
import { Icon } from 'components/shared/icon';
import { IconNames } from 'types/iconNames';

export type InputDatePickerComponentProps = {
  name: string;
  rules?: { validate: Record<string, (value: string) => boolean> };
  label?: string;
  withDate?: boolean;
  withTime?: boolean;
  presetDate?: IsoDateString;
  presetLabel?: string;
  presetIcon?: IconNames;
};

export const InputDatePickerComponent: React.FC<InputDatePickerComponentProps> = ({
  name,
  label,
  withDate = true,
  withTime = true,
  presetDate,
  presetLabel,
  presetIcon = 'alert-triangle',
}) => {
  const parentFormContext = useFormContext();
  if (!parentFormContext) return null;

  const hasPreset = presetDate && presetLabel;

  const onPresetClick = () => {
    parentFormContext.setValue(name, presetDate);
  };

  return (
    <>
      <Flex
        sx={{
          label: 'InputDatePicker',
          flexDirection: ['column', 'row'],
          alignItems: ['flex-start', 'center'],
          justifyContent: 'space-between',
          gap: [1, 2],
          width: '100%',
          userSelect: 'none',
          pt: [1, 0],
        }}
      >
        {(label || hasPreset) && (
          <Box sx={{ flexShrink: 0 }}>
            {label && <Box>{label}</Box>}
            {hasPreset && (
              <ToolTip toolTipContent={presetLabel}>
                <Button variant="secondarySmall" type="button" onClick={onPresetClick}>
                  <Icon name={presetIcon} size={16} />
                </Button>
              </ToolTip>
            )}
          </Box>
        )}
        <Flex
          sx={{
            width: ['100%', 'fit-content'],
            justifyContent: ['center', 'flex-end'],
            position: 'relative',
            backgroundColor: 'containerBackgroundLighter',
            color: 'containerTextColor',
            borderRadius: 4,
            px: 3,
            py: '4px',
            gap: 4,
          }}
        >
          {withDate && <DatePickerCalendar />}
          {withTime && (
            <Flex sx={{ ...flexRowJustifyStartAlignCenter(0) }}>
              <DatePickerFieldHours />
              <DatePickerFieldSeparator separator=":" />
              <DatePickerFieldMinutes />
            </Flex>
          )}
        </Flex>
      </Flex>
      <FormErrorBadge error={parentFormContext.formState.errors[name]} />
    </>
  );
};
