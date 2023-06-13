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
import { Box, Flex } from 'theme-ui';
import {
  flexColumnJustifyCenterAlignEnd,
  flexRowJustifyBetweenAlignCenter,
  flexRowJustifyStartAlignCenter,
} from 'styles/shortcuts';
import { BookingName } from 'layout/pages/user/index/bookingName';
import { BookingFromTo } from 'layout/pages/user/index/bookingFromTo';
import { TagList } from 'components/shared/tagList';
import { BookingDuration } from 'layout/pages/user/index/bookingDuration';
import { BookingItemContext } from 'layout/pages/user/index/list/bookingItemContext';
import { Responsively } from 'components/shared/responsively';
import { useTranslation } from 'next-i18next';
import { Icon } from 'components/shared/icon';
import { ToolTip } from 'components/shared/toolTip';
import useModal from 'components/modal/hooks/useModal';
import { ModalResponsive } from 'components/modal/modalResponsive';
import { BookingAddUpdateForm } from 'layout/pages/user/index/bookingAddUpdateForm';
import { Button } from '@theme-ui/components';
import { augmentBookingsList } from 'lib/api/functions/augmentBookingsList';

type ItemType = ReturnType<typeof augmentBookingsList>[number];

type Props = {
  item: ItemType;
};

export const BookingItem: React.FC<Props> = ({ item }) => {
  const { t } = useTranslation();
  const editModal = useModal(`EditModal-${item.id}`);
  const addModal = useModal(`AddModal-${item.id}`);
  const addBetweenModal = useModal(`AddBetweenModal-${item.id}`);

  return (
    <Flex
      sx={{
        label: 'BookingItem',
        ...flexRowJustifyBetweenAlignCenter(2),
        px: [2, 2, 4],
        py: [3, 3, 4],
        ...(item.overlapsWithNext
          ? { borderBottom: '4px dotted', borderBottomColor: 'warning' }
          : {
              borderBottom: '1px solid',
              borderBottomColor: 'containerTextColorMuted',
            }),
        ...(item.isMostRecent && {
          borderTop: '1px solid',
          borderTopColor: 'containerTextColorMuted',
        }),
        position: 'relative',
      }}
    >
      <Flex sx={{ flexDirection: 'column', gap: 1 }}>
        <BookingName item={item} />
        <TagList items={item.tags} />
      </Flex>
      <Flex sx={{ ...flexRowJustifyStartAlignCenter([2, 2, 4]), height: '100%', flexShrink: 0 }}>
        <Responsively mode="show" on={['md', 'lg']}>
          <Flex sx={{ ...flexRowJustifyStartAlignCenter([2, 2, 4]), height: '100%' }}>
            <BookingFromTo item={item} />
            <BookingDuration item={item} />
          </Flex>
        </Responsively>
        <Responsively mode="show" on={['xs', 'sm']}>
          <Flex sx={{ ...flexColumnJustifyCenterAlignEnd(1), height: '100%' }}>
            <BookingFromTo item={item} />
            <BookingDuration item={item} />
          </Flex>
        </Responsively>
        <BookingItemContext item={item} />
      </Flex>
      {item.overlapsWithNext && (
        <Flex
          sx={{
            position: 'absolute',
            inset: 'auto 0 0 0',
            textAlign: 'center',
            justifyContent: 'center',
            alignItems: 'center',
          }}
        >
          <Box
            sx={{
              position: 'absolute',
              color: 'warning',
              backgroundColor: 'containerBackground',
              padding: 1,
              borderRadius: '50%',
            }}
          >
            <ToolTip
              toolTipContent={t(
                'These two bookings overlap. Click to edit the top one and adjust the time.'
              )}
              width={240}
            >
              <Button variant="icon" type="button" onClick={editModal.openModal}>
                <Icon name="alert-triangle" size={20} />
              </Button>
            </ToolTip>
          </Box>
        </Flex>
      )}
      {item.isMostRecent && (
        <Flex
          sx={{
            position: 'absolute',
            inset: '0 0 auto 0',
            textAlign: 'center',
            justifyContent: 'center',
            alignItems: 'center',
          }}
        >
          <Box
            sx={{
              position: 'absolute',
              backgroundColor: 'containerBackground',
              padding: 1,
              borderRadius: '50%',
            }}
          >
            <Button
              variant="icon"
              type="button"
              title={t('Add booking')}
              onClick={addModal.openModal}
            >
              <Icon name="add-circle" size={20} />
            </Button>
          </Box>
        </Flex>
      )}
      {item.allowInsert && (
        <Flex
          sx={{
            position: 'absolute',
            inset: 'auto 0 0 0',
            textAlign: 'center',
            justifyContent: 'center',
            alignItems: 'center',
          }}
        >
          <Box
            sx={{
              position: 'absolute',
              backgroundColor: 'containerBackground',
              padding: 1,
              borderRadius: '50%',
            }}
          >
            <ToolTip
              toolTipContent={t(
                'There is a gap between these two bookings. Click to add a booking in between.'
              )}
              width={240}
            >
              <Button
                variant="icon"
                type="button"
                title={t('Insert booking')}
                onClick={addBetweenModal.openModal}
              >
                <Icon name="add-circle" size={20} />
              </Button>
            </ToolTip>
          </Box>
        </Flex>
      )}
      <ModalResponsive modalId={editModal.modalId}>
        <BookingAddUpdateForm
          mode="update"
          itemUpdate={item}
          onSave={editModal.closeModal}
          onCancel={editModal.closeModal}
        />
      </ModalResponsive>
      <ModalResponsive modalId={addModal.modalId}>
        <BookingAddUpdateForm
          mode="add"
          itemReference={item}
          onSave={addModal.closeModal}
          onCancel={addModal.closeModal}
        />
      </ModalResponsive>
      <ModalResponsive modalId={addBetweenModal.modalId}>
        <BookingAddUpdateForm
          mode="addBetween"
          itemReference={item}
          onSave={addBetweenModal.closeModal}
          onCancel={addBetweenModal.closeModal}
        />
      </ModalResponsive>
    </Flex>
  );
};
