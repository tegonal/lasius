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
import { Box, Flex } from 'theme-ui';
import { Button } from '@theme-ui/components';
import { SelectedTab } from 'components/shared/motion/selectedTab';
import { useStore } from 'storeContext/store';
import { useEffectOnce } from 'usehooks-ts';

type TabItem = { label: string; component: React.ReactNode; icon?: string };

type Props = {
  tabs: TabItem[];
};

export const Tabs: React.FC<Props> = ({ tabs }) => {
  const {
    state: { tabViews },
    dispatch,
  } = useStore();

  const [selected, setSelected] = useState<number>(0);

  const tabId = useId();

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

  return (
    <Flex
      sx={{
        flexDirection: 'column',
        label: 'Tabs',
        width: '100%',
        borderBottom: '1px solid',
        borderBottomColor: 'containerTextColorMuted',
      }}
    >
      <Flex
        sx={{
          flexShrink: 0,
          flexDirection: 'row',
          justifyContent: 'flex-start',
          gap: 3,
          borderBottom: '1px solid',
          borderBottomColor: 'containerTextColorMuted',
        }}
      >
        {tabs.map((item, index) => (
          <Box key={item.label} sx={{ position: 'relative', zIndex: 1 }}>
            {index === selected ? <SelectedTab layoutId={tabId} /> : null}
            <Button
              variant={index === selected ? 'tabSelected' : 'tab'}
              onClick={() => setSelected(index)}
              sx={{
                zIndex: 2,
                position: 'relative',
              }}
              aria-label={item.label}
            >
              {item.label}
            </Button>
          </Box>
        ))}
      </Flex>
      <Box
        sx={{
          label: `menuNavColumn-${selected}`,
          py: 3,
          width: '100%',
        }}
      >
        <AnimatePresence initial={false} mode="wait">
          <m.div
            key={`menuNavColumn-${selected}`}
            animate={{ opacity: 1, y: 0 }}
            initial={{ opacity: 0, y: 20 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.15 }}
          >
            {tabs[selected].component}
          </m.div>
        </AnimatePresence>
      </Box>
    </Flex>
  );
};
