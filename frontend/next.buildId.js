/* eslint-disable license-header/header */

// eslint-disable-next-line @typescript-eslint/no-var-requires
const packageJson = require('./package.json');

module.exports = {
  generateBuildId: async () => {
    const buildId = `${packageJson.version}`;
    console.info(`Current build: ${buildId}`);
    return buildId;
  },
  generateBuildIdSync: () => {
    const buildId = `${packageJson.version}`;
    console.info(`Current build: ${buildId}`);
    return buildId;
  },
};
