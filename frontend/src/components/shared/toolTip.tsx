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
import { useEffect, useRef, useState } from 'react';
import { Box } from '@theme-ui/components';
import { useBoolean, useHover } from 'usehooks-ts';
import { usePopper } from 'react-popper';
import { Popover } from '@headlessui/react';
import { Badge } from 'theme-ui';

type Props = {
  children: React.ReactNode | string;
  toolTipContent: React.ReactNode | string;
  offset?: number;
  placement?: 'top' | 'bottom' | 'left' | 'right';
  width?: number | 'auto';
};

/**
 * Tooltip component: Wraps a child component and displays a tooltip with {label} when hovering over the child.
 * @param children
 * @param toolTipContent
 * @param offset
 * @param placement
 */
export const ToolTip: React.FC<Props> = ({
  children,
  toolTipContent,
  offset = 8,
  placement = 'top',
  width = 'auto',
}) => {
  const visible = useBoolean(false);

  const [referenceElement, setReferenceElement] = useState();
  const [popperElement, setPopperElement] = useState();

  const { styles, attributes } = usePopper(referenceElement, popperElement, {
    placement,
    modifiers: [
      { name: 'offset', options: { offset: [0, offset] } },
      {
        name: 'preventOverflow',
        options: {
          altAxis: true,
          padding: 5,
          altBoundary: true,
        },
      },
    ],
  });

  const hoverRef = useRef(null);
  const isHover = useHover(hoverRef);

  useEffect(() => {
    if (isHover) {
      visible.setTrue();
    } else {
      visible.setFalse();
    }
  }, [isHover, visible]);

  return (
    <Box sx={{ ':hover': { cursor: 'pointer' } }} ref={hoverRef}>
      <Popover>
        <Box ref={setReferenceElement} onClick={visible.toggle}>
          {children}
        </Box>
        {visible.value && (
          <Popover.Panel
            static
            as={Box}
            ref={setPopperElement as any}
            sx={{ ...(styles.popper as any), zIndex: 9 }}
            {...attributes.popper}
          >
            {({ close }) => (
              <Badge variant="tooltip" onClick={() => close()} sx={{ width }}>
                {toolTipContent}
              </Badge>
            )}
          </Popover.Panel>
        )}
      </Popover>
    </Box>
  );
};
