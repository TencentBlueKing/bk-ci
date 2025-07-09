<script setup>
import { onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { BaseInfoContent, PermissionContent, PipelineContent, ArtifactoryContent } from "@/components/project-form-item/";

const { t } = useI18n();
const emits = defineEmits(['change', 'clearValidate', 'tabChange']);
const props = defineProps({
  data: Object,
  type: String,
  isRbac: Boolean,
  initPipelineDialect: String
});

const projectData = ref(props.data);
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
const handleChangeForm = () => {
  emits('change', true);
};

const handleClearValidate = () => {
  emits('clearValidate');
};
function tabChange(active) {
  emits('tabChange', active)
}
</script>

<template>
  <div class="setting-content">
    <bk-tab
      v-model:active="panelActive"
      type="card-tab"
      @tab-change="tabChange"
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
          <div
            v-else
            v-for="(panel, index) in item.panels"
            :key="panel.name"
            class="other-setting"
          >
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
        </div>
      </bk-tab-panel>
    </bk-tab>
  </div>
</template>

<style lang="scss" scoped>
.setting-content {
  .edit-form-content {
    background-color: #fff;
    border-radius: 2px;
  }
}
.other-setting {
  height: 100%;
  padding: 25px 0;
}
</style>
<style lang="scss">
.setting-content {
  .bk-tab-content {
    padding: 0;
  }
}
</style>