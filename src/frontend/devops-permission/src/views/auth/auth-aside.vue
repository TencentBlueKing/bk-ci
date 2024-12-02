<script setup>
import { ref, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { AngleDown, AngleRight } from 'bkui-vue/lib/icon';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const showMenu = ref(true);
const handleShowMenu = () => {
  showMenu.value = !showMenu.value;
};
const routeName = computed(() => route.name);
const curActive = ref('oauth');
const permissionMenus = computed(() => {
  return [
    { key: t('代码库授权'), value: 'codelib' },
    { key: t('流水线执行权限'), value: 'pipeline' },
    { key: t('部署节点授权'), value: 'node' },
  ]
})

const handleChangeMenu = (value) => {
  curActive.value = value;
  router.push({
    name: value
  })
}
onMounted(() => {
  if (routeName.value) curActive.value = routeName.value;
})
</script>

<template>
  <aside class="auth-aside">
    <div
      :class="{
        'menu-item': true,
        'active': 'oauth' === curActive
      }"
      @click="handleChangeMenu('oauth')"
    >
      <i class="permission-icon permission-icon-oauth" />
      <span>{{ t('我的OAUTH') }}</span>
    </div>
    <div class="menu-item-submenu">
      <div class="submenu-header" @click="handleShowMenu">
        <div>
          <i class="permission-icon permission-icon-oauth" />
          <span>{{ t('我的授权') }}</span>
        </div>
        <angle-down :class="{ 'angle-down is-show': !showMenu }" />
      </div>
      <ul :class="['submenu-list', {
        'show': !showMenu
      }]">
        <li
          v-for="menu in permissionMenus"
          :class="{
            'menu-item': true,
            'active': menu.value === curActive
          }"
          :key="menu.value"
          @click="handleChangeMenu(menu.value)"
        >
          <i class="default-icon" />
          {{ menu.key }}
        </li>
      </ul>
    </div>
  </aside>
</template>

<style lang="scss" scoped>
.auth-aside {
  width: 240px;
  color: #63656E;
  padding-top: 5px;
  background-color: #fff;
  box-shadow: 1px 0 0 0 #DCDEE5;
}
.menu-item {
  display: flex;
  align-items: center;
  width: 240px;
  height: 40px;
  font-size: 14px;
  padding-left: 25px;
  line-height: 40px;
  cursor: pointer;
  &.active {
    background: #E1ECFF;
    color: #3A84FF;
    .default-icon {
      background-color: #3A84FF !important;
    }
  }
  .permission-icon {
    margin-right: 10px;
  }
}
.menu-item-submenu {
  display: flex;
  flex-direction: column;
  font-size: 14px;
  cursor: pointer;
  .submenu-header {
    display: flex;
    justify-content: space-between;
    height: 40px !important;
    line-height: 40px !important;
    padding-left: 25px;
    padding-right: 12px;
  }
  .angle-down {
    &.is-show {
      transform: rotate(-90deg);
    }
  }
  .permission-icon {
    margin-right: 7px;
  }
  .submenu-list {
    &.show {
      height: 0;
      overflow: hidden;
    }
  }
  .default-icon {
    display: inline-block;
    width: 3px;
    height: 3px;
    background-color: #979BA5;
    border-radius: 50%;
    margin: 0 18px 0 5px;
  }
}
</style>