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

import { Alert, Box, Close, Flex } from 'theme-ui';
import React, { useEffect } from 'react';
import { AnimatePresence, m } from 'framer-motion';
import { ToastViewType } from 'types/dynamicViews';
import { useIsClient } from 'usehooks-ts';
import { useToast } from 'components/toasts/hooks/useToast';

const ToastItem: React.FC<{ item: ToastViewType }> = ({ item }) => {
  const { removeToast } = useToast();

  useEffect(() => {
    const timer = setTimeout(() => {
      removeToast(item);
    }, item.ttl);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <Box sx={{ pt: 3 }}>
      <Alert variant={`toast-${item.type}`}>
        {item.message}
        <Close sx={{ '&:hover': { cursor: 'pointer' } }} onClick={() => removeToast(item)} />
      </Alert>
    </Box>
  );
};

export const Toasts: React.FC = () => {
  const { toastViews } = useToast();
  const isClient = useIsClient();
  if (!isClient) return null;

  return (
    <Flex sx={{ flexDirection: 'column-reverse', position: 'fixed', bottom: 4, right: 4 }}>
      <AnimatePresence mode="sync">
        {toastViews.map((toast) => (
          <m.div
            key={toast.id}
            initial={{ opacity: 0, y: 50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 50 }}
          >
            <ToastItem item={toast} />
          </m.div>
        ))}
      </AnimatePresence>
    </Flex>
  );
};
