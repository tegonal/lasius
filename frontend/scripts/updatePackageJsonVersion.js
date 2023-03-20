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

/*
 * This script is used to update the version in package.json. It takes the current git hash from the
 * branch it is built on and appends it to the version number previously defined in package.json.
 */
// eslint-disable-next-line @typescript-eslint/no-var-requires
const fs = require('fs');

fs.readFile('./package.json', (err, data) => {
  if (err) throw err;

  let packageJsonObj = JSON.parse(data);
  const { name } = packageJsonObj;
  const newVersion = `${process.argv[2] || 'development-build'}`;

  console.log(`Building ${name}@${newVersion} ...`);

  packageJsonObj.version = newVersion;
  packageJsonObj = JSON.stringify(packageJsonObj);

  fs.writeFile('./package.json', packageJsonObj, (err) => {
    if (err) throw err;
  });
});
