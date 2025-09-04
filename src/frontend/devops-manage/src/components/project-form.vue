<script setup lang="ts">
import { onMounted, ref, watch, computed} from 'vue';
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

const projectForm = ref<any>(null);
const projectData = ref<any>(props.data);
const rules = {
  englishName: [
    {
      validator: value => props.type !== 'apply' || /^[a-z][a-z0-9-]{1,32}$/.test(value),
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
const initPipelineDialect = ref();
const activeCollapse = ref(['baseInfo', 'permission', 'pipeline', 'artifactory']);
const collapsePanels = computed(() => [
  {
    name: 'baseInfo',
    title: '基础信息',
    component: BaseInfoContent,
  },
  {
    name: 'permission',
    title: '权限',
    component: PermissionContent,
  },
  // ...projectData.value.properties ? [{
  //   name: 'pipeline',
  //   title: '流水线',
  //   isShow: projectData.value.properties,
  //   component: PipelineContent,
  // }] : [],
]);

const setProjectDeptProp = (dept: Dept) => {
  if (!dept) return;
  const { id, name, type } = dept;
  projectData.value[`${type}Id`] = id;
  projectData.value[`${type}Name`] = name;
};

const handleChangeForm = () => {
  emits('change', true);
};

const curDeptInfo = ref();
const fetchUserDetail = async () => {
  let deptInfos: Dept[] = [];
  let centerId = '';
  let centerName = '';
  if (props.type !== 'apply') { // 编辑项目
    centerId = projectData.value.centerId;
    centerName = projectData.value.centerName;
    deptInfos = [{
      id: '0',
      name: '',
      type: 'bg',
    }, ...(projectData.value.bgId ? [{
      id: projectData.value.bgId,
      name: projectData.value.bgName,
      type: 'bg',
    }] : []),
    ...((projectData.value.businessLineId && projectData.value.businessLineName) ? [{
      id: projectData.value.businessLineId,
      name: projectData.value.businessLineName,
      type: 'businessLine',
    }] : []),
    ...(projectData.value.deptId ? [{
      id: projectData.value.deptId,
      name: projectData.value.deptName,
      type: 'dept',
    }] : []),
    ];
    
    initPipelineDialect.value = projectData.value?.properties?.pipelineDialect
  } else { // 申请创建项目
    const res = await http.getUserDetail();
    deptInfos = res.deptInfos;
    centerId = res.centerId;
    centerName = res.centerName;
  }
  if (centerId) {
    setProjectDeptProp({
      type: 'center',
      id: centerId,
      name: centerName,
    });
  }

  curDeptInfo.value = deptInfos;
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
  await fetchUserDetail()
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
                :data="projectData"
                :initPipelineDialect="initPipelineDialect"
                :curDeptInfo="curDeptInfo"
                @handle-change-form="handleChangeForm"
                @clearValidate="handleClearValidate"
                @setProjectDeptProp="setProjectDeptProp"
              />
            </div>
          </template>
      </bk-collapse-panel>
    </bk-collapse>
    <project-settings
      v-else
      :type="type"
      :data="projectData"
      :initPipelineDialect="initPipelineDialect"
      :curDeptInfo="curDeptInfo"
      :btnLoading="btnLoading"
      @change="handleChangeForm"
      @clearValidate="handleClearValidate"
      @setProjectDeptProp="setProjectDeptProp"
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
