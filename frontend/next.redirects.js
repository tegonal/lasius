/* eslint-disable license-header/header */

module.exports = {
  redirects: async () => {
    return [
      {
        source: '/',
        has: [
          {
            type: 'header',
            key: 'User-Agent',
            value: '(.*Trident.*)',
          },
        ],
        permanent: false,
        destination: '/html/index-ie.html',
      },
    ];
  },
};
