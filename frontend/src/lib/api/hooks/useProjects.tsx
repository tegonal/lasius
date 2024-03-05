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

import { SelectAutocompleteSuggestionType } from 'components/forms/input/inputSelectAutocomplete';
import { ModelsUserProject } from 'lib/api/lasius';
import { orderBy } from 'lodash';
import { useProfile } from 'lib/api/hooks/useProfile';

export const useProjects = () => {
  const { profile } = useProfile();
  const projectSuggestions = (): SelectAutocompleteSuggestionType[] => {
    if (profile?.organisations) {
      const org = profile.organisations.find(
        (item) => item.organisationReference.id === profile.settings?.lastSelectedOrganisation?.id
      );
      return (
        orderBy(
          org?.projects.map((item) => item.projectReference),
          (data) => data.key
        ) || []
      );
    }
    return [];
  };

  const userProjects = (): ModelsUserProject[] => {
    if (profile?.organisations) {
      const org = profile?.organisations.find(
        (item) => item.organisationReference.id === profile.settings?.lastSelectedOrganisation?.id
      );
      return orderBy(org?.projects, (data) => data.projectReference.key) || [];
    }
    return [];
  };

  return {
    projectSuggestions,
    userProjects,
  };
};
