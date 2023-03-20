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

import { UIEvent, useEffect, useState } from 'react';

interface UseScrollPagination<E> {
  onScroll: (event: UIEvent<HTMLDivElement>) => void;
  visibleElements: E[];
}

function useScrollPagination<E>(
  elements: E[],
  showItemsPerStep = 30,
  scrollBeforeEnd = 50
): UseScrollPagination<E> {
  const [shownNumberOfItems, setShownNumberOfItems] = useState(showItemsPerStep);

  const onScroll = (event: UIEvent<HTMLDivElement>) => {
    if (!event.target) return;
    const { scrollHeight, scrollTop, clientHeight } = event.target as HTMLDivElement;
    const scroll = scrollHeight - scrollTop - clientHeight;

    if (scroll < scrollBeforeEnd && elements.length > shownNumberOfItems) {
      const newNumberOfItems = Math.min(shownNumberOfItems + showItemsPerStep, elements.length);
      if (newNumberOfItems > shownNumberOfItems) {
        setShownNumberOfItems(newNumberOfItems);
      }
    }
  };

  useEffect(() => {
    setShownNumberOfItems(showItemsPerStep);
  }, [elements.length, showItemsPerStep]);

  return { onScroll, visibleElements: elements.slice(0, shownNumberOfItems) };
}

export default useScrollPagination;
