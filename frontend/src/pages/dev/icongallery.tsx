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
import { NextPage } from 'next';
import fs from 'fs';
import { Box, Flex, Grid } from 'theme-ui';
import { clickableStyle, flexColumnJustifyCenterAlignCenter } from 'styles/shortcuts';
import { useCopyToClipboard } from 'usehooks-ts';
import { Icon } from 'components/shared/icon';
import { IconNames } from 'types/iconNames';
import { P } from 'components/tags/p';

type Props = {
  data: string[];
};
const IconGalleryPage: NextPage<Props> = ({ data }) => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [_value, copy] = useCopyToClipboard();

  return (
    <Box
      sx={{
        width: '100%',
        maxWidth: 800,
        margin: '0 auto',
        background: 'white',
        padding: 3,
        py: 5,
        color: 'black',
        fontSize: 1,
      }}
    >
      <Box sx={{ pb: 5, textAlign: 'center' }}>
        <P>
          All icons courtesy of{' '}
          <a href="https://streamlinehq.com" target="_blank" rel="noreferrer">
            Streamline HQ
          </a>
          . You are not allowed to use these icons unlicensed.
        </P>
        <P>Click on an icon to copy the icon name to the clipboard</P>
      </Box>
      <Grid sx={{ gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: 4 }}>
        {data.map((iconName) => (
          <Flex
            key={iconName}
            sx={{
              ...flexColumnJustifyCenterAlignCenter(2),
              textAlign: 'center',
              ...clickableStyle(),
            }}
            onClick={() => copy(iconName)}
          >
            <Icon name={iconName as IconNames} size={24} />
            <Box sx={{ color: 'lightgray' }}>{iconName}</Box>
          </Flex>
        ))}
      </Grid>
    </Box>
  );
};

export async function getServerSideProps() {
  const data = fs.readdirSync('./public/icons');

  return { props: { data: data.map((item) => item.replace('.svg', '')) } };
}

export default IconGalleryPage;
