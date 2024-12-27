<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { useRouter, useRoute } from 'vue-router';

const { t } = useI18n();
const router = useRouter();
const route = useRoute();

const menuGroups = [
{
    theme: t('个人事务'),
    items: [
      {
        name: t('我的权限'),
        code: 'my-permission',
        icon: 'permission-icon-wodequanxian',
      },
      {
        name: t('申请权限'),
        code: 'apply',
        icon: 'permission-icon-quanxianshenqing',
      },
      {
        name: t('我的申请'),
        code: 'my-apply',
        icon: 'permission-icon-wodeshenqing',
      },
      {
        name: t('我的审批'),
        code: 'my-approval',
        icon: 'permission-icon-wodeshenpi',
      },
      {
        name: t('我的交接'),
        code: 'my-handover',
        icon: 'permission-icon-handover'
      }
    ]
  },
  {
    theme: t('管理事务'),
    items: [
      {
        name: t('我的项目'),
        code: 'my-project',
        icon: 'permission-icon-wodexiangmu',
      }
    ]
  }
]

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
        v-for="(group, groupIndex) in menuGroups"
        :key="groupIndex"
      >
        <div class="menu-theme">{{ group.theme }}</div>
        <div
          v-for="(menu, index) in group.items"
          :key="index"
          @click="handleChangeMenu(menu)"
          :class="{
            'menu-item': true,
            'active': route.name === menu.code,
            'item-border': menu.code === 'apply'
          }"
        >
          <bk-badge
            position="top-right"
            theme="danger"
            dot
            :visible="true"
            style="margin: 0 18px 0 22px"
          >
            <i class="permission-icon" :class="menu.icon"></i>
          </bk-badge>
          <span>{{ menu.name }}</span>
        </div>
      </div>
    </section>

  </article>
</template>

<style lang="postcss" scoped>
  .permission-aside{
    width: 240px;
    height: 100%;
    background-color: #fff;
    box-shadow: 1px 0 0 0 #DCDEE5;
    z-index: 900;
  }
  .menu-list {
    margin-top: 8px;
    font-size: 14px;
    .menu-item {
      display: flex;
      align-items: center;
      width: 240px;
      height: 40px;
      margin-top: 4px;
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
    .menu-theme {
      height: 40px;
      line-height: 40px;
      color: #979BA5;
      margin: 8px 0 0 22px;
    }
    .item-border {
      border-bottom: 1px solid #e4e4e4;
    }
  }
</style>
