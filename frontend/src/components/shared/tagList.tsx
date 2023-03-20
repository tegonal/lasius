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
import { Badge, Box, Flex } from 'theme-ui';
import { noop } from 'lodash';
import { ModelsTags } from 'types/common';
import { ToolTip } from 'components/shared/toolTip';
import { Icon } from 'components/shared/icon';
import { useTranslation } from 'next-i18next';
import { clickableStyle, outlineStyle } from 'styles/shortcuts';

type ItemLabelProps = {
  label: string;
  summary: string;
};
const ItemLabel: React.FC<ItemLabelProps> = ({ label, summary }) => {
  let processedLabel = label;
  const cutoff = 18;
  const labelWordCount = label.split(' ').length;
  if (label.length > cutoff && labelWordCount > 2 && !summary) {
    processedLabel = `${label.substring(0, cutoff)}...`;
    return (
      <ToolTip toolTipContent={label}>
        <>{processedLabel}</>
      </ToolTip>
    );
  }

  if (summary) {
    return (
      <ToolTip toolTipContent={summary}>
        <>{`${processedLabel}: ${summary.substring(0, cutoff)}...`}</>
      </ToolTip>
    );
  }

  return <>{`${processedLabel}`}</>;
};

type PropsTagContainer = {
  item: ModelsTags;
  clickHandler?: (tag: ModelsTags) => void;
  hideRemoveIcon?: boolean;
  active?: boolean;
};

export const Tag: React.FC<PropsTagContainer> = ({
  active,
  clickHandler,
  hideRemoveIcon,
  item,
}) => {
  const { t } = useTranslation('common');
  const clickableAndRemovable = !!clickHandler && !hideRemoveIcon;
  const clickableAndNotRemovable = !!clickHandler && hideRemoveIcon;

  let tagVariant;

  switch (true) {
    case item.type === 'SimpleTag' || item.type === 'TagGroup':
      tagVariant = clickableAndNotRemovable ? `tag${item.type}Clickable` : `tag${item.type}`;
      break;
    case 'summary' in item:
      tagVariant = clickableAndNotRemovable ? 'tagWithSummaryClickable' : 'tagWithSummary';
      break;
    default:
      tagVariant = clickableAndNotRemovable ? 'tagSimpleTagClickable' : 'tagSimpleTag';
      break;
  }

  const summary = 'summary' in item && item.summary ? item.summary : '';

  return (
    <Badge
      variant={tagVariant}
      onClick={clickableAndNotRemovable ? () => clickHandler(item) : noop}
      sx={active ? outlineStyle : {}}
    >
      <ItemLabel label={item.id} summary={summary} />
      {clickableAndRemovable && (
        <Box
          onClick={() => clickHandler(item)}
          sx={{ mr: '-7px', ...clickableStyle(), color: 'currentcolor' }}
          title={t('Remove')}
        >
          <Icon name="remove-circle-interface-essential" size={18} />
        </Box>
      )}
    </Badge>
  );
};

type Props = {
  items: ModelsTags[] | null | undefined;
  clickHandler?: (tag: ModelsTags) => void;
  hideRemoveIcon?: boolean;
};

export const TagList: React.FC<Props> = ({ items, clickHandler, hideRemoveIcon = false }) => {
  if (!items || items.length < 1) return null;
  return (
    <Flex sx={{ label: 'TagList', gap: 1, flexWrap: 'wrap' }}>
      {items
        .filter((item) => !!item.id.trim())
        .map((item) => (
          <Tag
            key={item.id}
            item={item}
            clickHandler={clickHandler}
            hideRemoveIcon={hideRemoveIcon}
          />
        ))}
    </Flex>
  );
};
