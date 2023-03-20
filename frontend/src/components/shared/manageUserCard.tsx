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

import { CardSmall } from 'components/shared/cardSmall';
import { Box, Button, Flex } from 'theme-ui';
import { flexColumnJustifyCenterAlignCenter, fullWidthHeight } from 'styles/shortcuts';
import { AvatarUser } from 'components/shared/avatar/avatarUser';
import React, { useRef, useState } from 'react';
import { ModelsUserStub } from 'lib/api/lasius';
import { useOnClickOutside } from 'usehooks-ts';
import { m } from 'framer-motion';
import { themeRadii } from 'styles/theme/radii';
import { Icon } from 'components/shared/icon';
import { ModalConfirm } from 'components/modal/modalConfirm';
import { useTranslation } from 'next-i18next';
import { useProfile } from 'lib/api/hooks/useProfile';

const ContextCard: React.FC<{
  showContext: boolean;
  skipRender: boolean;
  children: React.ReactNode;
}> = ({ showContext, children, skipRender }) => {
  const variants = {
    visible: { y: -48 },
    hidden: { y: 0 },
  };
  if (skipRender) {
    return <>{children}</>;
  }
  return (
    <m.div
      initial="hidden"
      animate={showContext ? 'visible' : 'hidden'}
      variants={variants}
      whileHover={showContext ? {} : { y: -4 }}
    >
      {children}
    </m.div>
  );
};

type Props = {
  user: ModelsUserStub;
  onRemove: () => void;
  canRemove?: boolean;
};

export const UserCard: React.FC<Props> = ({ canRemove = false, onRemove, user }) => {
  const [showContext, setShowContext] = useState<boolean>(false);
  const [showConfirmationDialog, setShowConfirmationDialog] = useState<boolean>(false);
  const ref = useRef(null);
  const { t } = useTranslation('common');
  const { userId } = useProfile();

  useOnClickOutside(ref, () => setShowContext(false));

  const handleConfirm = () => {
    setShowContext(false);
    setShowConfirmationDialog(false);
    onRemove();
  };

  const handleCancel = () => {
    setShowContext(false);
    setShowConfirmationDialog(false);
  };

  const isCurrentUser = () => {
    return user.id === userId;
  };

  return (
    <Box
      ref={ref}
      sx={{
        label: 'UserCard',
        position: 'relative',
        overflow: 'hidden',
        borderRadius: themeRadii.medium,
      }}
      key={user.id}
    >
      <ContextCard skipRender={!canRemove} showContext={showContext}>
        <CardSmall onClick={() => setShowContext(!showContext)} borderRadius={themeRadii.none}>
          <Flex sx={{ ...flexColumnJustifyCenterAlignCenter(), pt: 2 }}>
            <AvatarUser firstName={user.firstName} lastName={user.lastName} size={64} />
          </Flex>
          <Box sx={{ fontSize: 1 }}>
            {user.firstName} {user.lastName}
          </Box>
        </CardSmall>
        {canRemove && (
          <Box
            sx={{
              position: 'absolute',
              right: 0,
              bottom: -48,
              left: 0,
              height: 48,
              background: isCurrentUser() ? 'greenGradient' : 'redGradient',
            }}
          >
            <Flex sx={{ ...flexColumnJustifyCenterAlignCenter(3), ...fullWidthHeight() }}>
              {isCurrentUser() ? (
                <>{t('You')}</>
              ) : (
                <Button variant="contextIcon" onClick={() => setShowConfirmationDialog(true)}>
                  <Icon name="bin-2-alternate-interface-essential" size={24} />
                </Button>
              )}
            </Flex>
          </Box>
        )}
      </ContextCard>
      {showConfirmationDialog && (
        <ModalConfirm
          text={{ action: t('Are you sure you want to remove this member?') }}
          onConfirm={handleConfirm}
          onCancel={handleCancel}
        />
      )}
    </Box>
  );
};
