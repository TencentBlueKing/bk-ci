
import BkPermission from './components/permission-main.vue';
import PermissionDirective from './directive/authority-directive';
import { AuthorityDirectiveV3 } from './directive/authority-directive';
import handleNoPermission from './function/permission';
import { handleNoPermissionV3 } from './function/permission';
import { loadI18nMessages } from './utils/locale';

function install (Vue, opts = {}) {
  loadI18nMessages(opts.i18n)
  Vue.component('bk-permission', BkPermission)
}

BkPermission.install = install

export {
  BkPermission,
  PermissionDirective,
  handleNoPermission,
  handleNoPermissionV3,
  AuthorityDirectiveV3,
};
