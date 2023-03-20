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

import React, { useContext } from 'react';
import { Grid, Input } from 'theme-ui';
import { ButtonUpDown } from '../shared/buttonUpDown';
import { InputDatepickerContext } from 'components/forms/input/datePicker/store/store';
import {
  datePickerDigitInputStyle,
  datePickerYearInputStyle,
} from 'components/forms/input/datePicker/sharedStyle';
import { Box } from '@theme-ui/components';
import { useDateFieldInput } from 'components/forms/input/datePicker/useDateFieldInput';

export const DatePickerFieldYears: React.FC = () => {
  const { state, dispatch } = useContext(InputDatepickerContext);

  const { handleClickDown, handleClickUp, inputProps } = useDateFieldInput({
    incrementerValue: 1,
    setter: 'setYear',
    dispatch,
    defaultValue: state.date.getFullYear(),
    digits: 4,
  });

  return (
    <Grid
      sx={{
        label: 'DatePickerFieldYears',
        ...datePickerYearInputStyle,
      }}
    >
      <ButtonUpDown direction="up" onClick={handleClickUp} />
      <Box>
        <Input {...inputProps} aria-label="Years" sx={{ ...datePickerDigitInputStyle }} />
      </Box>
      <ButtonUpDown direction="down" onClick={handleClickDown} />
    </Grid>
  );
};
