
import BkPermission from './components/permission-main.vue';
import PermissionDirective from './directive/authority-directive';
import handleNoPermission from './function/permission';
import { loadI18nMessages } from './utils/locale'
console.log(233);
function install (Vue, opts = {}) {
  loadI18nMessages(opts.i18n)
  Vue.component('bk-permission', BkPermission)
}

BkPermission.install = install

export {
  BkPermission,
  PermissionDirective,
  handleNoPermission,
};
