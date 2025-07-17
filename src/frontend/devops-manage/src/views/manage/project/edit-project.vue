<script setup lang="ts">
import {
  RESOURCE_ACTION,
  handleProjectManageNoPermission,
} from '@/utils/permission.js';
import { InfoBox, Message } from 'bkui-vue';
import {
  onMounted,
  ref,
} from 'vue';
import {
  useRoute,
  useRouter,
} from 'vue-router';
import http from '@/http/api';
import { useI18n } from 'vue-i18n';
import ProjectForm from '@/components/project-form.vue';


const { t } = useI18n();
const router = useRouter();
const route = useRoute();

const { projectCode } = route.params;
const projectData = ref<any>({});
const projectForm = ref(null);
const isLoading = ref(false);
const isChange = ref(false);
const btnLoading = ref(false);
const hasPermission = ref(true)

const currentDialect = ref();
const isDialectDialog = ref(false);
let initdata;
const fetchProjectData = async () => {
  isLoading.value = true;
  await http.requestProjectData({
    englishName: projectCode,
  }).then((res) => {
    projectData.value = {
      ...res,
      properties: {
        pipelineDialect: 'CLASSIC',
        loggingLineLimit: null,
        ...res.properties,
      },
    };
    currentDialect.value = res.properties?.pipelineDialect || 'CLASSIC';
    if (projectData.value.centerId === '0') projectData.value.centerId = ''
    if (projectData.value.projectType === 0) projectData.value.projectType = ''
    initdata = JSON.stringify(projectData.value)
  }).catch((err) => {
    if (err.code === 403) {
      hasPermission.value = false
    } else {
      Message({
        theme: 'error',
        message: err.message || err,
      })
    }
  });
  isLoading.value = false;
};

const goShow = () => {
  router.push({
    path: 'show',
  });
}

/**
 * 取消编辑项目
 */
const handleCancel = () => {
  const onConfirm = () => {
    isChange.value = false;
    goShow()
  };
  if (isChange.value) {
    InfoBox({
      title: t('确认离开当前页?'),
      subTitle: t('离开将会导致未保存信息丢失'),
      contentAlign: 'center',
      headerAlign: 'center',
      footerAlign: 'center',
      confirmText: t('离开'),
      cancelText: t('取消'),
      onConfirm,
      onClosed: () => true,
    });
  } else {
    onConfirm();
  };
};

/**
 * 表单数据变更
 */
const handleFormChange = (val: boolean) => {
  isChange.value = val;
};

const infoBoxInstance = ref();

const updateProject = async () => {
  infoBoxInstance.value?.hide()
  btnLoading.value = true;
  const result = await http
    .requestUpdateProject({
      projectId: projectData.value.englishName,
      projectData: projectData.value,
    })
    .catch((err) => {
      if (err.code === 403) {
        handleProjectManageNoPermission({
          action: RESOURCE_ACTION.EDIT,
          projectId: projectCode,
          resourceCode: projectCode,
        })
      }
      Message({
        theme: 'error',
        message: err.message || err,
      })
    })
    .finally(() => {
      btnLoading.value = false;
    });
  if (result) {
    Message({
      theme: 'success',
      message: t('保存成功'),
    });
    goShow()
  }
  return Promise.resolve(false)
};
const showNeedApprovedTips = () => {
  infoBoxInstance.value = InfoBox({
    isShow: true,
    infoType: 'warning',
    title: t('本次提交需要审核'),
    subTitle: t('修改了“项目性质”或“项目最大可授权人员范围”，需要你的上级审核'),
    contentAlign: 'center',
    headerAlign: 'center',
    footerAlign: 'center',
    confirmText: t('确认提交'),
    cancelText: t('取消'),
    onConfirm: updateProject,
    onClosed: () => true,
  });
};

const undateMetadata = async (params) => {
  try {
    btnLoading.value = true;
    const res = await http.batchUpdateMetadata(projectData.value.englishName, params)
    if (res) {
      Message({
        theme: 'success',
        message: t('保存成功'),
      });
      goShow()
    }
  } catch (err) {
    console.log(err);
  } finally {
    btnLoading.value = false;
  }
}

/**
 * 更新项目
 */
const handleUpdate = (panel, params) => {
  if (panel) {
    undateMetadata(params)
  } else {
    if(currentDialect.value === 'CLASSIC' && projectData.value.properties.pipelineDialect === 'CONSTRAINED'){
      isDialectDialog.value = true;
      return
    }
    updateConfirm()
  }
};

const updateConfirm = async () => {
  projectForm.value?.validate().then(async () => {
    await updateProject();
  })
}

const handleConfirm = () => {
  isDialectDialog.value = false;
  updateConfirm()
}

const handleClosed = () => {
  isDialectDialog.value = false;
  projectData.value.properties.pipelineDialect = 'CLASSIC'
}

const initProjectForm = (value) => {
  projectForm.value = value;
};

const initProjectData = (value) => {
  Object.assign(projectData.value, value);
};

const handleNoPermission = () => {
  handleProjectManageNoPermission({
    action: RESOURCE_ACTION.VIEW,
    projectId: projectCode,
    resourceCode: projectCode,
  })
};

onMounted(() => {
  fetchProjectData();
});
</script>

<template>
  <bk-loading class="edit-project-content" :loading="isLoading">
    <section class="edit-project-form" v-if="hasPermission">
      <project-form
        v-if="!isLoading"
        class="edit-form"
        type="edit"
        :is-change="isChange"
        :data="projectData"
        :btnLoading="btnLoading"
        @change="handleFormChange"
        @initProjectForm="initProjectForm"
        @handleCancel="handleCancel"
        @handleUpdate="handleUpdate"
        @initProjectData="initProjectData"
      >
      </project-form>
    </section>
    <bk-exception
      v-else
      class="edit-project-form"
      type="403"
      :title="t('无编辑权限')"
      :description="t('你没有项目的编辑权限，请先申请', [projectCode])"
    >
      <bk-button theme="primary" @click="handleNoPermission">
        {{ t('去申请') }}
      </bk-button>
    </bk-exception>
  </bk-loading>
  <bk-dialog
    :width="480"
    header-align="center"
    footer-align="center"
    :is-show="isDialectDialog"
    @confirm="handleConfirm"
    @closed="handleClosed"
  >
    <template #header>
      <img src="@/css/svg/warninfo.svg" class="manage-icon-tips">
      <h2 class="dialog-header"> {{ t('确定升级变量语法风格为制约风格？')}}</h2>
    </template>
    <template #default>
      <div class="project">
        {{ t('项目：') }} <span class="project-name">{{ projectData.projectName }}</span>
      </div>
      <div class="description">
        {{ t('升级后，该项目下的流水线引用变量时仅支持X模式。') }}
        <i18n-t keypath="升级有可能导致存量流水线X，请谨慎操作。" tag="div">
          <span class="warn-tip">{{ t('运行失败') }}</span>
        </i18n-t>
      </div>
    </template>
  </bk-dialog>
</template>

<style lang="postcss" scoped>
  .edit-project-content {
    display: flex;
    flex-direction: column;
    position: relative;
    height: calc(100% - 108px);
    padding: 16px 24px 16px;
    overflow: auto;
    box-sizing: border-box;
   
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
      width: 1200px;
      height: 100%;
      background-color: #FFF;
      flex: 1;
      margin: 0 auto;
      :deep(.bk-form-label) {
        font-size: 12px;
      }
      :deep(.bk-form-content) {
        max-width: 700px;
      }
    }
    .mr10 {
      margin-right: 10px;
    }
    .btn {
      width: 88px;
    }
  }
  .manage-icon-tips {
    width: 42px;
    height: 42px;
  }
  .dialog-header {
    font-family: MicrosoftYaHei;
    font-size: 20px;
    color: #313238;
    letter-spacing: 0;
  }
  .progect {
    font-size: 14px;
    color: #63656E;
    .project-name {
      color: #313238;
      font-weight: bold;
    }
  }
  .description {
    width: 416px;
    height: 58x;
    padding: 6px 16px;
    margin: 16px 0;
    background: #F5F6FA;
    border-radius: 2px;
    font-size: 14px;
    color: #63656E;

    .warn-tip {
      font-weight: 700;
      color: red;
    }
  }
</style>
