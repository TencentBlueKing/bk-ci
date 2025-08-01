<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import {
  ref,
} from 'vue';
import http from '@/http/api';
import { Message, InfoBox } from 'bkui-vue';
import ManageHeader from '@/components/manage-header.vue';
import ProjectForm from '@/components/project-form.vue';
import {
  useRouter,
} from 'vue-router';
const { t } = useI18n();
const router = useRouter();

const projectData = ref({
  projectName: '',
  englishName: '',
  description: '',
  projectType: '',
  logoAddr: '',
  bgId: 0,
  bgName: '',
  deptId: '',
  deptName: '',
  centerId: '',
  centerName: '',
  subjectScopes: [],
  secrecy: false,
  authSecrecy: 0,
  properties: {
    pipelineDialect: 'CLASSIC',
    loggingLineLimit: null
  }
});
const projectForm = ref(null);
const btnLoading = ref(false);
const handleConfirm = () => {
  projectForm.value?.validate().then(async () => {
    btnLoading.value = true;
    const result = await http.requestCreateProject({
      projectData: projectData.value,
    })
      .catch(() => false)
      .finally(() => {
        btnLoading.value = false;
      });
    if (result) {
      Message({
        theme: 'success',
        message: t('提交成功'),
      });
      router.push({
        path: `${projectData.value.englishName}/show`,
      });
    }
  })
};
const initProjectForm = (value) => {
  projectForm.value = value;
};

const handleCancel = () => {
  router.back();
};
</script>

<template>
  <section class="content-wrapper">
    <manage-header
      class="manage-header"
      :name="t('申请新建项目')"
    />
    <article class="apply-project-content">
      <section class="create-project-form">
        <project-form
          ref="projectForm"
          type="apply"
          :data="projectData"
          @initProjectForm="initProjectForm"
        >
      </project-form>
      <div class="btn-group">
        <bk-button
          class="btn mr10"
          theme="primary"
          :loading="btnLoading"
          @click="handleConfirm"
        >
          {{ t('提交') }}
        </bk-button>
        <bk-button
          class="btn"
          theme="default"
          :loading="btnLoading"
          @click="handleCancel"
        >
          {{ t('取消') }}
        </bk-button>
      </div>
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
    padding: 24px 24px 16px;
    height: calc(100% - 108px);
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
      width: 1200px;
      flex: 1;
      margin: 0 auto;
    }
    .btn-group {
      position: fixed;
      bottom: 0;
      left: 0;
      padding-left: 24px;
      width: 100%;
      height: 48px;
      line-height: 48px;
      background: #FAFBFD;
      box-shadow: 0 -1px 0 0 #DCDEE5;
    }
    .mr10 {
      margin-right: 10px;
    }
    .btn {
      width: 88px;
    }
  }
</style>
