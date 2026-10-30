'use strict';

module.exports = {
  routes: [
    {
      method: 'GET',
      path: '/storefront-pages/enabled',
      handler: 'storefront-page.enabled',
      config: { auth: false },
    },
    {
      method: 'GET',
      path: '/storefront-pages/by-slug/:slug',
      handler: 'storefront-page.bySlug',
      config: { auth: false },
    },
  ],
};
