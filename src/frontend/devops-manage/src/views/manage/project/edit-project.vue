<script setup lang="ts">
import {
  ref,
  onMounted,
} from 'vue';
import {
  useRoute,
  useRouter,
} from 'vue-router';
import http from '@/http/api';
import { useI18n } from 'vue-i18n';
import { InfoBox, Message, Popover } from 'bkui-vue';
import ProjectForm from '@/components/project-form.vue';
import {
  handleProjectManageNoPermission,
  RESOURCE_ACTION,
  RESOURCE_TYPE,
} from '@/utils/permission.js'

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
const statusDisabledTips = {
  1: t('新建项目申请审批中，暂不可修改'),
  4: t('更新项目信息审批中，暂不可修改'),
};
const currentDialect = ref();
const isDialectDialog = ref(false);

const fetchProjectData = async () => {
  isLoading.value = true;
  await http.requestProjectData({
    englishName: projectCode,
  }).then((res) => {
    projectData.value = res;
    currentDialect.value = res.pipelineDialect;
    if (projectData.value.centerId === '0') projectData.value.centerId = ''
    if (projectData.value.projectType === 0) projectData.value.projectType = ''
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

/**
 * 取消编辑项目
 */
const handleCancel = () => {
  const onConfirm = () => {
    isChange.value = false;
    router.push({
      path: 'show',
    });
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
    router.push({
      path: 'show',
    });
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

/**
 * 更新项目
 */
const handleUpdate = () => {
  if(currentDialect.value === 'CLASSIC' && projectData.value.pipelineDialect === 'CONSTRAINED'){
    isDialectDialog.value = true;
    return
  }
  updateConfirm()
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
}

const initProjectForm = (value) => {
  projectForm.value = value;
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
        @change="handleFormChange"
        @initProjectForm="initProjectForm">
      </project-form>
      <div class="btn-group">{{ projectData?.approvalStatus }}
        <Popover
          :content="statusDisabledTips[projectData?.approvalStatus]"
          :disabled="![1, 4].includes(projectData?.approvalStatus)"
          v-perm="{
            disablePermissionApi: [1, 3, 4].includes(projectData?.approvalStatus),
            hasPermission: [1, 3, 4].includes(projectData?.approvalStatus),
            permissionData: {
              projectId: projectCode,
              resourceType: RESOURCE_TYPE,
              resourceCode: projectCode,
              action: RESOURCE_ACTION.EDIT
            }
          }"
        >
          <span>
            <bk-button
              class="btn mr10"
              :disabled="[1, 4].includes(projectData?.approvalStatus)"
              theme="primary"
              :loading="btnLoading"
              @click="handleUpdate"
            >
              {{ t('提交更新') }}
            </bk-button>
          </span>
        </Popover>
        <bk-button
          class="btn"
          :loading="btnLoading"
          @click="handleCancel"
        >
          {{ t('取消') }}
        </bk-button>
      </div>
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
        {{ t('升级后，该项目对变量引用方式将有更严格的要求。') }}
      </div>
    </template>
  </bk-dialog>
</template>

<style lang="postcss" scoped>
  .edit-project-content {
    display: flex;
    flex-direction: column;
    height: 100%;
    padding: 16px 24px 24px;
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
      :deep(.bk-form-label) {
        font-size: 12px;
      }
      :deep(.bk-form-content) {
        max-width: 700px;
      }
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
    display: flex;
    align-items: center;
    width: 416px;
    height: 46px;
    padding: 0 16px;
    margin: 16px 0;
    background: #F5F6FA;
    border-radius: 2px;
    font-size: 14px;
    color: #63656E;
  }
</style>
