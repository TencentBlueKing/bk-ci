<script setup lang="ts">
import ManageHeader from '@/components/manage-header.vue';
import ProjectForm from '@/components/project-form.vue';
import http from '@/http/api';
import { InfoBox, Message } from 'bkui-vue';
import {
  ref,
} from 'vue';
import { useI18n } from 'vue-i18n';
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
  projectTypes: '',
  logoAddr: '',
  productId: '',
  productName: '',
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
  }
});
const projectForm = ref(null);
const btnLoading = ref(false);
const handleConfirm = () => {
  const confirmFn = async () => {
    infoBoxInstance.value.hide();
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
  };
  const infoBoxInstance = ref();
  projectForm.value?.validate().then(() => {
    infoBoxInstance.value = InfoBox({
      isShow: true,
      infoType: 'warning',
      title: t('创建项目需你的上级审批，确认提交吗'),
      contentAlign: 'center',
      headerAlign: 'center',
      footerAlign: 'center',
      confirmText: t('确认提交'),
      cancelText: t('取消'),
      onConfirm: confirmFn,
      onClosed: () => true,
    });
  });
};
const initProjectForm = (value) => {
  projectForm.value = value;
};

const productIdChange = ({ id, list }) => {
  projectData.value.productName = list.find(i => i.ProductId === id)?.ProductName;
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
          @productIdChange="productIdChange"
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
    }
    .btn-group {
      display: flex;
      margin: 24px 0;
    }
    .mr10 {
      margin-right: 10px;
    }
    .btn {
      width: 88px;
    }
  }
</style>
