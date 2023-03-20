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

// eslint-disable-next-line @typescript-eslint/no-var-requires
const fs = require('fs');

const directory = './public/icons';
const filesUnclean = fs.readdirSync(directory);

// eslint-disable-next-line no-restricted-syntax
for (const file of filesUnclean) {
  if (file.endsWith('.SVG') || file.startsWith('streamlinehq-') || file.includes('')) {
    fs.renameSync(
      `${directory}/${file}`,
      `${directory}/${file.replace('.SVG', '.svg').replace('', '').replace('streamlinehq-', '')}`
    );
  }
}

const filesCleaned = fs.readdirSync(directory);

const names = filesCleaned
  .filter((item) => !/(^|\/)\.[^\/\.]/g.test(item))
  .filter((item) => item !== 'default.svg')
  .map((name) => `'${name.replace('.svg', '')}'`);

const tsDefinitions = `/* eslint-disable license-header/header */
export type IconNames = ${names.join(' | ')};`;

fs.writeFileSync('./src/types/iconNames.d.ts', tsDefinitions, (err) => {
  if (err) throw err;
});
