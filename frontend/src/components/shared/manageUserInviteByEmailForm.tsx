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

import { FormGroup } from 'components/forms/formGroup';
import { FormElement } from 'components/forms/formElement';
import { Box, Flex, Input, Label } from 'theme-ui';
import { emailValidationPattern } from 'lib/validators';
import { FormErrorBadge } from 'components/forms/formErrorBadge';
import { Button, Select } from '@theme-ui/components';
import React, { useState } from 'react';
import { ModelsInvitationLink } from 'lib/api/lasius';
import { useTranslation } from 'next-i18next';
import { useForm } from 'react-hook-form';
import { ModalConfirm } from 'components/modal/modalConfirm';
import { Icon } from 'components/shared/icon';
import { useCopyToClipboard } from 'usehooks-ts';
import { SelectArrow } from 'components/forms/input/shared/selectArrow';
import { UserRoles } from 'dynamicTranslationStrings';
import { inviteOrganisationUser } from 'lib/api/lasius/organisations/organisations';
import { inviteProjectUser } from 'lib/api/lasius/projects/projects';
import { ModelsUserOrganisationRoleEnum, ModelsUserProjectRoleEnum } from 'lib/api/enums';

type Props = {
  organisation: string | undefined;
  project?: string;
  onSave: () => void;
};

type Form = {
  inviteMemberByEmailAddress: string;
  projectRole: ModelsUserProjectRoleEnum;
  organisationRole: ModelsUserOrganisationRoleEnum;
};

export const ManageUserInviteByEmailForm: React.FC<Props> = ({ onSave, organisation, project }) => {
  const { t } = useTranslation('common');
  const mode = project && organisation ? 'project' : 'organisation';
  const hookForm = useForm<Form>({
    mode: 'onChange',
    shouldUnregister: true,
    defaultValues: {
      inviteMemberByEmailAddress: '',
      projectRole: ModelsUserProjectRoleEnum.ProjectMember,
      organisationRole: ModelsUserOrganisationRoleEnum.OrganisationMember,
    },
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showCopyToClipboard, setShowCopyToClipboard] = useState(false);
  const [invitationLink, setInvitationLink] = useState<ModelsInvitationLink | null>(null);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [_value, copy] = useCopyToClipboard();

  const handleClipboardConfirm = () => {
    hookForm.reset();
    setShowCopyToClipboard(false);
  };

  const onSubmit = async () => {
    setIsSubmitting(true);
    const { inviteMemberByEmailAddress, projectRole, organisationRole } = hookForm.getValues();
    if (project && organisation) {
      const data = await inviteProjectUser(organisation, project, {
        email: inviteMemberByEmailAddress,
        role: projectRole,
      });
      setInvitationLink(data);
    } else if (organisation) {
      const data = await inviteOrganisationUser(organisation, {
        email: inviteMemberByEmailAddress,
        role: organisationRole,
      });
      setInvitationLink(data);
    }
    setIsSubmitting(false);
    setShowCopyToClipboard(true);
    onSave();
  };

  const registrationLink = (invitationId: string) => {
    const url = new URL(window.location.toString());
    return `${url.protocol}//${url.host}/join/${invitationId}`;
  };

  return (
    <form onSubmit={hookForm.handleSubmit(onSubmit)}>
      <FormGroup>
        <FormElement>
          <Label htmlFor="inviteMemberByEmailAddress">{t('Invite by E-Mail')}</Label>
          <Input
            {...hookForm.register('inviteMemberByEmailAddress', {
              required: true,
              pattern: emailValidationPattern,
            })}
            autoComplete="off"
          />
          <FormErrorBadge error={hookForm.formState.errors.inviteMemberByEmailAddress} />
        </FormElement>
        {mode === 'project' && (
          <FormElement>
            <Label htmlFor="projectRole">{t('Project role')}</Label>
            <Select
              arrow={<SelectArrow />}
              {...hookForm.register('projectRole', {
                required: true,
              })}
            >
              <option
                key={ModelsUserProjectRoleEnum.ProjectMember}
                value={ModelsUserProjectRoleEnum.ProjectMember}
              >
                {UserRoles[ModelsUserProjectRoleEnum.ProjectMember]}
              </option>
              <option
                key={ModelsUserProjectRoleEnum.ProjectAdministrator}
                value={ModelsUserProjectRoleEnum.ProjectAdministrator}
              >
                {UserRoles[ModelsUserProjectRoleEnum.ProjectAdministrator]}
              </option>
            </Select>
          </FormElement>
        )}
        {mode === 'organisation' && (
          <FormElement>
            <Label htmlFor="organisationRole">{t('Organisation role')}</Label>
            <Select
              arrow={<SelectArrow />}
              {...hookForm.register('organisationRole', {
                required: true,
              })}
            >
              <option
                key={ModelsUserOrganisationRoleEnum.OrganisationMember}
                value={ModelsUserOrganisationRoleEnum.OrganisationMember}
              >
                {UserRoles[ModelsUserOrganisationRoleEnum.OrganisationMember]}
              </option>
              <option
                key={ModelsUserOrganisationRoleEnum.OrganisationAdministrator}
                value={ModelsUserOrganisationRoleEnum.OrganisationAdministrator}
              >
                {UserRoles[ModelsUserOrganisationRoleEnum.OrganisationAdministrator]}
              </option>
            </Select>
          </FormElement>
        )}
        <FormElement>
          <Button type="submit" disabled={isSubmitting}>
            {t('Invite')}
          </Button>
        </FormElement>
      </FormGroup>
      {showCopyToClipboard && invitationLink && (
        <ModalConfirm onConfirm={handleClipboardConfirm}>
          <Box>
            {t(
              'Copy the link and send it to your colleague. If he or she does not have an account yet, one will be created when the invitation is accepted.'
            )}
          </Box>
          <Flex sx={{ gap: 3, py: 3 }}>
            <Box variant="styles.code">{registrationLink(invitationLink.id)}</Box>

            <Button
              variant="icon"
              aria-label={t('Copy to clipboard')}
              onClick={() => copy(registrationLink(invitationLink.id))}
            >
              <Icon name="copy-paste-interface-essential" size={16} />
            </Button>
          </Flex>
        </ModalConfirm>
      )}
    </form>
  );
};
