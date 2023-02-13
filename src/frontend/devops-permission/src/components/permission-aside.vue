<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { useRouter, useRoute } from 'vue-router';

const { t } = useI18n();
const router = useRouter();
const route = useRoute();

const menuList = [
  {
    name: t('我的权限'),
    code: 'my-permission',
    icon: '',
  },
  {
    name: t('我的申请'),
    code: 'my-apply',
    icon: '',
  },
  {
    name: t('我的审批'),
    code: 'my-approval',
    icon: '',
  },
  {
    name: t('申请权限'),
    code: 'apply',
    icon: '',
  },
];

const handleChangeMenu = (menu: any) => {
  router.push({
    name: menu.code,
  });
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
          'active': route.name === menu.code
        }"
      >
        <bk-badge
          position="top-right"
          theme="danger"
          dot
          :visible="true"
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
