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
import { DropdownList } from 'components/forms/input/shared/dropdownList';
import { DropdownListItem } from 'components/forms/input/shared/dropdownListItem';
import { cleanStrForCmp } from 'lib/strings';
import { find, sortBy } from 'lodash';
import { useTranslation } from 'next-i18next';
import React, { useEffect, useState } from 'react';
import { clickableStyle } from 'styles/shortcuts';
import { Box, Input } from 'theme-ui';
import { Controller, useFormContext } from 'react-hook-form';
import { FormErrorBadge } from 'components/forms/formErrorBadge';
import { Icon } from 'components/shared/icon';
import { ModelsEntityReference } from 'lib/api/lasius';

export type SelectAutocompleteSuggestionType = ModelsEntityReference;

type Props = {
  suggestions: SelectAutocompleteSuggestionType[];
  name: string;
  required?: boolean;
};

export const InputSelectAutocomplete: React.FC<Props> = ({
  suggestions = [],
  name,
  required = false,
}) => {
  const { t } = useTranslation('common');
  const parentFormContext = useFormContext();

  const errors = parentFormContext?.formState.errors[name];

  const [inputText, setInputText] = useState<string>('');
  const [selected, setSelected] = useState<SelectAutocompleteSuggestionType | ''>('');

  const resetSelection = () => {
    setSelected('');
    setInputText('');
    parentFormContext?.setValue(name, null);
  };

  useEffect(() => {
    if (!parentFormContext) return () => null;
    const subscription = parentFormContext.watch((value, { name: fieldname }) => {
      if (name === fieldname && value[name]) {
        const preSelected = find(suggestions, { id: value[name] });
        if (preSelected) {
          setSelected(preSelected);
          setInputText(preSelected.key);
        }
      }
      if (name === fieldname && !value[name]) {
        setSelected('');
        setInputText('');
      }
    });
    return () => subscription.unsubscribe();
  }, [name, parentFormContext, selected, suggestions]);

  const availableSuggestions = sortBy(
    suggestions.filter((item) => cleanStrForCmp(item.key).includes(cleanStrForCmp(inputText))),
    ['key']
  );

  const rules = required
    ? {
        validate: {
          required: (v: string | undefined) => !!v,
        },
      }
    : {};

  if (!parentFormContext) return null;

  return (
    <>
      <Box sx={{ label: 'InputSelectAutocomplete' }}>
        <Box sx={{ position: 'relative' }}>
          <Controller
            name={name}
            control={parentFormContext.control}
            rules={rules}
            render={({ field: { value, onChange } }) => (
              <Combobox
                value={value}
                onChange={(change: SelectAutocompleteSuggestionType) => onChange(change.id)}
              >
                {({ open }) => (
                  <>
                    {/* Wrapping the input with a Button is a hack for https://github.com/tailwindlabs/headlessui/discussions/1236,
                    Without that the combobox does not open when you click in the input */}
                    <Combobox.Button as={Box}>
                      <Combobox.Input
                        as={Input}
                        onChange={(e) => setInputText(e.currentTarget.value)}
                        placeholder={t('Select project')}
                        autoComplete="off"
                        value={inputText}
                      />
                    </Combobox.Button>
                    {(selected || inputText) && (
                      <Box
                        sx={{
                          position: 'absolute',
                          right: 2,
                          top: '50%',
                          transform: 'translateY(-50%)',
                          ...clickableStyle(),
                        }}
                        onClick={resetSelection}
                      >
                        <Icon name="remove-circle-interface-essential" size={20} />
                      </Box>
                    )}
                    <Combobox.Options as={Box}>
                      {open && availableSuggestions.length > 0 && (
                        <DropdownList>
                          {availableSuggestions.map((suggestion) => (
                            <Combobox.Option as={Box} key={suggestion.key} value={suggestion}>
                              {({ active, selected }) => (
                                <DropdownListItem
                                  key={suggestion.id}
                                  itemValue={suggestion.key}
                                  itemSearchString={inputText}
                                  active={active}
                                  selected={selected}
                                />
                              )}
                            </Combobox.Option>
                          ))}
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
      <FormErrorBadge error={errors} />
    </>
  );
};
