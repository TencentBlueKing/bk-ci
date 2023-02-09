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
const { t } = useI18n();
const router = useRouter();
const route = useRoute();

const { projectCode } = route.params;
const projectData = ref<any>({});
const isLoading = ref(false);
const isChange = ref(false);
const isToBeApproved = ref(false);
const btnLoading = ref(false);
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
  });
  isLoading.value = false;
};

/**
 * 取消编辑项目
 */
const handleCancel = () => {
  const onClosed = () => {
    isChange.value = false;
    router.push({
      path: 'show',
    });
  };
  if (isChange.value) {
    InfoBox({
      type: 'warning',
      title: t('确认离开当前页面吗?'),
      subTitle: t('离开将会丢失未保存的信息，建议保存后离开'),
      contentAlign: 'center',
      headerAlign: 'center',
      footerAlign: 'center',
      confirmText: t('留在此页'),
      cancelText: t('直接离开'),
      onConfirm: () => true,
      onClosed,
    });
  } else {
    onClosed();
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

const showNeedApprovedTips = () => {
  InfoBox({
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

const updateProject = async () => {
  btnLoading.value = true;
  const result = await http.requestUpdateProject({
    projectId: projectData.value.englishName,
    projectData: projectData.value,
  });
  btnLoading.value = false;
  if (result) {
    Message({
      theme: 'success',
      message: t('保存成功'),
    });
    router.push({
      path: 'show',
    });
  }
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

onMounted(() => {
  fetchProjectData();
});
</script>

<template>
  <bk-loading class="edit-project-content" :loading="isLoading">
    <section class="edit-project-form">
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
            :disabled="![1, 4].includes(projectData.approvalStatus)">
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
  </bk-loading>
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
