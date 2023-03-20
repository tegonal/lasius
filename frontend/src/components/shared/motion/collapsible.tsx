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

import * as React from 'react';
import { useId, useState } from 'react';
import { AnimatePresence, m } from 'framer-motion';

type Props = {
  children: React.ReactNode;
  label: string;
  initiallyOpen?: boolean;
};

/**
 * Collapsible component: Wraps a child component and and reveals it when clicking on the label.
 * @param label
 * @param children
 * @param initiallyOpen
 */
export const Collapsible: React.FC<Props> = ({ label, children, initiallyOpen = false }) => {
  const [expanded, setExpanded] = useState<boolean>(initiallyOpen);
  const id = useId();

  return (
    <>
      <m.header
        initial={false}
        animate={{ backgroundColor: expanded ? '#FF0088' : '#0055FF' }}
        onClick={() => setExpanded(!expanded)}
      >
        {label}
      </m.header>
      <AnimatePresence initial={false}>
        {expanded && (
          <m.section
            key={id}
            initial="collapsed"
            animate="open"
            exit="collapsed"
            variants={{
              open: { opacity: 1, height: 'auto' },
              collapsed: { opacity: 0, height: 0 },
            }}
            transition={{ duration: 0.8, ease: [0.04, 0.62, 0.23, 0.98] }}
          >
            {children}
          </m.section>
        )}
      </AnimatePresence>
    </>
  );
};
