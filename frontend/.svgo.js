/* eslint-disable license-header/header */

module.exports = {
  multipass: true,
  js2svg: {
    pretty: true,
    indent: 2,
  },
  plugins: [
    {
      name: 'convertColors',
      params: {
        currentColor: true,
      },
    },
    {
      name: 'collapseGroups',
    },
    {
      name: 'convertShapeToPath',
    },
    {
      name: 'mergePaths',
    },
    {
      name: 'convertPathData',
    },
    {
      name: 'convertTransform',
    },
    {
      name: 'cleanupListOfValues',
    },
    {
      name: 'removeUselessStrokeAndFill',
    },
    {
      name: 'removeUnusedNS',
    },
    {
      name: 'cleanupNumericValues',
    },
    {
      name: 'removeUnknownsAndDefaults',
    },
    {
      name: 'removeNonInheritableGroupAttrs',
    },
    {
      name: 'convertStyleToAttrs',
    },
    {
      name: 'cleanupAttrs',
    },
    {
      name: 'removeDoctype',
    },
    {
      name: 'removeXMLProcInst',
    },
    {
      name: 'removeComments',
    },
    {
      name: 'removeMetadata',
    },
    {
      name: 'removeTitle',
    },
    {
      name: 'removeDesc',
    },
    {
      name: 'removeUselessDefs',
    },
    {
      name: 'cleanupIDs',
    },
  ],
};
