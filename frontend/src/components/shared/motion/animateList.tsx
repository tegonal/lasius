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
import { stringHash } from 'lib/stringHash';

const item = {
  hidden: { opacity: 0, transition: { duration: 0.5 } },
  show: (i: number) => ({ opacity: 1, transition: { delay: i * 0.12, duration: 0.5 } }),
  exit: { opacity: 0, transition: { duration: 0.5 } },
};

function getComponentKey(component: React.ReactElement, index: number) {
  if (typeof component === 'object' && component !== null && component.key != null) {
    return stringHash(component.key);
  }
  return stringHash(index);
}

type Props = {
  children: React.ReactElement[];
  popLayout?: boolean;
};
export const AnimateList: React.FC<Props> = ({ children, popLayout }) => {
  const mode = popLayout ? 'popLayout' : undefined;
  return (
    <AnimatePresence mode={mode}>
      {React.Children.map(children, (child, idx) => (
        <m.div
          exit="exit"
          initial="hidden"
          animate="show"
          variants={item}
          custom={idx}
          key={getComponentKey(child, idx)}
        >
          {child}
        </m.div>
      ))}
    </AnimatePresence>
  );
};
