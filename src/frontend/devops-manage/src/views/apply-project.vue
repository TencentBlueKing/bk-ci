<script setup lang="ts">
import ManageHeader from '@/components/manage-header.vue';
import ProjectForm from '@/components/project-form.vue';
import { useI18n } from 'vue-i18n';
const { t } = useI18n();
</script>

<template>
  <section class="content-wrapper">
    <manage-header
      class="manage-header"
      name="申请新建项目"
    />
    <article class="apply-project-content">
      <section class="create-project-form">
        <project-form>
          <bk-form-item>
            <bk-button class="btn mr10" theme="primary">{{ t('提交') }}</bk-button>
            <bk-button class="btn" theme="default">{{ t('取消') }}</bk-button>
          </bk-form-item>
        </project-form>
      </section>
    </article>
  </section>
</template>

<style lang="postcss" scoped>
  .manage-header {
    flex-direction: initial;
  }
  .content-wrapper {
    display: flex;
    flex-direction: column;
    height: 100%;
  }
  .apply-project-content {
    display: flex;
    flex-direction: column;
    padding: 24px;
    height: 100%;
    overflow: auto;
    &::-webkit-scrollbar-thumb {
      background-color: #c4c6cc !important;
      border-radius: 5px !important;
      &:hover {
        background-color: #979ba5 !important;
      }
    }
    &::-webkit-scrollbar {
      width: 8px !important;
      height: 8px !important;
    }
    .create-project-form {
      width: 1000px;
      flex: 1;
      margin: 0 auto;
      background-color: #fff;
      padding: 32px 120px 32px 80px;
      box-shadow: 0 2px 2px 0 rgba(0,0,0,0.15);
    }
    .mr10 {
      margin-right: 10px;
    }
    .btn {
      width: 88px;
    }
  }
</style>
