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

import React, { useEffect, useState } from 'react';
import { Alert, Box, Flex, Grid, Input, Label } from 'theme-ui';
import { useTranslation } from 'next-i18next';
import { Button } from '@theme-ui/components';
import { FormBody } from 'components/forms/formBody';
import { FormElement } from 'components/forms/formElement';
import { FormProvider, useForm } from 'react-hook-form';
import { useOrganisation } from 'lib/api/hooks/useOrganisation';
import { getGetProjectListKey, updateProject } from 'lib/api/lasius/projects/projects';
import { ModelsProject, ModelsSimpleTag, ModelsTagGroup, ModelsUserProject } from 'lib/api/lasius';
import { useSWRConfig } from 'swr';
import { getGetUserProfileKey } from 'lib/api/lasius/user/user';
import { useToast } from 'components/toasts/hooks/useToast';
import {
  getTagsByProject,
  useGetTagsByProject,
} from 'lib/api/lasius/user-organisations/user-organisations';
import { InputTagsAdmin } from 'components/forms/input/inputTagsAdmin';
import { Tag } from 'components/shared/tagList';
import { isEqual, noop, unionWith } from 'lodash';
import { Icon } from 'components/shared/icon';
import { preventEnterOnForm } from 'components/forms/input/shared/preventEnterOnForm';
import { themeRadii } from 'styles/theme/radii';
import { P } from 'components/tags/p';
import { tagGroupTemplate } from 'projectConfig/tagGroupTemplate';
import { FormErrorBadge } from 'components/forms/formErrorBadge';
import { stringHash } from 'lib/stringHash';
import { logger } from 'lib/logger';

type Props = {
  item?: ModelsProject | ModelsUserProject;
  mode: 'add' | 'update';
  onSave: () => void;
  onCancel: () => void;
};

type FormValues = {
  newTagGroupName: string;
  tagGroups: ModelsTagGroup[] | [];
  simpleTags: ModelsSimpleTag[] | [];
};

export const ProjectAddUpdateTagsForm: React.FC<Props> = ({ item, onSave, onCancel, mode }) => {
  const { t } = useTranslation('common');

  const hookForm = useForm<FormValues>({
    defaultValues: {
      newTagGroupName: '',
      tagGroups: [],
      simpleTags: [],
    },
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const { selectedOrganisationId } = useOrganisation();
  const { addToast } = useToast();

  const { mutate } = useSWRConfig();

  const projectId = (item && 'id' in item ? item.id : item?.projectReference.id) || '';
  const projectKey = (item && 'key' in item ? item.key : item?.projectReference.key) || '';
  const projectOrganisationId =
    (item && 'organisationReference' in item
      ? item.organisationReference.id
      : selectedOrganisationId) || selectedOrganisationId;

  const bookingCategories = useGetTagsByProject(selectedOrganisationId, projectId);

  useEffect(() => {
    if (item && bookingCategories.data) {
      // filter taggroups and simpletags into separate arrays for editing, we are merging them back together on submit
      const tagGroups = bookingCategories.data.filter((tag) => tag.type === 'TagGroup') as
        | ModelsTagGroup[]
        | [];

      const simpleTags = bookingCategories.data.filter((tag) => tag.type === 'SimpleTag') as
        | ModelsSimpleTag[]
        | [];

      hookForm.setValue('tagGroups', tagGroups);
      hookForm.setValue('simpleTags', simpleTags);
    }
  }, [bookingCategories.data, hookForm, item, projectKey]);

  const onSubmit = async () => {
    setIsSubmitting(true);
    const { simpleTags, tagGroups } = hookForm.getValues();
    const bookingCategories = [...simpleTags, ...tagGroups];
    logger.info('submit', bookingCategories);
    if (mode === 'update' && item) {
      await updateProject(projectOrganisationId, projectId, {
        ...item,
        bookingCategories,
      });
    }
    addToast({ message: t('Project updated'), type: 'SUCCESS' });
    await mutate(getGetProjectListKey(projectOrganisationId));
    await mutate(getGetUserProfileKey());
    await mutate(getTagsByProject(selectedOrganisationId, projectId));
    setIsSubmitting(false);
    onSave();
  };

  // useEffect(() => {
  //   const subscription = hookForm.watch((value, { name }) => {
  //     switch (name) {
  //       case 'simpleTags':
  //         if (value.simpleTags) {
  //           logger.info('bookingCategories', value.simpleTags);
  //         }
  //         break;
  //       case 'tagGroups':
  //         if (value.tagGroups) {
  //           logger.info('bookingCategories', value.tagGroups);
  //         }
  //         break;
  //       default:
  //         break;
  //     }
  //   });
  //   return () => subscription.unsubscribe();
  // }, [hookForm]);

  const addTemplate = () => {
    const tagGroups = hookForm.getValues('tagGroups');
    const simpleTags = hookForm.getValues('simpleTags');

    const newTagGroups = tagGroupTemplate.filter((tag) => tag.type === 'TagGroup') as
      | ModelsTagGroup[]
      | [];

    const newSimpleTags = tagGroupTemplate.filter((tag) => tag.type === 'SimpleTag') as
      | ModelsSimpleTag[]
      | [];

    hookForm.setValue('tagGroups', unionWith(tagGroups, newTagGroups, isEqual));
    hookForm.setValue('simpleTags', unionWith(simpleTags, newSimpleTags, isEqual));

    hookForm.trigger('tagGroups');
    hookForm.trigger('simpleTags');
  };

  const removeTagGroup = (index: number) => {
    const tagGroups = hookForm.getValues('tagGroups');
    tagGroups.splice(index, 1);
    logger.info('tagGroups', tagGroups);
    hookForm.setValue('tagGroups', tagGroups);
    hookForm.trigger('tagGroups');
  };

  const createTagGroup = () => {
    const newTagGroupName = hookForm.getValues('newTagGroupName');
    const tagGroups: ModelsTagGroup[] = hookForm.getValues('tagGroups');
    if (!newTagGroupName) {
      addToast({
        message: t('Tag group name is required'),
        type: 'ERROR',
      });
      return;
    }

    if (tagGroups.find((tagGroup) => tagGroup.id === newTagGroupName)) {
      addToast({
        message: t('Tag group already exists'),
        type: 'ERROR',
      });
      return;
    }

    const newTagGroup: ModelsTagGroup = {
      id: newTagGroupName,
      type: 'TagGroup',
      relatedTags: [
        {
          id: 'Billable',
          type: 'SimpleTag',
        },
      ],
    };
    tagGroups.push(newTagGroup);
    hookForm.setValue('tagGroups', tagGroups);
    hookForm.trigger('tagGroups');
  };

  return (
    <FormProvider {...hookForm}>
      <Box sx={{ width: 'auto', position: 'relative' }}>
        {/* eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions */}
        <form onSubmit={hookForm.handleSubmit(onSubmit)} onKeyDown={(e) => preventEnterOnForm(e)}>
          <Grid gap={4} sx={{ gridTemplateColumns: 'auto 240px', width: '100%' }}>
            <Box>
              <FormBody>
                <FormElement>
                  <Label htmlFor="simpleTags">{t('Tag groups')}</Label>
                  {hookForm.getValues('tagGroups').length === 0 && (
                    <Flex>
                      <Alert variant="info" sx={{ my: 3, maxWidth: 500 }}>
                        {t(
                          'Tag groups allow easy filtering of a large number of bookings without users having to think too much about tagging rules. While adding a booking, a user can choose the tag group and essentially add multiple tags at the same time.'
                        )}
                      </Alert>
                    </Flex>
                  )}
                  <Grid gap={3} sx={{ gridTemplateColumns: '1fr 1fr 1fr', width: '100%', mt: 1 }}>
                    {hookForm.getValues('tagGroups').map((tagGroup: ModelsTagGroup, index) => (
                      <Flex
                        sx={{
                          gap: 3,
                          p: 3,
                          backgroundColor: 'containerBackgroundDarker',
                          borderRadius: themeRadii.medium,
                        }}
                        key={stringHash({ tagGroup, index })}
                      >
                        <Box sx={{ flexGrow: 1 }}>
                          <Box sx={{ mb: 2, display: 'inline-block' }}>
                            <Tag
                              key={tagGroup.id}
                              item={tagGroup}
                              clickHandler={noop}
                              hideRemoveIcon
                            />
                          </Box>
                          <InputTagsAdmin
                            tags={tagGroup.relatedTags}
                            name="tagGroups"
                            tagGroupIndex={index}
                          />
                        </Box>
                        <Box>
                          <Button
                            type="button"
                            variant="smallTransparent"
                            onClick={() => removeTagGroup(index)}
                          >
                            <Icon name="bin-2-alternate-interface-essential" size={18} />
                          </Button>
                        </Box>
                      </Flex>
                    ))}
                    <Box
                      sx={{
                        p: 3,
                        backgroundColor: 'containerBackgroundDarker',
                        borderRadius: themeRadii.medium,
                      }}
                    >
                      <FormElement>
                        <P>{t('Add a new tag group')}</P>
                      </FormElement>
                      <FormElement>
                        <Label htmlFor="simpleTags">{t('Name')}</Label>
                        <Input {...hookForm.register('newTagGroupName')} autoComplete="off" />
                        <FormErrorBadge error={hookForm.formState.errors.newTagGroupName} />
                      </FormElement>
                      <FormElement>
                        <Button
                          type="button"
                          variant="secondarySmall"
                          onClick={() => createTagGroup()}
                        >
                          {t('Create tag group')}
                        </Button>
                      </FormElement>
                    </Box>
                  </Grid>
                </FormElement>
                <FormElement>
                  <Label htmlFor="simpleTags">{t('Simple tags')}</Label>
                  <InputTagsAdmin tags={hookForm.getValues('simpleTags')} name="simpleTags" />
                </FormElement>
              </FormBody>
            </Box>
            <Box>
              <FormElement>
                <Button type="button" variant="secondary" onClick={() => addTemplate()}>
                  {t('Add default tag groups')}
                </Button>
              </FormElement>
              <Box sx={{ height: 20 }} />
              <FormElement>
                <Button
                  type="submit"
                  disabled={isSubmitting}
                  sx={{ position: 'relative', zIndex: 0 }}
                >
                  {t('Save')}
                </Button>
                <Button type="button" variant="secondary" onClick={onCancel}>
                  {t('Cancel')}
                </Button>
              </FormElement>
            </Box>
          </Grid>
        </form>
      </Box>
    </FormProvider>
  );
};
