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
import { layoutColumnStyle } from 'styles/shortcuts';
import { themeRadii } from 'styles/theme/radii';
import { AllProjectsRightColumn } from 'layout/pages/organisation/projects/allProjectsRightColumn';
import { AllProjectsList } from 'layout/pages/organisation/projects/allProjectsList';
import { ScrollContainer } from 'components/scrollContainer';

export const AllProjectsLayout: React.FC = () => {
  return (
    <>
      <ScrollContainer
        sx={{
          ...layoutColumnStyle,
          pt: 4,
        }}
      >
        <AllProjectsList />
      </ScrollContainer>
      <ScrollContainer
        sx={{
          ...layoutColumnStyle,
          background: 'containerBackgroundDarker',
          borderTopRightRadius: themeRadii.large,
        }}
      >
        <AllProjectsRightColumn />
      </ScrollContainer>
    </>
  );
};
