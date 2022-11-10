<script setup lang="ts">
import ProjectForm from '@/components/project-form.vue';
import { useI18n } from 'vue-i18n';
import {
  useRouter,
} from 'vue-router';
const { t } = useI18n();
const router = useRouter();

const handleCancel = () => {
  router.back();
};

</script>

<template>
  <article class="edit-project-content">
    <section class="edit-project-form">
      <project-form class="edit-form">
        <bk-form-item>
          <bk-button class="btn mr10" theme="primary">{{ t('提交更新') }}</bk-button>
          <bk-button class="btn" theme="default" @click="handleCancel">{{ t('取消') }}</bk-button>
        </bk-form-item>
      </project-form>
    </section>
  </article>
</template>

<style lang="postcss" scoped>
  .edit-project-content {
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
    .edit-project-form {
      width: 100%;
      flex: 1;
      margin: 0 auto;
      background-color: #fff;
      padding: 32px 48px;
      box-shadow: 0 2px 2px 0 rgba(0,0,0,0.15);
      :deep(.bk-form-label) {
        font-size: 12px;
      }
      :deep(.bk-form-content) {
        max-width: 500px;
      }
    }
    .mr10 {
      margin-right: 10px;
    }
    .btn {
      width: 88px;
    }
  }
</style>
