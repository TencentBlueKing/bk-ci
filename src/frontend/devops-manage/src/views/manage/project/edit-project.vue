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
const isLoading = ref(false);
const isChange = ref(false);
const isToBeApproved = ref(false);
const btnLoading = ref(false);
const hasPermission = ref(true)
const statusDisabledTips = {
  1: t('新建项目申请审批中，暂不可修改'),
  4: t('更新项目信息审批中，暂不可修改'),
};

const fetchProjectData = async () => {
  isLoading.value = true;
  await http.requestProjectData({
    englishName: projectCode,
  }).then((res) => {
    projectData.value = res;
    if (projectData.value.centerId === '0') projectData.value.centerId = ''
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

const handleApprovedChange = (val: boolean) => {
  isToBeApproved.value = val;
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
    subTitle: t('修改了“项目性质”或“项目最大可授权人员范围”，需要经过审核'),
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
const handleUpdate = async () => {
  if (isToBeApproved.value) {
    showNeedApprovedTips();
  } else {
    updateProject();
  };
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
        @approvedChange="handleApprovedChange">
        <bk-form-item>
          <Popover
            :content="statusDisabledTips[projectData.approvalStatus]"
            :disabled="![1, 4].includes(projectData.approvalStatus)"
            v-perm="{
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
                :disabled="[1, 4].includes(projectData.approvalStatus)"
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
        </bk-form-item>
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
</template>

<style lang="postcss" scoped>
  .edit-project-content {
    display: flex;
    flex-direction: column;
    height: 100%;
    overflow: auto;
    margin: 16px 24px 24px;
    background-color: #fff;
    box-shadow: 0 2px 2px 0 rgb(0 0 0 / 15%);
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
      padding: 32px 48px;
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
</style>
