<script setup>
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { BaseInfoContent, PermissionContent, PipelineContent, ArtifactoryContent } from "@/components/project-form-item/";

const { t } = useI18n();
const emits = defineEmits(['change', 'clearValidate']);
const props = defineProps({
  data: Object,
  type: String,
  isRbac: Boolean,
  initPipelineDialect: String
});

const projectData = ref(props.data);
const panelActive = ref('projectSettings')
const panels = [
  {
    name: 'projectSettings',
    label: '项目信息',
    activeCollapse: ['baseInfo', 'permission'],
    collapsePanels: [{
      name: 'baseInfo',
      title: '基础信息',
      component: BaseInfoContent,
    },
    ...props.isRbac ? [{
      name: 'permission',
      title: '权限',
      component: PermissionContent,
    }] : [],]
  },
  {
    name: 'pipelineSettings',
    label: '流水线设置',
    activeCollapse: ['pipeline'],
    collapsePanels: [
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
    activeCollapse: ['artifactory'],
    collapsePanels: [{
      name: 'artifactory',
      title: '制品库',
      component: ArtifactoryContent,
    }]
  },
]
const handleChangeForm = () => {
  emits('change', true);
};

const handleClearValidate = () => {
  emits('clearValidate');
};


</script>

<template>
  <div class="setting-content">
    <bk-tab
      v-model:active="panelActive"
      type="card-tab"
    >
      <bk-tab-panel
        v-for="(item, index) in panels"
        :key="item.name"
        :label="item.label"
        :name="item.name"
      >
        <div class="edit-form-content">
          <bk-collapse
            v-model="item.activeCollapse"
            :hasHeaderHover="false"
          >
            <bk-collapse-panel
              v-for="(panel, index) in item.collapsePanels"
              :key="panel.name"
              :name="panel.name"
              icon="right-shape"
            >
                <span class="title">{{ t(panel.title) }}</span>
                <template #content>
                  <div :class="['project-tab', { 'has-bottom-border': index !== item.collapsePanels.length - 1 }]">
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
        </div>
      </bk-tab-panel>
    </bk-tab>
  </div>
</template>

<style lang="scss" scoped>
.setting-content {
  .edit-form-content {
    background-color: #fff;
    box-shadow: 0 2px 4px 0 #1919290d;
    border-radius: 2px;
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