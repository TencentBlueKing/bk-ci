<script setup>
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { BaseInfoContent, PermissionContent, PipelineContent, ArtifactoryContent } from "@/components/project-form-item/";
import { RESOURCE_ACTION, RESOURCE_TYPE} from '@/utils/permission.js';
import { InfoBox, Popover } from 'bkui-vue';
import { useRoute } from 'vue-router';
const { t } = useI18n();
const emits = defineEmits(['change', 'clearValidate', 'handleCancel', 'initProjectData', 'handleUpdate']);
const props = defineProps({
  data: Object,
  type: String,
  isRbac: Boolean,
  initPipelineDialect: String,
  btnLoading: Boolean
});

const route = useRoute();
const { projectCode } = route.params;
const componentsRef = ref();
const projectData = ref(props.data);
const initData = JSON.stringify(projectData.value)
const isChange = ref(false);
const infoBoxRef = ref();
const metadataList = ref([]);
const panelActive = ref('projectSettings')
const tabPanels = [
  {
    name: 'projectSettings',
    label: '项目信息',
    activeCollapse: ['baseInfo', 'permission'],
    panels: [{
      name: 'baseInfo',
      title: '基础信息',
      component: BaseInfoContent,
    },
    ...props.isRbac ? [{
      name: 'permission',
      title: '权限',
      component: PermissionContent,
    }] : []]
  },
  {
    name: 'pipelineSettings',
    label: '流水线设置',
    panels: [
      ...projectData.value.properties ? [{
        name: 'pipeline',
        title: '流水线',
        isShow: projectData.value.properties,
        component: PipelineContent,
      }] : [],
    ]
  },
  {
    name: 'artifactorySettings',
    label: '制品库设置',
    panels: [{
      name: 'artifactory',
      title: '制品库',
      component: ArtifactoryContent,
    }]
  },
]
const statusDisabledTips = {
  1: t('新建项目申请审批中，暂不可修改'),
  4: t('更新项目信息审批中，暂不可修改'),
};

function handleChangeForm () {
  isChange.value = true
  emits('change', true);
};
function handleClearValidate () {
  emits('clearValidate');
};
function tabBeforeChange(name){
  if (props.type === 'edit' && isChange.value) {
    infoBoxRef.value = InfoBox({
      title: t('确认离开当前页?'),
      subTitle: t('离开将会导致未保存信息丢失'),
      contentAlign: 'center',
      headerAlign: 'center',
      footerAlign: 'center',
      confirmText: t('离开'),
      cancelText: t('取消'),
      onConfirm: () => {
        isChange.value = false
        infoBoxRef.value.hide()
        panelActive.value = name
        emits('initProjectData', JSON.parse(initData))
        componentsRef.value?.resetData?.();
      },
      onClosed: () => {
        infoBoxRef.value.hide()
      }
    });
    return false
  } else {
    isChange.value = false
    return true
  }
}
function changeTab (name) {
  panelActive.value = name
  sessionStorage.setItem('currentTab', name)
}
function handleCancel() {
  emits('handleCancel')
}
function updateMetadata(params) {
  metadataList.value = params
}
function handleUpdate() {
  const updateEventName = panelActive.value === 'artifactorySettings' ? 'artifactorySettings' : undefined;
  emits('handleUpdate', updateEventName, metadataList.value);
  isChange.value = false
}

onMounted(() => {
  const currentTab = sessionStorage.getItem('currentTab')
  if (currentTab) {
    panelActive.value = currentTab
  }
})
</script>

<template>
  <div class="setting-content">
    <bk-tab
      v-model:active="panelActive"
      type="card-tab"
      :beforeChange="tabBeforeChange"
      @change="changeTab"
    >
      <bk-tab-panel
        v-for="(item, index) in tabPanels"
        :key="item.name"
        :label="t(item.label)"
        :name="item.name"
      >
        <div class="edit-form-content">
          <bk-collapse
            v-if="item.name === 'projectSettings'"
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
                    <component
                      :is="panel.component"
                      :type="type"
                      :is-rbac="isRbac"
                      :data="projectData"
                      :initPipelineDialect="initPipelineDialect"
                      @handle-change-form="handleChangeForm"
                      @clearValidate="handleClearValidate"
                    />
                  </div>
                </template>
            </bk-collapse-panel>
          </bk-collapse>
          <template v-else>
            <div
              v-for="(panel, index) in item.panels"
              :key="panel.name"
              class="other-setting"
            >
                <component
                  :ref="el => { if (panel.name === 'artifactory') componentsRef = el }"  
                  :is="panel.component"
                  :type="type"
                  :is-rbac="isRbac"
                  :data="projectData"
                  :initPipelineDialect="initPipelineDialect"
                  @handle-change-form="handleChangeForm"
                  @clearValidate="handleClearValidate"
                  @updateMetadata="updateMetadata"
                />
            </div>
          </template>
          <div class="btn-group">
            <div class="buttons">
              <Popover
                :content="statusDisabledTips[projectData?.approvalStatus]"
                :disabled="![1, 4].includes(projectData?.approvalStatus)"
              >
                <span>
                  <bk-button
                    class="btn mr10"
                    :disabled="[1, 4].includes(projectData?.approvalStatus)"
                    theme="primary"
                    :loading="btnLoading"
                    @click="handleUpdate"
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
                    {{ t('提交更新') }}
                  </bk-button>
                </span>
              </Popover>
              <bk-button
                class="btn"
                @click="handleCancel"
              >
                {{ t('取消') }}
              </bk-button>
            </div>
          </div>
        </div>
      </bk-tab-panel>
    </bk-tab>
  </div>
</template>

<style lang="scss" scoped>
.setting-content {
  position: relative;
  .edit-form-content {
    background-color: #fff;
    border-radius: 2px;
  }
}
.other-setting {
  height: 100%;
  padding: 25px 0;
}
.btn-group {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 48px;
  line-height: 48px;
  background: #FAFBFD;
  box-shadow: 0 -1px 0 0 #DCDEE5;
  .buttons {
    width: 1200px;
    margin: 0 auto;
  }
}
</style>
<style lang="scss">
.setting-content {
  .bk-tab-content {
    padding: 0;
  }
}
</style>