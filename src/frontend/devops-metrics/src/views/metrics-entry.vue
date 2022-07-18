<script setup lang="ts">
import {
  useRoute,
} from 'vue-router';
import { useI18n } from "vue-i18n";
const { t } = useI18n();
const route = useRoute()
const navList = [
  {
    name: t('Overview'),
    router: 'MetricsOverview',
    icon: 'metrics-overview',
  },
  {
    name: t('Fail analysis'),
    router: 'FailAnalysis',
    icon: 'metrics-fail-analysis',
  },
  {
    name: t('Plugin trend'),
    router: 'PluginRunAnalysis',
    icon: 'metrics-plugin-trend',
    meta: ['PluginFailAnalysis']
  },
];
</script>

<template>
  <article class="metric-home">
    <aside class="metric-nav">
      <h3 class="nav-title">{{ t('Metrics') }}</h3>
      <ul class="nav-list">
        <router-link
          v-for="nav in navList"
          :key="nav.name"
          :to="{ name: nav.router }"
          :class="{ 'nav-item': true, 'router-link-active': nav.meta && nav.meta.includes(String(route.name)) }"
        >
          <i :class="[nav.icon, 'metrics-icon']"></i>
          {{ nav.name }}
        </router-link>
      </ul>
    </aside>
    <main class="metric-main">
      <router-view></router-view>
    </main>
  </article>
</template>

<style lang="scss" scoped>
  .metric-home {
    width: 100%;
    height: 100vh;
    overflow: hidden;
    display: flex;
  }

  .metric-nav {
    width: 240px;
    background: #fff;
    border-right: 1px solid #dde4eb;
    font-size: 14px;

    .nav-title {
      height: 53px;
      line-height: 53px;
      border-bottom: 1px solid #dde4eb;
      padding: 0 17px;
      font-size: 16px;
    }

    .nav-list {
      margin-top: 8px;
      display: flex;
      flex-direction: column;

      .nav-item {
        position: relative;
        display: flex;
        align-items: center;
        padding: 0 23px;
        height: 40px;
        line-height: 40px;
        color: #63656e;

        .metrics-icon {
          font-size: 16px;
          margin-right: 8px;
        }
      }

      .router-link-active {
        background: #e1ecff;
        color: #3a84ff;

        &::before {
          content: '';
          position: absolute;
          left: 0;
          height: 40px;
          width: 1px;
          background: #4288fa;
        }
      }
    }
  }

  .metric-main {
    height: 100%;
    width: calc(100% - 240px);
  }
</style>
