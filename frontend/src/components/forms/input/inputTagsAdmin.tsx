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

import { Combobox } from '@headlessui/react';
import { Tag, TagList } from 'components/shared/tagList';
import { differenceBy, filter, noop, uniqBy } from 'lodash';
import { useTranslation } from 'next-i18next';
import React, { useEffect, useState } from 'react';
import { Box, Flex, Input } from 'theme-ui';
import { ModelsTags } from 'types/common';
import { Controller, useFormContext } from 'react-hook-form';
import { clickableStyle } from 'styles/shortcuts';
import { Icon } from 'components/shared/icon';
import { ModelsSimpleTag } from 'lib/api/lasius';
import { DropdownList } from 'components/forms/input/shared/dropdownList';

type Props = {
  name: 'tagGroups' | 'simpleTags';
  tags: ModelsTags[] | [];
  tagGroupIndex?: number;
};

export const InputTagsAdmin: React.FC<Props> = ({ name, tags = [], tagGroupIndex = 0 }) => {
  const { t } = useTranslation('common');
  const parentFormContext = useFormContext();
  const [inputText, setInputText] = useState<string>('');

  const [selectedTags, setSelectedTags] = useState<ModelsTags[]>(tags);

  useEffect(() => {
    if (!parentFormContext) return () => null;
    const subscription = parentFormContext.watch((value, { name: fieldname }) => {
      if (name === fieldname) {
        if (
          fieldname === 'tagGroups' &&
          Array.isArray(value[name]) &&
          value[name][tagGroupIndex]?.relatedTags
        ) {
          setSelectedTags(value[name][tagGroupIndex].relatedTags);
        } else {
          setSelectedTags(value[name]);
        }
      }
    });
    return () => subscription.unsubscribe();
  }, [name, parentFormContext, tagGroupIndex]);

  const removeTag = (tag: ModelsTags) => {
    const toRemove = filter(selectedTags, { id: tag.id });
    const tags = differenceBy(selectedTags, toRemove, 'id');
    setSelectedTags(tags);
    parentFormContext.setValue(name, tags);
  };

  const addTag = (tag: any) => {
    const tags = uniqBy([...selectedTags, tag], 'id');
    setInputText('');
    setSelectedTags(tags);
    if (name === 'tagGroups') {
      const currentTags = parentFormContext.getValues(name);
      currentTags[tagGroupIndex].relatedTags = tags;
      parentFormContext.setValue(name, currentTags);
    } else {
      parentFormContext.setValue(name, tags);
    }
  };

  const inputTag: ModelsSimpleTag = { id: inputText, type: 'SimpleTag' };

  const displayCreateTag = inputText.length > 0 && !selectedTags.find((s) => s.id === inputText);

  if (!parentFormContext) return null;

  const preventDefault = (e: any) => {
    if (inputText) e.preventDefault();
  };

  return (
    <Box sx={{ label: 'InputTagsAdmin' }}>
      {Array.isArray(selectedTags) && selectedTags.length > 0 && (
        <Box sx={{ my: 2 }}>
          <TagList items={selectedTags} clickHandler={removeTag} />
        </Box>
      )}
      <Box sx={{ position: 'relative' }}>
        <Controller
          name={name}
          control={parentFormContext.control}
          render={() => (
            <Combobox value={selectedTags} onChange={addTag}>
              {({ open }) => (
                <>
                  {/* Wrapping the input with a Button is a hack for https://github.com/tailwindlabs/headlessui/discussions/1236,
                    Without that the combobox does not open when you click in the input */}
                  <Combobox.Button as={Box}>
                    <Combobox.Input
                      as={Input}
                      onChange={(e) => setInputText(e.currentTarget.value)}
                      onClick={preventDefault}
                      displayValue={() => inputText}
                      placeholder={t('Enter a tag to add it')}
                      autoComplete="off"
                    />
                    {inputText && (
                      <Box
                        sx={{
                          position: 'absolute',
                          right: 2,
                          top: '50%',
                          transform: 'translateY(-50%)',
                          ...clickableStyle(),
                        }}
                        onClick={() => setInputText('')}
                      >
                        <Icon name="remove-circle-interface-essential" size={20} />
                      </Box>
                    )}
                  </Combobox.Button>
                  <Combobox.Options as={Box}>
                    {open && displayCreateTag && (
                      <DropdownList sx={{ px: 2, display: 'flex', gap: 0, flexWrap: 'wrap' }}>
                        {displayCreateTag && (
                          <Combobox.Option
                            as={Flex}
                            key="create_tag"
                            sx={{
                              width: 'fit-content',
                              p: 1,
                              gap: 2,
                              alignItems: 'center',
                              flexBasis: '100%',
                              mb: 2,
                            }}
                            value={inputTag}
                          >
                            {({ active }) => (
                              <>
                                <Box sx={{ fontSize: 1 }}>{`${t('Custom tag')}: `}</Box>
                                <Tag
                                  active={active}
                                  item={inputTag}
                                  clickHandler={noop}
                                  hideRemoveIcon
                                />
                              </>
                            )}
                          </Combobox.Option>
                        )}
                      </DropdownList>
                    )}
                  </Combobox.Options>
                </>
              )}
            </Combobox>
          )}
        />
      </Box>
    </Box>
  );
};
