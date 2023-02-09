<script setup lang="ts">
import {
  ref,
  watch,
} from 'vue';
import { useI18n } from 'vue-i18n';
import http from '@/http/api';
import {
  useRoute,
  useRouter,
} from 'vue-router';
import { Message, InfoBox, Popover } from 'bkui-vue';

import { computed, onMounted } from '@vue/runtime-core';
const { t } = useI18n();
const router = useRouter();
const route = useRoute();

const { projectCode } = route.params;
const projectData = ref<any>({});
const projectDiffData = ref<any>({});
const isLoading = ref(false);
const userName = ref('');
const showStatusTips = computed(() => [1, 3, 4, 6].includes(projectData.value.approvalStatus));
const fetchProjectData = async () => {
  isLoading.value = true;
  await http.requestProjectData({
    englishName: projectCode,
  }).then((res) => {
    projectData.value = res;
  });
  isLoading.value = false;
};

const fieldMap = [
  {
    current: 'projectName',
    after: 'afterProjectName',
  },
  {
    current: 'description',
    after: 'afterDescription',
  },
  {
    current: 'authSecrecy',
    after: 'afterAuthSecrecy',
  },
  {
    current: 'logoAddr',
    after: 'afterLogoAddr',
  },
]

const fetchDiffProjectData = () => {
  http.requestDiffProjectData({
    englishName: projectCode,
  }).then((res) => {
    projectDiffData.value = res;
    
    fieldMap.forEach(field => {
      if (projectData.value[field.current] !== projectDiffData.value[field.after]) {
        projectData.value[field.after] = projectDiffData.value[field.after];
      }
    });

    if (projectData.value?.subjectScopes.length !== projectDiffData.value?.afterSubjectScopes.length) {
      projectData.value['afterSubjectScopes'] = projectDiffData.value.afterSubjectScopes
    } else {
      const subjectScopesIdMap = projectData.value.subjectScopes.map(i => i.id);
      let isChange = false;
      subjectScopesIdMap.forEach(id => {
        isChange = projectDiffData.value.afterSubjectScopes.some(scopes => scopes.id !== id);
      });
      if (isChange) {
        projectData.value['afterSubjectScopes'] = projectDiffData.value.afterSubjectScopes
      }
    }
  });
};

const getUserInfo = () => {
  http.getUser().then(res => {
    userName.value = res.username;
  });
};

const handleEdit = () => {
  router.push({
    path: 'edit',
  });
};

const handleToApprovalDetails = () => {
  window.open(`/console/permission/${projectData.value.englishName}/approval`, '_blank')
};

/**
 * 取消创建项目
 */
const handleCancelCreation = () => {
  const onConfirm = async () => {
    const result = await http.cancelCreateProject({
      projectId: projectData.value.project_id,
    });
    if (result) {
      Message({
        theme: 'success',
        message: t('取消创建成功'),
      });
      const { origin } = window.location;
      window.location.href = `${origin}/console/pm`;
    }
  };

  InfoBox({
    type: 'warning',
    title: t('确定取消创建项目'),
    contentAlign: 'center',
    headerAlign: 'center',
    footerAlign: 'center',
    onConfirm,
  });
};

const statusDisabledTips = {
  1: t('新建项目申请审批中，暂不可修改'),
  4: t('更新项目信息审批中，暂不可修改'),
};

const tipsStatusMap = {
  1: {
    type: 'success',
    message: t('新建项目申请目前正在审批中，可前往查看'),
  },
  2: {
    type: 'error',
    message: t('新建项目申请被拒绝'),
  },
  3: {
    type: 'success',
    message: t('新建项目申请已取消'),
  },
  4: {
    type: 'info',
    message: t('更新项目信息目前正在审批中，可前往查看'),
  },
  5: {
    type: 'success',
    message: t('更新项目信息审批被拒绝'),
  },
};

watch(() => projectData.value.approvalStatus, (status) => {
  if (status === 4) fetchDiffProjectData();
}, {
  deep: true,
});

onMounted(async () => {
  await fetchProjectData();
});
</script>

<template>
  <bk-loading class="content-wrapper" :loading="isLoading">
    <article class="project-info-content" v-if="!isLoading">
      <bk-alert :theme="tipsStatusMap[projectData.approvalStatus].type" closable v-if="showStatusTips">
        <template #title>
          {{ tipsStatusMap[projectData.approvalStatus].message || '--' }}
          <a class="approval-details" @click="handleToApprovalDetails">{{ t('审批详情') }}</a>
        </template>
      </bk-alert>
      <section class="content-main">
        <bk-form class="detail-content-form" :label-width="160">
          <bk-form-item :label="t('项目名称')" property="projectName">
            <div class="project-name">
              <img v-if="projectData.logoAddr" class="project-logo" :src="projectData.logoAddr" alt="">
              <span class="item-value">{{ projectData.projectName }}</span>
            </div>
            <div class="diff-content" v-if="projectData.afterLogoAddr || projectData.afterProjectName">
              <p class="update-title">{{ t('本次更新：') }}</p>
              <div>
                <img class="project-logo" :src="projectData.afterLogoAddr" alt="">
                <span class="item-value">{{ projectData.afterProjectName }}</span>
              </div>
            </div>
          </bk-form-item>
          <bk-form-item :label="t('项目ID')" property="englishName">
            <span class="item-value">{{ projectData.englishName }}</span>
          </bk-form-item>
          <bk-form-item :label="t('项目描述')" property="description">
            <span class="item-value">{{ projectData.description }}</span>
            <div class="diff-content" v-if="projectData.afterDescription">
              <p class="update-title">{{ t('本次更新：') }}</p>
              <div>{{ projectData.afterDescription }}</div>
            </div>
          </bk-form-item>
          <bk-form-item :label="t('项目所属组织')" property="bg">
            <span>{{ projectData.bgName }} - {{ projectData.deptName }} - {{ projectData.centerName }}</span>
          </bk-form-item>
          <bk-form-item :label="t('项目性质')" property="authSecrecy">
            <span class="item-value">{{ projectData.authSecrecy ? t('保密项目') : t('私有项目') }}</span>
            <div class="diff-content" v-if="projectData.afterAuthSecrecy">
              <p class="update-title">
                {{ t('本次更新：') }}
                <span class="inApproval">{{ t('(审批中)') }}</span>
              </p>
              <div>{{ projectData.afterAuthSecrecy ? t('保密项目') : t('私有项目') }}</div>
            </div>
          </bk-form-item>
          <bk-form-item :label="t('项目最大可授权人员范围')" property="subjectScopes">
            <span class="item-value">
              <bk-tag
                v-for="(subjectScope, index) in projectData.subjectScopes"
                :key="index"
              >
                {{ subjectScope.name }}
              </bk-tag>
            </span>
            <div class="diff-content scopes-diff" v-if="projectData.afterSubjectScopes">
              <p class="update-title">
                {{ t('本次更新：') }}
                <span class="inApproval">{{ t('(审批中)') }}</span>
              </p>
              <bk-tag
                v-for="(subjectScope, index) in projectData.afterSubjectScopes"
                :key="index"
              >
                {{ subjectScope.name }}
              </bk-tag>
            </div>
          </bk-form-item>
          <bk-form-item>
            <!--
              approvalStatus
              0-创建成功/修改成功,最终态
              1-创建审批中
              2-创建拒绝
              3-创建取消
              4-更新审批中
              5-更新拒绝
            -->
            <Popover
              :content="statusDisabledTips[projectData.approvalStatus]"
              :disabled="![1, 4].includes(projectData.approvalStatus)">
              <span>
                <bk-button
                  class="btn mr10"
                  theme="primary"
                  :disabled="[1, 4].includes(projectData.approvalStatus)"
                  @click="handleEdit"
                >
                  {{ t('编辑') }}
                </bk-button>
              </span>
            </Popover>
            
            <bk-button
              v-if="projectData.approvalStatus === 1"
              class="btn"
              theme="default"
              @click="handleCancelCreation"
            >
              {{ t('取消创建') }}
            </bk-button>

            <bk-button
              v-if="projectData.approvalStatus === 1"
              class="btn"
              theme="default"
              @click="handleCancelCreation"
            >
              {{ t('取消创建') }}
            </bk-button>
          </bk-form-item>
        </bk-form>
      </section>
    </article>
  </bk-loading>
</template>

<style lang="postcss" scoped>
  .content-wrapper {
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
  }
  .project-info-content {
    display: flex;
    flex-direction: column;
    padding: 24px;
    width: 100%;
    height: 100%;
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
  }
  .approval-details {
    cursor: pointer;
    color: #3A84FF;
    margin-left: 5px;
  }
  .content-main {
    flex: 1;
    padding: 32px 48px;
    margin-top: 16px;
    background-color: #fff;
    box-shadow: 0 2px 2px 0 rgba(0,0,0,0.15);
  }

  .detail-content-form {
    :deep(.bk-form-label) {
      font-size: 12px;
      text-align: left;
      color: #979BA5;
    }
    .project-name {
      display: flex;
      align-items: center;
    }
    .project-logo {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      margin-right: 5px;
    }
    .item-value {
      display: inline-block;
      max-width: 800px;
    }
    .mr10 {
      margin-right: 10px;
    }
    .btn {
      width: 88px;
    }
    .diff-content {
      max-width: 800px;
      padding: 8px;
      background: #F5F7FA;
      border: 1px solid #DCDEE5;
      border-radius: 2px;
    }
    .scopes-diff {
      margin-top: 10px;
    }
    .update-title {
      color: #63656E;
      font-weight: 700;
    }
    .inApproval {
      font-size: 12px;
      color: #FF9C01;
    }
  }
</style>
