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

import React, { useEffect, useId, useState } from 'react';
import { AnimatePresence, m } from 'framer-motion';
import { Box, Flex, Grid } from 'theme-ui';
import { Button } from '@theme-ui/components';
import { Icon } from 'components/shared/icon';
import { SelectedTabIcon } from 'components/shared/motion/selectedTabIcon';
import { useEffectOnce } from 'usehooks-ts';
import { IconNames } from 'types/iconNames';
import { fullWidthHeight } from 'styles/shortcuts';
import { ScrollContainer } from 'components/scrollContainer';
import { useStore } from 'storeContext/store';

const PresenceItem = m(Box);

export type IconTabsItem = {
  id: string;
  name: string;
  component: React.ReactNode;
  icon: IconNames;
};

type Props = {
  tabs: IconTabsItem[];
  position?: 'top' | 'left';
};

export const IconTabs: React.FC<Props> = ({ tabs, position = 'top' }) => {
  const {
    state: { tabViews },
    dispatch,
  } = useStore();

  const tabId = useId();

  const [selected, setSelected] = useState(0);

  useEffectOnce(() => {
    if (!tabViews.find((tab) => tab.id === tabId)) {
      dispatch({ type: 'tabview.add', payload: { id: tabId, activeIndex: 0 } });
    }
  });

  useEffect(() => {
    const tabView = tabViews.find((tab) => tab.id === tabId);
    if (tabView) {
      setSelected(tabView.activeIndex);
    }
  }, [tabViews, tabId]);

  const handleTabClick = (index: number) => {
    dispatch({ type: 'tabview.update', payload: { id: tabId, activeIndex: index } });
  };

  return (
    <Grid
      sx={{
        label: 'IconTabs',
        ...fullWidthHeight(),
        ...(position === 'top'
          ? { gridTemplateRows: 'min-content auto' }
          : { gridTemplateColumns: 'min-content auto' }),
        justifyContent: 'stretch',
        position: 'relative',
        overflow: 'auto',
        gap: 0,
      }}
    >
      <Flex
        sx={{
          pt: [0, 2],
          mx: [2, 3],
          ...(position === 'top'
            ? {
                borderBottom: '1px solid',
                borderBottomColor: 'muted',
                flexDirection: 'row',
                justifyContent: 'center',
              }
            : {
                borderRight: '1px solid',
                borderRightColor: 'muted',
                flexDirection: 'column',
                justifyContent: 'flex-start',
              }),
        }}
      >
        {tabs.map((item, index) => (
          <Box key={item.id} sx={{ position: 'relative', zIndex: 1 }}>
            {index === selected ? <SelectedTabIcon layoutId={tabId} radiusOn={position} /> : null}
            <Button
              variant="tabIcon"
              onClick={() => handleTabClick(index)}
              sx={{
                label: 'TabIconButton',
                zIndex: 2,
                position: 'relative',
                color: index === selected ? 'negativeText' : 'currentcolor',
              }}
              title={item.name}
              aria-label={item.name}
            >
              <Icon name={item.icon} size={24} />
            </Button>
          </Box>
        ))}
      </Flex>
      <ScrollContainer sx={{ pt: 2 }}>
        <AnimatePresence initial={false} mode="popLayout">
          <PresenceItem
            key={`menuBooking-${selected}`}
            animate={{ opacity: 1, ...(position === 'top' ? { y: 0 } : { x: 0 }) }}
            initial={{ opacity: 0, ...(position === 'top' ? { y: 20 } : { x: 10 }) }}
            exit={{ opacity: 0, ...(position === 'top' ? { y: -20 } : { x: -10 }) }}
            transition={{ duration: 0.15 }}
          >
            {tabs[selected].component}
          </PresenceItem>
        </AnimatePresence>
      </ScrollContainer>
    </Grid>
  );
};
