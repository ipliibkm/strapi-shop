'use strict';

module.exports = {
  routes: [
    {
      method: 'GET',
      path: '/storefront-forms/by-slug/:slug',
      handler: 'storefront-form.bySlug',
      config: { auth: false },
    },
  ],
};
