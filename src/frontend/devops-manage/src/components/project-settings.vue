<script setup>
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { BaseInfoContent, PermissionContent, PipelineContent, ArtifactoryContent } from "@/components/project-form-item/";
const { t } = useI18n();
const emits = defineEmits(['change', 'clearValidate', 'tabChange']);
const props = defineProps({
  data: Object,
  type: String,
  initPipelineDialect: String,
  curDeptInfo: Array,
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
    {
      name: 'permission',
      title: '权限',
      component: PermissionContent,
    }]
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
function setProjectDeptProp (dept) {
  emits('setProjectDeptProp', dept)
}
function tabChange(active) {
  emits('tabChange', active)
}
function tabBeforeChange(name){
  console.log("🚀 ~ tabBeforeChange ~ name:", name)
  // return new Promise(async (resolve) => {
  //   if (props.type === 'edit') {
  //     infoBoxRef.value = InfoBox({
  //       type: 'warning',
  //       width: 640,
  //       title: '确认离开当前页？',
  //       content: '离开将会导致未保存信息丢失',
  //       footer: () => h('div',{}, [
  //         h(Button, {
  //           onClick() {
  //             resolve(true);
  //             infoBoxRef.value.hide()
  //           },
  //         }, t('确认')),
  //         h(Button, {
  //           onClick() {
  //             resolve(false);
  //             infoBoxRef.value.hide()
  //           },
  //         }, t('取消'))
  //       ]),
  //       onCancel() {
  //         resolve(false);
  //         infoBoxRef.value.hide()
  //       },
  //     });
  //   } else {
  //     resolve(true);
  //   }
  // });
}
</script>

<template>
  <div class="setting-content">
    <bk-tab
      v-model:active="panelActive"
      type="card-tab"
      @tab-change="tabChange"
      @beforeChange="tabBeforeChange"
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
                      :data="projectData"
                      :initPipelineDialect="initPipelineDialect"
                      @handle-change-form="handleChangeForm"
                      @clearValidate="handleClearValidate"
                      @setProjectDeptProp="setProjectDeptProp"
                      :curDeptInfo="curDeptInfo"
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
                  :is="panel.component"
                  :type="type"
                  :data="projectData"
                  :initPipelineDialect="initPipelineDialect"
                  @handle-change-form="handleChangeForm"
                  @clearValidate="handleClearValidate"
                />
            </div>
          </template>
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