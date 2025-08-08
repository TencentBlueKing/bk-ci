<script setup lang="ts">
import http from '@/http/api';
import {
  handleProjectManageNoPermission,
  RESOURCE_ACTION,
  RESOURCE_TYPE,
} from '@/utils/permission.js';
import {
  InfoBox,
  Message,
  Popover
} from 'bkui-vue';
import {
  computed,
  onMounted,
  ref,
  watch
} from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import DialectPopoverTable from "@/components/dialectPopoverTable.vue";
import { ArtifactoryContent } from "@/components/project-form-item/";

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const { projectCode } = route.params;
const projectData = ref<any>({});
const projectDiffData = ref<any>({});
const isLoading = ref(false);
const userName = ref('');
const hasPermission = ref(true)
const showException = ref(false);
const exceptionObj = ref({
  type: '',
  title: '',
  description: '',
  showBtn: false
})
const activeTab = ref('projectSettings');
const tabPanels = computed(() => [
  {
    name: 'projectSettings',
    label: '项目信息',
    activeCollapse: ['baseInfo', 'permission'],
    panels: [{
      name: 'baseInfo',
      title: '基础信息',
    },
    ...isRbac.value ? [{
      name: 'permission',
      title: '权限',
    }] : []]
  },
  {
    name: 'pipelineSettings',
    label: '流水线设置',
    activeCollapse: ['pipeline'],
    panels: [
      ...projectData.value.properties ? [{
        name: 'pipeline',
        title: '流水线',
      }] : [],
    ]
  },
  {
    name: 'artifactorySettings',
    label: '制品库设置',
    activeCollapse: ['artifactory'],
    panels: [{
      name: 'artifactory',
      title: '制品库',
    }]
  },
])
const isRbac = computed(() => {
  return authProvider.value === 'rbac'
})
const authProvider = ref(window.top.BK_CI_AUTH_PROVIDER || '')
const projectList = window.parent?.vuexStore.state.projectList || [];
const fetchProjectData = async () => {
  isLoading.value = true;
  await http
    .requestProjectData({
      englishName: projectCode,
    })
    .then((res) => {
      projectData.value =  {
        ...res,
        properties: {
          pipelineDialect: 'CLASSIC',
          ...res.properties,
        },
      };

      // 审批状态下项目 -> 获取审批详情数据
      if ([1, 3, 4].includes(projectData.value.approvalStatus)) {
        fetchApprovalInfo();
      }
    })
    .catch((err) => {
      showException.value = true
      if (err.code === 403) {
        hasPermission.value = false
        exceptionObj.value.showBtn = true;
        exceptionObj.value.type = '403';
        exceptionObj.value.title = t('无项目权限');
        exceptionObj.value.description = t('你没有项目的查看权限，请先申请', [projectCode]);
      } else if (err.code === 404)  {
        exceptionObj.value.showBtn = false;
        exceptionObj.value.type = '404';
        exceptionObj.value.title = t('项目不存在');
        exceptionObj.value.description = '';
      } else if (err.code === 2119042) {
        exceptionObj.value.showBtn = false;
        exceptionObj.value.type = '403';
        exceptionObj.value.title = t('项目创建中');
        exceptionObj.value.description = t('项目正在创建审批中，请耐心等待', [projectCode]);
      }
    });
  isLoading.value = false;
};

const fetchApprovalInfo = () => {
  http.requestApprovalInfo(projectCode as string).then(res => {
    projectData.value = { ...projectData.value, ...res }
  });
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
  {
    current: 'bgName',
    after: 'afterBgName',
  },
  {
    current: 'deptName',
    after: 'afterDeptName',
  },
  {
    current: 'projectType',
    after: 'afterProjectType'
  },
  {
    current: 'centerName',
    after: 'afterCenterName',
  }
];
const propertiesFieldMap = [
  {
    current: 'pipelineDialect',
    after: 'afterPipelineDialect',
  },
  {
    current: 'pipelineNameFormat',
    after: 'afterPipelineNameFormat',
  },
  {
    current: 'loggingLineLimit',
    after: 'afterLoggingLineLimit'
  }
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
    propertiesFieldMap.forEach(field => {
      if (projectData.value?.properties[field.current] !== projectDiffData.value?.[field.after]) {
        projectData.value[field.after] = projectDiffData.value[field.after];
      }
    });
    if (projectData.value?.subjectScopes.length !== projectDiffData.value?.afterSubjectScopes.length) {
      projectData.value['afterSubjectScopes'] = projectDiffData.value.afterSubjectScopes
    } else {
      const subjectScopesIdMap = projectData.value.subjectScopes.map((i: any) => i.id);
      let isChange = false;
      subjectScopesIdMap.forEach((id: any) => {
        isChange = projectDiffData.value.afterSubjectScopes.some((scopes: any) => scopes.id !== id);
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
const changeTab = (name) => {
  activeTab.value = name
  sessionStorage.setItem('currentTab', name)
}
const handleEdit = () => {
  router.push({
    path: 'edit',
  });
};

const handleToApprovalDetails = (applyId: any) => {
  window.open(`/console/permission/my-apply/${applyId}`, '_blank')
};

/**
 * 取消更新项目 
 */
const handleCancelUpdate = () => {
  const onConfirm = async () => {
    const result = await http.cancelUpdateProject({
      projectId: projectData.value.project_id,
    });
    if (result) {
      Message({
        theme: 'success',
        message: t('取消更新成功'),
      });
      fetchProjectData();
    }
  };

  InfoBox({
    infoType: 'warning',
    title: t('确定取消更新项目'),
    contentAlign: 'center',
    headerAlign: 'center',
    footerAlign: 'center',
    onConfirm,
  });
};

/**
 * 停用/启用项目
 */
const handleEnabledProject = () => {
  const { englishName, enabled } = projectData.value;
  http
    .enabledProject({
      projectId: englishName,
      enable: !enabled,
    })
    .then(res => {
      if (res) {
        const message = enabled ? t('停用项目成功') : t('启用项目成功');
        Message({
          theme: 'success',
          message,
        });
        fetchProjectData();
      }
    })
    .catch((err) => {
      if (err.code === 403) {
        handleProjectManageNoPermission({
          action: RESOURCE_ACTION.ENABLE,
          projectId: projectCode,
          resourceCode: projectCode,
        })
      }
    })
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
      window.parent.location.href = `${location.origin}/console/pm`
    }
  };
  InfoBox({
    infoType: 'warning',
    title: t('确定取消创建项目'),
    contentAlign: 'center',
    headerAlign: 'center',
    footerAlign: 'center',
    onConfirm,
  });
};

const handleNoPermission = () => {
  const project = projectList.find(project => project.projectCode === projectCode)
  const params = {
    projectId: projectCode,
    resourceCode: projectCode,
    action: RESOURCE_ACTION.VIEW
  }
  if (!project) {
    delete params.action
  }
  handleProjectManageNoPermission(params)
};

const statusDisabledTips = {
  1: t('新建项目申请审批中，暂不可修改'),
  4: t('更新项目信息审批中，暂不可修改'),
};
const tipsStatusMap = {
  1: {
    type: 'info',
    message: t('新建项目申请目前正在审批中，可前往查看'),
  },
  2: {
    type: 'success',
    message: t('新建项目申请已通过'),
  },
  3: {
    type: 'error',
    message: t('新建项目申请被拒绝。'),
  },
  4: {
    type: 'info',
    message: t('更新项目信息目前正在审批中，可前往查看'),
  },
  5: {
    type: 'success',
    message: t('更新项目信息已通过'),
  },
  6: {
    type: 'error',
    message: t('更新项目信息审批被拒绝。'),
  },
  7: {
    type: 'error',
    message: t('创建项目申请单已撤回')
  }
};

const projectTypeNameMap = {
  0: '--',
  1: t('手游'),
  2: t('端游'),
  3: t('页游'),
  4: t('平台产品'),
  5: t('支撑产品'),
}
watch(() => projectData.value.approvalStatus, (status) => {
  if (status === 4) fetchDiffProjectData();
}, {
  deep: true,
});
onMounted(async () => {
  const currentTab = sessionStorage.getItem('currentTab')
  if (currentTab) {
    activeTab.value = currentTab
  }
  await getUserInfo();
  await fetchProjectData();
});
</script>

<template>
  <section class="project-info">
    <bk-alert
      v-if="projectData.projectCode && projectData.tipsStatus !== 0"
      :theme="tipsStatusMap[projectData.tipsStatus].type"
      closable
      class="status-tips"
    >
      <template #title>
        {{ tipsStatusMap[projectData.tipsStatus].message || '--' }}
        <a class="approval-details" v-if="[1, 3, 4].includes(projectData.tipsStatus)" @click="handleToApprovalDetails(projectData.applyId)">{{ t('审批详情') }}</a>
        <span v-if="projectData.approvalMsg">{{ t('拒绝理由：') }}{{ projectData.approvalMsg }}</span>
      </template>
    </bk-alert>

    <div class="project-info-content">
      <bk-tab
        class="content-wrapper"
        v-model:active="activeTab"
        type="card-tab"
        @change="changeTab"
        v-if="hasPermission && projectData.projectCode"
      >
        <bk-tab-panel
          v-for="(item, index) in tabPanels"
          :key="item.name"
          :label="t(item.label)"
          :name="item.name"
        >
          <bk-loading
            :loading="isLoading"
            class="detail-content-form"
          >
          <template v-if="item.name === 'projectSettings'">
            <bk-collapse
              v-model="item.activeCollapse"
              :hasHeaderHover="false"
            >
              <bk-collapse-panel
                v-for="(panel, index) in item.panels"
                :key="panel.name"
                :name="panel.name"
                icon="right-shape"
              >
                  <span class="title">{{ t(panel.title) }}</span>
                  <template #content>
                    <div :class="['project-tab', { 'has-bottom-border': index !== item.panels.length - 1 }]">
                      <template v-if="panel.name === 'baseInfo'">
                        <bk-form label-position="right" :label-width="200">
                          <bk-form-item :label="t('项目名称')" property="projectName">
                            <div class="project-name">
                              <img v-if="projectData.logoAddr" class="project-logo" :src="projectData.logoAddr" alt="">
                              <span class="item-value">{{ projectData.projectName }}</span>
                              <span class="enable-status">
                                <img class="enable-status-icon" v-if="projectData.enabled" src="../../../css/svg/normal.svg" alt="">
                                <img class="enable-status-icon" v-else src="../../../css/svg/unknown.svg" alt="">
                                {{ projectData.enabled ? t('已启用') : t('已停用') }}
                              </span>
                            </div>
                            <div class="diff-content" v-if="projectData.afterLogoAddr || projectData.afterProjectName">
                              <p class="update-title">{{ t('本次更新：') }}</p>
                              <div class="project-logo-name">
                                <img v-if="projectData.afterLogoAddr" class="project-logo" :src="projectData.afterLogoAddr" alt="">
                                <span class="item-value">{{ projectData.afterProjectName || projectData.projectName }}</span>
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
                          <bk-form-item :label="t('项目类型')" property="bg">
                            <span>{{ projectTypeNameMap[projectData.projectType] }}</span>
                            <div class="diff-content" v-if="projectData.afterProjectType">
                              <p class="update-title">
                                {{ t('本次更新：') }}
                              </p>
                              <span>{{ projectTypeNameMap[projectData.afterProjectType] }}</span>
                            </div>
                          </bk-form-item>
                          <bk-form-item v-if="isRbac" :label="t('项目性质')" property="authSecrecy">
                            <span class="item-value">{{ projectData.authSecrecy ? t('保密项目') : t('私有项目') }}</span>
                            <div class="diff-content" v-if="projectData.afterAuthSecrecy">
                              <p class="update-title">
                                {{ t('本次更新：') }}
                                <span class="inApproval">{{ t('(审批中)') }}</span>
                              </p>
                              <div>{{ projectData.afterAuthSecrecy ? t('保密项目') : t('私有项目') }}</div>
                            </div>
                          </bk-form-item>
                        </bk-form>
                      </template>
                      <template v-if="panel.name === 'permission'">
                        <bk-form label-position="right" :label-width="200">
                          <bk-form-item :label="t('项目最大可授权人员范围')" property="subjectScopes">
                            <span class="item-value">
                              <bk-tag
                                v-for="(subjectScope, index) in projectData.subjectScopes"
                                :key="index"
                              >
                                {{ subjectScope.id === '*' ? t('全员') : subjectScope.name }}
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
                                {{ subjectScope.id === '*' ? t('全员') : subjectScope.name }}
                              </bk-tag>
                            </div>
                          </bk-form-item>
                        </bk-form>
                      </template>
                    </div>
                  </template>
              </bk-collapse-panel>
            </bk-collapse>
          </template>
          <template v-else>
            <div
              v-for="panel in item.panels"
              :key="panel.name"
              style="padding: 9px 0;"
            >
              <template v-if="panel.name === 'pipeline'">
                <bk-form lable-position="right" :label-width="200">
                  <bk-form-item
                    v-if="projectData.properties"
                    property="pipelineDialect"
                  >
                    <template #label>
                      <dialect-popover-table />
                    </template>
                    <div>
                      <span>{{ t(projectData.properties.pipelineDialect) }}</span>
                      <div class="diff-content" v-if="projectData.afterPipelineDialect">
                        <p class="update-title">
                          {{ t('本次更新：') }}
                        </p>
                        <span>{{ t(projectData.afterPipelineDialect) }}</span>
                      </div>
                    </div>
                  </bk-form-item>
                  <bk-form-item
                    :label="t('命名规范提示')"
                    property="pipelineNameFormat"
                  >
                    <span class="item-value">
                      {{ projectData.properties.enablePipelineNameTips ? (projectData.properties.pipelineNameFormat || '--') : t('未开启') }}
                    </span>
                    <div class="diff-content" v-if="projectData.afterPipelineNameFormat">
                      <p class="update-title">
                        {{ t('本次更新：') }}
                      </p>
                      <span>{{ projectData.afterPipelineNameFormat }}</span>
                    </div>
                  </bk-form-item>
                  <bk-form-item
                    :label="t('构建日志归档阈值')"
                    property="loggingLineLimit"
                    :description="t('单个步骤(Step)日志达到阈值时，将压缩并归档到日志仓库。可下载日志文件到本地查看。')"
                  >
                    <span class="item-value">
                      {{ projectData.properties.loggingLineLimit || '--' }}
                      <span v-if="projectData.properties.loggingLineLimit">
                        {{ t('万行') }}
                      </span>
                    </span>
                    <div class="diff-content" v-if="projectData.afterLoggingLineLimit">
                      <p class="update-title">
                        {{ t('本次更新：') }}
                      </p>
                      <span>{{ `${projectData.afterLoggingLineLimit} ${t('万行')}` }}</span>
                    </div>
                  </bk-form-item>
                </bk-form>
              </template>
              <template v-if="panel.name === 'artifactory'">
                <bk-form label-position="right" :label-width="200">
                  <ArtifactoryContent 
                    :data="projectData"
                    type="show"
                  />
                </bk-form>
              </template>
            </div>
          </template>
          </bk-loading>
        </bk-tab-panel>
      </bk-tab>

      <bk-exception
        v-if="showException"
        class="exception-content mt20"
        :type="exceptionObj.type"
        :title="exceptionObj.title"
        :description="exceptionObj.description"
      >
        <bk-button v-if="exceptionObj.showBtn" theme="primary" @click="handleNoPermission">
          {{ t('去申请') }}
        </bk-button>
      </bk-exception>

      <div class="btn-group">
        <!--
          approvalStatus
          0-创建成功/修改成功,最终态
          1-新增审批
          2-正常
          3-新增审批拒绝
          4-更新审批中
        -->
        <Popover
          :content="statusDisabledTips[projectData.approvalStatus]"
          :disabled="![1, 4].includes(projectData.approvalStatus)"
        >
          <span>
            <bk-button
              v-if="hasPermission && projectData.projectCode"
              class="btn mr10"
              theme="primary"
              :disabled="[1, 4].includes(projectData.approvalStatus)"
              @click="handleEdit"
              v-perm="{
                disablePermissionApi: !projectData.projectCode || [1, 3, 4].includes(projectData.approvalStatus),
                hasPermission: !projectData.projectCode || [1, 3, 4].includes(projectData.approvalStatus),
                permissionData: {
                  projectId: projectData.projectCode,
                  resourceType: RESOURCE_TYPE,
                  resourceCode: projectData.projectCode,
                  action: RESOURCE_ACTION.EDIT
                }
              }"
            >
              {{ t('编辑') }}
            </bk-button>
          </span>
        </Popover>

        <Popover
          :content="t('仅更新人可撤销更新')"
          :disabled="userName !== projectData.updator">
          <bk-button
            v-if="[4].includes(projectData.approvalStatus)"
            class="btn"
            theme="default"
            :disabled="userName !== projectData.updator"
            @click="handleCancelUpdate"
          >
            {{ t('撤销更新') }}
          </bk-button>
        </Popover>
        
        <bk-button
          v-if="[1, 3].includes(projectData.approvalStatus)"
          class="btn"
          theme="default"
          @click="handleCancelCreation"
        >
          {{ t('取消创建') }}
        </bk-button>
        <bk-button
          v-if="projectData.approvalStatus === 2"
          v-perm="{
            disablePermissionApi: !projectData.projectCode,
            hasPermission: !projectData.projectCode,
            permissionData: {
              projectId: projectData.projectCode,
              resourceType: RESOURCE_TYPE,
              resourceCode: projectData.projectCode,
              action: RESOURCE_ACTION.ENABLE
            }
          }"
          class="enable-btn"
          theme="default"
          @click="handleEnabledProject"
        >
          {{ projectData.enabled ? t('停用项目') : t('启用项目') }}
        </bk-button>
      </div>
    </div>
  </section>
</template>

<style lang="scss">
 .content-wrapper {
  .bk-collapse-content {
    padding: 0;
  }
  .bk-tab-content{
    padding: 0;
  }
 }
</style>

<style lang="scss" scoped>
  .project-info {
    display: flex;
    flex-direction: column;
    padding: 24px;
    height: 100%;
    width: 100%;
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
  .content-wrapper {
    flex: 1;
    width: 100%;
  }
  .project-info-content {
    display: flex;
    flex-direction: column;
    width: 1200px;
    height: 100%;
    margin: 0 auto;
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
  .status-tips {
    width: 1200px;
    margin: auto;
    margin-bottom: 16px;
  }
  .approval-details {
    cursor: pointer;
    color: #3A84FF;
    margin-left: 5px;
  }
  .content-main {
    color: #313238;
  }
  .exception-content {
    height: 100%;
    background-color: #fff;
    box-shadow: 0 2px 2px 0 rgb(0 0 0 / 15%);
  }
  .detail-content-form {
    margin-bottom: 20px;
    padding: 16px 18px 16px 32px;
    background-color: #fff;
    box-shadow: 0 2px 2px 0 rgb(0 0 0 / 15%);
    ::v-deep .bk-form-label {
      font-size: 12px;
      color: #979BA5;
    }
    .title {
      width: 56px;
      height: 22px;
      margin-bottom: 16px;
      font-family: MicrosoftYaHei-Bold;
      font-weight: 700;
      font-size: 14px;
      color: #63656E;
      letter-spacing: 0;
      line-height: 22px;
    }
    .sub-title {
      font-size: 14px;
      color: #63656E;
      border-bottom: 2px solid #DCDEE5;
      margin-bottom: 15px;
    }
    .project-name {
      display: flex;
      align-items: center;

    }
    .project-tab {
      padding: 5px 49px;
    }
    .enable-status {
      display: flex;
      height: 24px;
      align-items: center;
      margin-left: 20px;
      padding: 0px 10px;
      background: #F0F1F5;
      border-radius: 12px;
      .enable-status-icon {
        width: 18px;
        height: 18px;
        margin-right: 2px;
      }
    }
    .project-logo {
      width: 60px;
      height: 60px;
      margin-right: 10px;
    }
    .item-value {
      display: inline-block;
      max-width: 800px;
      white-space: pre-wrap;
    }
    .mr10 {
      margin-right: 10px;
    }
    .btn {
      width: 88px;
    }
    .enable-btn {
      width: 120px;
    }
    .diff-content {
      max-width: 800px;
      padding: 8px;
      background: #F5F7FA;
      border: 1px solid #DCDEE5;
      border-radius: 2px;
      white-space: pre-wrap;
    }
    .scopes-diff {
      margin-top: 10px;
    }
    .update-title {
      color: #63656E;
      font-weight: 700;
    }
    .project-logo-name {
      display: flex;
    }
    .inApproval {
      font-size: 12px;
      color: #FF9C01;
    }
  }
  .btn-group {
    margin-top: 20px;
  }
</style>
