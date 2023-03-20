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
import { AnimatePresence, m } from 'framer-motion';

const item = {
  hidden: { opacity: 0, transition: { duration: 1 } },
  show: (i: number) => ({ opacity: 1, transition: { delay: i * 0.15, duration: 0.2 } }),
  exit: { opacity: 0, transition: { duration: 1 } },
};

type Props = {
  children: React.ReactNode;
  hash?: string;
  useAvailableSpace?: boolean;
};
export const AnimateChange: React.FC<Props> = ({ children, hash, useAvailableSpace }) => {
  const style = useAvailableSpace ? { width: '100%', height: '100%' } : {};
  return (
    <AnimatePresence key={hash} mode="popLayout">
      {React.Children.map(children, (child, idx) => (
        <m.div
          exit="exit"
          initial="hidden"
          animate="show"
          variants={item}
          custom={idx}
          style={style}
        >
          {child}
        </m.div>
      ))}
    </AnimatePresence>
  );
};
