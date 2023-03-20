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
import { useColorMode } from 'theme-ui';
import { Button } from '@theme-ui/components';
import { Icon } from 'components/shared/icon';
import { themeColorModes } from 'styles/theme/colors';

export const ColorModeToggle: React.FC = () => {
  const [mode, setMode] = useColorMode();

  const toggleMode = () => {
    const idx = themeColorModes.indexOf(mode);
    const newMode =
      idx + 1 < themeColorModes.length ? themeColorModes[idx + 1] : themeColorModes[0];
    setMode(newMode);
  };

  return (
    <Button
      variant="iconMuted"
      sx={{ label: 'ColorModeToggle', width: 'auto' }}
      onClick={toggleMode}
    >
      <Icon name="photo-adjust-brightness-images-photography" size={24} />
    </Button>
  );
};
