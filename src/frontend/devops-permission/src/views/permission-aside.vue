<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { ref } from 'vue';
const { t } = useI18n();

const menuList = [
  {
    name: t('我的权限'),
    icon: '',
  },
  {
    name: t('我的申请'),
    icon: '',
  },
  {
    name: t('我的审批'),
    icon: '',
  },
  {
    name: t('申请权限'),
    icon: '',
  },
];
const activeMenu = ref(t('我的权限'));

const handleChangeMenu = (menu: any) => {
  activeMenu.value = menu.name;
};
</script>

<template>
  <article class="permission-aside">
    <section class="menu-list">
      <div
        v-for="(menu, index) in menuList"
        :key="index"
        @click="handleChangeMenu(menu)"
        :class="{
          'menu-item': true,
          'active': activeMenu === menu.name
        }"
      >
        <bk-badge
          position="top-right"
          theme="danger"
          dot
          :visible="false"
          style="margin: 0 18px 0 25px"
        >
          <i class="permission-icon permission-icon-app-store-fill"></i>
        </bk-badge>
        <span>{{ menu.name }}</span>
      </div>
    </section>

  </article>
</template>

<style lang="postcss" scoped>
  .permission-aside{
    width: 240px;
    height: 100%;
    background-color: #fff;
  }
  .menu-list {
    margin-top: 8px;
    font-size: 14px;
    .menu-item {
      display: flex;
      align-items: center;
      width: 240px;
      height: 40px;
      cursor: pointer;
      &:hover {
        color: #3A84FF;
        background-color: #E1ECFF;
      }
    }
    .active {
      color: #3A84FF;
      background-color: #E1ECFF;
    }
  }
</style>
