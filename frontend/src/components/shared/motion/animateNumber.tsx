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

import { animate } from 'framer-motion';
import React, { useEffect, useRef } from 'react';
import { isInteger, padStart, round } from 'lodash';
import { countDecimals } from 'lib/countDecimals';

type Props = {
  from: number;
  to: number;
  leftpad?: number;
};

const number = (value: number, from: number, to: number) =>
  round(value, isInteger(to) ? (to === 0 ? countDecimals(from) : 0) : countDecimals(to));

const numberLeftpadded = (value: number, from: number, to: number, leftpad: number) =>
  padStart(number(value, from, to).toString(), 1 + leftpad, '0');

/**
 * AnimateNumber component: Animates a number from one value to another.
 * @param from
 * @param to
 * @param leftpad
 */
export const AnimateNumber: React.FC<Props> = ({ from, to, leftpad = 0 }) => {
  const nodeRef: any = useRef();

  useEffect(() => {
    const node = nodeRef.current;

    const controls = animate(from, to, {
      duration: 0.33,
      onUpdate(value) {
        node.textContent =
          leftpad > 0 ? numberLeftpadded(value, from, to, leftpad) : number(value, from, to);
      },
    });

    return () => controls.stop();
  }, [from, leftpad, to]);

  return <span ref={nodeRef} />;
};
