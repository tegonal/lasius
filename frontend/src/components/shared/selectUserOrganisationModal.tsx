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
import { useTranslation } from 'next-i18next';
import { ModelsEntityReference, ModelsUserOrganisation } from 'lib/api/lasius';
import { Box, Flex, Grid, Heading } from 'theme-ui';
import { AvatarOrganisation } from 'components/shared/avatar/avatarOrganisation';
import { flexColumnJustifyCenterAlignCenter } from 'styles/shortcuts';
import { CardSmall } from 'components/shared/cardSmall';
import { Icon } from 'components/shared/icon';
import useModal from 'components/modal/hooks/useModal';
import { MODAL_SELECT_ORGANISATION } from 'components/shared/selectUserOrganisation';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { noop } from 'lodash';

type Props = {
  onSelect?: (organisation: ModelsEntityReference) => void;
  selected?: ModelsEntityReference;
};

export const SelectUserOrganisationModal: React.FC<Props> = ({ selected, onSelect = noop }) => {
  const { t } = useTranslation('common');
  const { selectedOrganisationId, organisations, setSelectedOrganisation } = useOrganisation();
  const { closeModal } = useModal(MODAL_SELECT_ORGANISATION);

  const selectOrganisation = async (orgReference: ModelsEntityReference) => {
    await setSelectedOrganisation(orgReference);
    onSelect(orgReference);
    closeModal();
  };

  const isCurrent = (item: ModelsUserOrganisation) => {
    if (selected) {
      return selected.id === item.organisationReference.id;
    }
    return item.organisationReference.id === selectedOrganisationId;
  };

  return (
    <Grid sx={{ gap: 3, gridTemplateColumns: '1fr 1fr 1fr' }}>
      <Heading as="h1" sx={{ gridColumn: '1 / 4' }}>
        {t('Select organisation')}
      </Heading>
      {organisations.map((item) => (
        <CardSmall
          key={item.organisationReference.id}
          onClick={() => selectOrganisation(item.organisationReference)}
        >
          <Flex sx={{ ...flexColumnJustifyCenterAlignCenter(), pt: 2 }}>
            <AvatarOrganisation name={item.organisationReference.key} size={64} />
          </Flex>
          <Box sx={{ lineHeight: 'normal' }}>
            {item.private ? t('My personal organisation') : item.organisationReference.key}
          </Box>
          {isCurrent(item) && (
            <Box title={t('Selected')} sx={{ position: 'absolute', top: 2, right: 2 }}>
              <Icon name="check-circle-1-interface-essential" size={18} />
            </Box>
          )}
        </CardSmall>
      ))}
    </Grid>
  );
};
