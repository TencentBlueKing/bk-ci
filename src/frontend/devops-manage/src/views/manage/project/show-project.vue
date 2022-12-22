<script setup lang="ts">
import {
  ref,
} from 'vue';
import { useI18n } from 'vue-i18n';
import http from '@/http/api';
import {
  useRoute,
  useRouter,
} from 'vue-router';
import { Message, InfoBox } from 'bkui-vue';
import { computed, onMounted } from '@vue/runtime-core';
const { t } = useI18n();
const router = useRouter();
const route = useRoute();

const { projectCode } = route.params;
const projectData = ref<any>({});
const isLoading = ref(false);
const showStatusTips = computed(() => [1, 3, 4, 6].includes(projectData.value.approvalStatus));
const showCancelCreationBtn = computed(() => projectData.value.approvalStatus === 1);
const fetchProjectData = async () => {
  isLoading.value = true;
  await http.requestProjectData({
    englishName: projectCode,
  }).then((res) => {
    projectData.value = res;
  });
  isLoading.value = false;
};

const handleEdit = () => {
  router.push({
    path: 'edit',
  });
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

const approvalStatusMap = {
  1: {
    type: 'success',
    message: t('新建项目申请目前正在审批中，可前往查看'),
  },
  2: {
    type: 'success',
    message: t('新建项目申请已通过'),
  },
  3: {
    type: 'error',
    message: t('新建项目申请被拒绝。拒绝理由：项目所属组织信息填写有误。可前往查看'),
  },
  4: {
    type: 'success',
    message: t('编辑项目目前正在审批中，可前往查看'),
  },
  5: {
    type: 'success',
    message: t('编辑项目审批已通过'),
  },
  6: {
    type: 'error',
    message: t('编辑项目审批未通过。未通过理由：项目所属组织信息填写有误。可前往查看'),
  },
};

onMounted(() => {
  fetchProjectData();
});
</script>

<template>
  <bk-loading class="content-wrapper" :loading="isLoading">
    <article class="project-info-content">
      <bk-alert type="error" closable v-if="showStatusTips">
        <template #title>
          {{ approvalStatusMap[projectData.approvalStatus].message || '--' }}
        </template>
      </bk-alert>
      <section class="content-main">
        <bk-form class="detail-content-form" :label-width="160">
          <bk-form-item :label="t('项目名称')" :property="'name'">
            <div class="project-name">
              <img v-if="projectData.logoAddr" class="project-logo" :src="projectData.logoAddr" alt="">
              <span class="item-value">{{ projectData.projectName }}</span>
            </div>
          </bk-form-item>
          <bk-form-item :label="t('项目ID')" :property="'name'">
            <span class="item-value">{{ projectData.englishName }}</span>
          </bk-form-item>
          <bk-form-item :label="t('项目描述')" :property="'name'">
            <span class="item-value">{{ projectData.description }}</span>
          </bk-form-item>
          <bk-form-item :label="t('项目所属组织')" :property="'name'">
            <div>{{ projectData.bgName }} - {{ projectData.deptName }} - {{ projectData.centerName }}</div>
          </bk-form-item>
          <bk-form-item :label="t('项目性质')" :property="'name'">
            <span class="item-value">{{ projectData.authSecrecy ? t('保密项目') : t('私有项目') }}</span>
          </bk-form-item>
          <bk-form-item :label="t('项目最大可授权人员范围')" :property="'name'">
            <span class="item-value">
              <bk-tag
                v-for="(subjectScope, index) in projectData.subjectScopes"
                :key="index"
              >
                {{ subjectScope.name }}
              </bk-tag>
            </span>
          </bk-form-item>
          <bk-form-item>
            <bk-button
              class="btn mr10"
              theme="primary"
              :disabled="showCancelCreationBtn"
              @click="handleEdit"
            >
              {{ t('编辑') }}
            </bk-button>
            <bk-button
              v-if="[1, 3].includes(projectData.approvalStatus)"
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
  }
</style>
