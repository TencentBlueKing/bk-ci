<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import http from '@/http/api';
import { BaseInfoContent, PermissionContent, PipelineContent } from "@/components/project-form-item/";
import ProjectSettings from "./project-settings.vue"

const { t } = useI18n();
const emits = defineEmits(['change', 'approvedChange', 'initProjectForm', 'productIdChange', 'handleCancel', 'initProjectData', 'handleUpdate']);

export interface Dept {
  id: string;
  name: string;
  type?: string;
  parentId?: string;
  children?: Dept[];
  [key: string]: any;
}
const props = defineProps({
  data: Object,
  type: String,
  isChange: Boolean,
  btnLoading: Boolean
});

const isRbac = computed(() => {
  return authProvider.value === 'rbac'
})
const authProvider = ref(window.top.BK_CI_AUTH_PROVIDER || '')
const projectForm = ref();
const rules = {
  englishName: [
    {
      validator: value => /^[a-z][a-z0-9\-]{1,32}$/.test(value),
      message: t('项目ID必须由小写字母+数字+中划线组成，以小写字母开头，长度限制32字符！'),
      trigger: 'blur',
    },
  ],
  bgId: [
    {
      validator: () => !!(projectData.value.bgId && projectData.value.deptId),
      message: t('请选择项目所属组织'),
      trigger: 'blur',
    },
  ],
  subjectScopes: [
    {
      validator: () => projectData.value.subjectScopes.length > 0,
      message: t('请选择项目项目最大可授权人员范围'),
      trigger: 'change',
    },
  ],
};

const projectData = ref<any>(props.data);
const initPipelineDialect = ref();
const activeCollapse = ref(['baseInfo', 'permission', 'pipeline', 'artifactory']);
const collapsePanels = computed(() => [
  {
    name: 'baseInfo',
    title: '基础信息',
    component: BaseInfoContent,
  },
  ...isRbac.value ? [{
    name: 'permission',
    title: '权限',
    component: PermissionContent,
  }] : [],
  // ...projectData.value.properties ? [{
  //   name: 'pipeline',
  //   title: '流水线',
  //   isShow: projectData.value.properties,
  //   component: PipelineContent,
  // }] : [],
]);

const handleChangeForm = () => {
  emits('change', true);
};

const fetchUserDetail = async () => {
  if (props.type !== 'apply') {
    initPipelineDialect.value = projectData.value?.properties?.pipelineDialect
    return;
  };
  await http.getUserDetail().then((res) => {
    const { bgId, centerId, deptId } = res;
    projectData.value.bgId = bgId;
    projectData.value.centerId = centerId === '0' ? '' : centerId;
    projectData.value.deptId = deptId;
  });
};

const handleClearValidate = () => {
  projectForm.value.clearValidate();
}

const handleUpdate = (panel, params) => {
  emits('handleUpdate', panel, params)
}

watch(() => [projectData.value.authSecrecy, projectData.value.subjectScopes], (newValues, oldValues) => {
  if (newValues[0] !== oldValues[0] || JSON.stringify(newValues[1]) !== JSON.stringify(oldValues[1])) {
    projectForm.value.validate();
    emits('approvedChange', true);
  }
}, {
  deep: true,
});

onMounted(async () => {
  await fetchUserDetail();
  emits('initProjectForm', projectForm.value);
});
</script>

<template>
  <bk-form
    ref="projectForm"
    :rules="rules"
    :model="projectData"
    :label-width="216"
    class="project-form"
  >
    <bk-collapse
      v-if="props.type === 'apply'"
      v-model="activeCollapse"
      :hasHeaderHover="false"
    >
      <bk-collapse-panel
        v-for="(panel, index) in collapsePanels"
        :key="panel.name"
        :name="panel.name"
        icon="right-shape"
      >
          <span class="title">{{ t(panel.title) }}</span>
          <template #content>
            <div :class="['project-tab', { 'has-bottom-border': index !== collapsePanels.length - 1 }]">
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
    <project-settings
      v-else 
      :type="type"
      :is-rbac="isRbac"
      :data="projectData"
      :initPipelineDialect="initPipelineDialect"
      :btnLoading="btnLoading"
      @change="handleChangeForm"
      @clearValidate="handleClearValidate"
      @handleCancel="$emit('handleCancel')"
      @handleUpdate="handleUpdate"
      @initProjectData="$emit('initProjectData', $event)"
    />
  </bk-form>
</template>

<style lang="scss">
  .textarea {
    margin-top: 10px;
    
    :deep(textarea) {
      width: auto;
    }
  }
  :deep(.bk-form-label) {
    font-size: 12px;
  }
  .project-form {
    .bk-collapse-header {
      .bk-collapse-title {
        margin-left: 12px;
      }
      .bk-collapse-icon {
        left: 0;
      }
    }
    .bk-collapse-content {
      padding: 0;
    }
    .bk-collapse-item {
      padding: 16px 32px 0 32px;
      background-color: #fff;
    }
    .bk-form-item {
      margin: 0 auto;
      margin-bottom: 24px;
    }
    .title {
      font-weight: 700;
      font-size: 14px;
      color: #313238;
    }
  }
  .bk-form-error {
    white-space: nowrap;
  }
  .project-tab {
    width: 100%;
    padding: 20px 0;
    .sub-title {
      font-size: 14px;
      border-bottom: 2px solid #DCDEE5;
      margin-bottom: 15px;
    }
    .conventions-input {
      margin-top: 10px;
      max-width: 1000px;
    }
  }
  .has-bottom-border {
    border-bottom: 1px solid #DCDEE5;
  }
</style>