<script setup lang="ts">

import { useI18n } from 'vue-i18n';
const { t } = useI18n();
</script>

<template>
  <section class="sub-header clearfix">
    <div class="sub-header-left clearfix">
      <span class="default-title">
        {{ t('项目管理') }}
      </span>
    </div>
  </section>
  <section class="project-list-content">
    项目列表
  </section>
</template>

<style lang="postcss" scoped>
  .sub-header {
    width: 100%;
    height: 60px;
    padding: 0 30px;
    border-bottom: 1px solid #dde4eb;
    box-shadow: 0 2px 5px rgba(0, 0, 0, .03);
    display: flex;
    background: #fff;
    &-left {
      display: flex;
      flex: 1;
      align-items: center;
      max-width: 124px;
      height: 100%;
    }
    .default-title {
      letter-spacing: .5px;
      font-size: 16px;
      color: #333948;
    }
  }
  .project-list-content {
    padding: 20px 30px 0 30px;
    height: calc(100% - 60px);
  }
</style>
