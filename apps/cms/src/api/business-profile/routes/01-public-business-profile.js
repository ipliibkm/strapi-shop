'use strict';

module.exports = {
  routes: [
    {
      method: 'GET',
      path: '/business-profile/public-contact',
      handler: 'business-profile.public',
      config: { auth: false },
    },
  ],
};
