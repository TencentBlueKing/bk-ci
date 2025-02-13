<script setup lang="ts">
import {
  ref,
  watch,
  onBeforeUnmount,
  computed,
  onMounted,
  getCurrentInstance,
  h,
} from 'vue';
import {
  EditLine,
} from 'bkui-vue/lib/icon';
import IAMIframe from './IAM-Iframe';
import { useI18n } from 'vue-i18n';
import { Message, Popover, InfoBox, Alert, Input } from 'bkui-vue';
import http from '@/http/api';
import DialectPopoverTable from "@/components/dialectPopoverTable.vue";
import copyImg from "@/css/svg/copy.svg";
import { copyToClipboard } from "@/utils/util.js"
const {
  t,
} = useI18n();
const emits = defineEmits(['change', 'approvedChange', 'initProjectForm']);

const props = defineProps({
  data: Object,
  type: String,
  isChange: Boolean,
});

const logoFiles = computed(() => {
  const { logoAddr } = projectData.value;
  const files: any = [];
  if (logoAddr) {
    files.push({
      url: logoAddr,
    });
  }
  return files;
});
const isRbac = computed(() => {
  return authProvider.value === 'rbac'
})
const authProvider = ref(window.top.BK_CI_AUTH_PROVIDER || '')
const projectForm = ref(null);
const iframeRef = ref(null);
const vm = getCurrentInstance();
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
      validator: () => projectData.value.bgId && projectData.value.deptId,
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

const projectTypeList = [
  {
    id: 1,
    name: t('手游'),
  },
  {
    id: 2,
    name: t('端游'),
  },
  {
    id: 3,
    name: t('页游'),
  },
  {
    id: 4,
    name: t('平台产品'),
  },
  {
    id: 5,
    name: t('支撑产品'),
  },
];

const projectData = ref<any>(props.data);
const deptLoading = ref({
  bg: false,
  dept: false,
  center: false,
});

const curDepartmentInfo = ref({
  bg: [],
  dept: [],
  center: [],
});

const showDialog = ref(false);

const confirmSwitch = ref('');
const pipelineSideslider = ref(false);
const pipelineList = ref([]);
const isLoading = ref(false);
const pipelinePagination = ref({ count: 0, limit: 100, current: 1 });

const getDepartment = async (type: string, id: any) => {
  deptLoading.value[type] = true;
  try {
    const res = await http.getOrganizations({
      type,
      id,
    });
    curDepartmentInfo.value[type] = [...res];
  } catch (err: any) {
    Message({
      message: err.message || err,
      theme: 'danger',
    });
    curDepartmentInfo.value[type] = [];
  } finally {
    deptLoading.value[type] = false;
  }
};

/**
 * 根据 type id 设置组织名称
 * @param {*} type 类型
 * @param {*} id 类型ID
 */
const setOrgName = (type: string, id: any) => {
  const item = curDepartmentInfo.value[type].find((item: { id: any; }) => item.id === id);
  if (item) {
    projectData.value[`${type}Name`] = item.name;
  }
};

const handleChangeBg = (type: string, id: any) => {
  handleChangeForm();
  projectData.value.deptId = '';
  projectData.value.deptName = '';
  projectData.value.centerId = '';
  projectData.value.centerName = '';
  curDepartmentInfo.value.dept = [];
  curDepartmentInfo.value.center = [];
  if (id) {
    setOrgName(type, id);
    getDepartment('dept', id);
  }
};

const handleChangeDept = (type: string, id: any) => {
  handleChangeForm();
  projectData.value.centerId = '';
  projectData.value.centerName = '';
  curDepartmentInfo.value.center = [];
  if (id) {
    setOrgName(type, id);
    getDepartment('center', id);
  }
};

const handleChangeCenter = (type: string, id: any) => {
  handleChangeForm();
  if (id) {
    setOrgName(type, id);
  };
};

const fetchDepartmentList = async () => {
  const { bgId, deptId, centerId } = projectData.value;
  await getDepartment('bg', 0);
  if (bgId) {
    await getDepartment('dept', bgId);
  }
  if (deptId) {
    await getDepartment('center', deptId);
  }
};

const handleChangeForm = () => {
  emits('change', true);
};

const handleUploadLogo = async (res: any) => {
  handleChangeForm();
  const { file } = res;
  if (file) {
    if (!(file.type === 'image/jpeg' || file.type === 'image/png')) {
      Message({
        theme: 'danger',
        message: t('只允许上传png、jpg'),
      });
    } else if (file.size > (2 * 1024 * 1024)) {
      Message({
        theme: 'danger',
        message: t('大小不超过2M'),
      });
    } else {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => {
        projectData.value.logoAddr = reader.result;
      };
    }
    const formData = new FormData();
    formData.append('logo', file);
    await http.uploadProjectLogo({
      formData,
    }).then((res) => {
      projectData.value.logoAddress = res;
    });
  }
};

const createAlertContent = (titleText, paragraphs) => {
  return h(Alert, { theme: "warning" }, {
    title: () => h('div', { style: { 'line-height': '20px' } }, [
      h('p', { class: 'alarm' }, t(titleText)),
      ...paragraphs
    ])
  });
};

const changeClassic = () => {
  return createAlertContent('危险操作告警：', [
    h('p', t('alarmTip1')),
    h('p', { class: 'tip-script', style: { 'font-size': '12px' } }, [
      h('p', { class: 'text-white' }, 'a=3'),
      h('p', [
        h('span', { class: 'text-echo' }, 'echo'),
        h('span', { class: 'text-a' }, '$'),
        h('span', '{'),
        h('span', { class: 'text-a' }, 'a'),
        h('span', '}'),
      ]),
    ]),
    h('p', [
      h('span', {
        class: 'tip-blue',
        onClick() {
          pipelineSideslider.value = true;
          // Message({ theme: 'success', message: t('复制成功') });
        },
      }, t('X条', [pipelinePagination.value.count])),
      h('span', t('alarmTip2'))
    ]),
  ]);
};

const changeConstrained = () => {
  return createAlertContent('危险操作告警：', [
    h('p', [
      h('span', t('alarmTip3')),
      h('span', {
        class: 'tip-blue',
        onClick() {
          pipelineSideslider.value = true;
        },
      }, t('X条', [pipelinePagination.value.count])),
      h('span', t('alarmTip4'))
    ]),
  ]);
};

const beforeChange = async () => {
  confirmSwitch.value = '';

  return new Promise((resolve) => {
    if (props.type === 'edit') {
      const copyText = t('我已明确变更风险且已确认变更无影响');
      const isClassic = projectData.value.properties.pipelineDialect !== 'CLASSIC';
      const title = isClassic ? t('确认切换成“传统风格？') : t('确认切换成“制约风格？');
      const content = isClassic ? changeClassic() : changeConstrained();

      InfoBox({
        type: 'warning',
        width: 640,
        title: title,
        confirmText: t('确认切换'),
        cancelText: t('取消'),
        confirmButtonTheme: 'danger',
        content: h('div', [
          content,
          h('div', { class: 'confirm-switch' }, [
            h('p', { class: 'confirm-tit' }, [
              h('span', t('请输入')),
              h('span', { style: { 'font-weight': 700 } }, ` "${copyText}" `),
              h('img', {
                src: copyImg,
                style: { width: '12px', 'vertical-align': 'middle', margin: '4px' },
                onClick() {
                  copyToClipboard(copyText);
                  Message({ theme: 'success', message: t('复制成功') });
                },
              }),
              h('span', t('以确认切换'))
            ]),
            h('input', {
              class: 'confirm-input',
              placeholder: t('请输入'),
              value: confirmSwitch.value,
              onInput: (event: any) => {
                confirmSwitch.value = event.target.value;
              }
            })
          ])
        ]),
        onConfirm: () => {
          if (confirmSwitch.value === copyText) {
            resolve(true);
          } else {
            resolve(false);
            Message({ theme: 'error', message: t('请输入：我已明确变更风险且已确认变更无影响') });
            confirmSwitch.value = '';
          }
        },
        onCancel: () => {
          resolve(false);
        },
      });
    } else {
      resolve(true);
    }
  });
};

const fetchListViewPipelines = async ()=> {
  try {
    isLoading.value = true
    const params = {
      showDelete: true,
      sortType: 'CREATE_TIME',
      collation: 'DESC',
      page: pipelinePagination.value.current,
      pageSize: pipelinePagination.value.limit,
      viewId: 'allPipeline'
    }
    const res = await http.listViewPipelines(projectData.value.englishName, params)
    pipelineList.value = res.records
    pipelinePagination.value.count = res.count
  } catch (error) {
    console.log(error);
  } finally {
    isLoading.value = false
  }
}

const pageLimitChange = (limit: number) => {
  pipelinePagination.value.limit = limit;
  fetchListViewPipelines()
}

const pageValueChange = (value:number) => {
  pipelinePagination.value.current = value;
  fetchListViewPipelines()
}

const handleToPipeline = (row) => {
  window.open(`/console/pipeline/${row.projectId}/${row.pipelineId}/history/pipeline`, '__blank')
}

const handleMessage = (event: any) => {
  const { data } = event;
  if (data.type === 'IAM') {
    switch (data.code) {
      case 'success':
        handleChangeForm();
        projectData.value.subjectScopes = data.data.subject_scopes;
        showDialog.value = false;
        break;
      case 'cancel':
        showDialog.value = false;
        break;
      case 'load':
        setTimeout(() => {
          // 回显数据
          vm?.refs?.iframeRef?.$el?.firstElementChild?.contentWindow?.postMessage?.(
            JSON.parse(JSON.stringify({
              subject_scopes: projectData.value.subjectScopes,
            })),
            window.BK_IAM_URL_PREFIX,
          );
        }, 0);
        break;
    }
  }
};

const fetchUserDetail = async () => {
  if (props.type !== 'apply') return;
  await http.getUserDetail().then((res) => {
    const { bgId, centerId, deptId } = res;
    projectData.value.bgId = bgId;
    projectData.value.centerId = centerId === '0' ? '' : centerId;
    projectData.value.deptId = deptId;
  });
};

const showMemberDialog = () => {
  showDialog.value = true;
};

const validateProjectNameTips = ref('');
watch(() => projectData.value.projectName, (val) => {
  if (props.type === 'apply' && val) {
    http.validateProjectName(val)
      .then(() => {
        validateProjectNameTips.value = '';
      })
      .catch(() => {
        projectForm.value.clearValidate();
        validateProjectNameTips.value = t('项目名称已存在');
      });
  } else if (!val) {
    validateProjectNameTips.value = '';
  }
}, {
  deep: true,
});

const validateEnglishNameTips = ref('');
watch(() => projectData.value.englishName, (val) => {
  if (props.type === 'apply' && val && /^[a-z][a-z0-9\-]{1,32}$/.test(val)) {
    http.validateEnglishName(val)
      .then(() => {
        validateEnglishNameTips.value = '';
      })
      .catch(() => {
        projectForm.value.clearValidate();
        validateEnglishNameTips.value = t('项目ID已存在');
      });
  } else if (!val || !/^[a-z][a-z0-9\-]{1,32}$/.test(val)) {
    validateEnglishNameTips.value = '';
  }
}, {
  deep: true,
});

watch(() => [projectData.value.authSecrecy, projectData.value.projectType, projectData.value.subjectScopes], () => {
  projectForm.value.validate();
  emits('approvedChange', true);
}, {
  deep: true,
});

onMounted(async () => {
  await fetchUserDetail();
  if (props.type === 'edit' && projectData.value.englishName) {
    fetchListViewPipelines();
  }
  // await fetchDepartmentList();
  emits('initProjectForm', projectForm.value);
  window.addEventListener('message', handleMessage);
});

onBeforeUnmount(() => {
  window.removeEventListener('message', handleMessage);
});
</script>

<template>
  <bk-form
    ref="projectForm"
    :rules="rules"
    :model="projectData"
    :label-width="216"
  >
    <div class="project-tab advanced">
      <p class="title">{{t('基础信息')}}</p>
      <bk-form-item :label="t('项目名称')" property="projectName" :required="true">
        <bk-input
          v-model="projectData.projectName"
          :placeholder="t('请输入1-32字符的项目名称')"
          :maxlength="32"
          @change="handleChangeForm"
        ></bk-input>
        <div class="error-tips" v-if="validateProjectNameTips">
          {{ validateProjectNameTips }}
        </div>
      </bk-form-item>
      <bk-form-item :label="t('项目ID')" property="englishName" :required="true">
        <bk-input
          v-model="projectData.englishName"
          :disabled="type === 'edit'"
          :maxlength="32"
          :placeholder="t('请输入2-32 字符的项目ID，由小写字母、数字、中划线组成，以小写字母开头。提交后不可修改。')"
        ></bk-input>
        <div class="error-tips" v-if="validateEnglishNameTips">
          {{ validateEnglishNameTips }}
        </div>
      </bk-form-item>
      <bk-form-item :label="t('项目描述')" property="description" :required="true">
        <bk-input
          v-model="projectData.description"
          class="textarea"
          type="textarea"
          :rows="3"
          :maxlength="255"
          :placeholder="t('请输入项目描述')"
          @change="handleChangeForm"
        ></bk-input>
      </bk-form-item>
      <bk-form-item :label="t('项目LOGO')">
        <bk-upload
          theme="picture"
          :files="logoFiles"
          with-credentials
          :multiple="false"
          :custom-request="handleUploadLogo"
        />
        <span class="logo-upload-tip">{{ t('只允许上传png、jpg，大小不超过 2M')}}</span>
      </bk-form-item>
      <!-- <bk-form-item :label="t('项目所属组织')" property="bgId" :required="true">
        <div class="bk-dropdown-box">
          <bk-select
            v-model="projectData.bgId"
            placeholder="BG"
            name="bg"
            :loading="deptLoading.bg"
            filterable
            @change="id => handleChangeBg('bg', id)"
          >
            <bk-option
              v-for="bg in curDepartmentInfo.bg"
              :value="bg.id"
              :key="bg.id"
              :label="bg.name"
            />
          </bk-select>
        </div>
        <div class="bk-dropdown-box">
          <bk-select
            v-model="projectData.deptId"
            :placeholder="t('部门')"
            name="dept"
            :loading="deptLoading.dept"
            filterable
            @change="id => handleChangeDept('dept', id)"
          >
            <bk-option
              v-for="bg in curDepartmentInfo.dept"
              :value="bg.id"
              :key="bg.id"
              :label="bg.name"
            />
          </bk-select>
        </div>
        <div class="bk-dropdown-box">
          <bk-select
            v-model="projectData.centerId"
            :placeholder="t('中心')"
            name="center"
            :loading="deptLoading.center"
            filterable
            @change="id => handleChangeCenter('center', id)"
          >
            <bk-option
              v-for="center in curDepartmentInfo.center"
              :value="center.id"
              :key="center.id"
              :label="center.name"
            />
          </bk-select>
        </div>
      </bk-form-item> -->
      <bk-form-item :label="t('项目类型')" property="projectType" :required="true">
        <bk-select
          v-model="projectData.projectType"
          :placeholder="t('选择项目类型')"
          name="center"
          searchable
          @change="handleChangeForm"
        >
          <bk-option
            v-for="type in projectTypeList"
            :value="type.id"
            :key="type.id"
            :label="type.name"
          />
        </bk-select>
      </bk-form-item>
      <bk-form-item
        v-if="isRbac"
        :label="t('项目性质')"
        property="authSecrecy"
        :required="true"
      >
        <bk-radio-group
          v-model="projectData.authSecrecy"
          @change="handleChangeForm"
        >
          <bk-radio class="mr10" :label="0">
            <Popover :content="t('`项目最大可授权人员范围`内的用户可以主动申请加入项目')">
              <span class="authSecrecy-item">{{ t('私有项目') }}</span>
            </Popover>
          </bk-radio>
          <bk-radio :label="1">
            <Popover :content="t('拥有项目/资源管理权限的成员才可以主动添加用户')">
              <span class="authSecrecy-item">{{ t('保密项目') }}</span>
            </Popover>
          </bk-radio>
        </bk-radio-group>
      </bk-form-item>
    </div>
    <div class="project-tab">
      <p class="title">{{t('高级信息')}}</p>
      <div v-if="isRbac">
        <div class="sub-title">{{ t('权限')  }}</div>
        <bk-form-item
          :label="t('项目最大可授权人员范围')"
          :description="t('该设置表示可以加入项目的成员的最大范围，范围内的用户才可以成功加入项目下的任意用户组')"
          property="subjectScopes"
          :required="true">
          <bk-tag
            v-for="(subjectScope, index) in projectData.subjectScopes"
            :key="index"
          >
            {{ subjectScope.id === '*' ? t('全员') : subjectScope.name }}
          </bk-tag>
          <EditLine
            class="edit-line ml5"
            @click="showMemberDialog"
          />
        </bk-form-item>
      </div>
      <div v-if="projectData.properties">
        <div class="sub-title">{{ t('流水线')  }}</div>
        <bk-form-item
          property="pipelineDialect"
        >
          <template #label>
            <dialect-popover-table />
          </template>
          <bk-radio-group
            v-model="projectData.properties.pipelineDialect"
            @change="handleChangeForm"
            :beforeChange="beforeChange"
          >
            <bk-radio label="CLASSIC">
              <span>{{ t('CLASSIC') }}</span>
            </bk-radio>
            <bk-radio label="CONSTRAINED">
              <span>{{ t('CONSTRAINED') }}</span>
            </bk-radio>
          </bk-radio-group>
          <div v-if="type==='apply'">
            <bk-alert theme="warning">
              <template #title>
                <div v-if="projectData.properties.pipelineDialect==='CONSTRAINED'">
                  {{ t('CLASSICTIP') }}
                </div>
                <div v-else-if="projectData.properties.pipelineDialect==='CLASSIC'">
                  <p>{{ t('CONSTRAINEDTIP') }}</p>
                  <p>{{ t('示例，存在流水线变量 a=1，则如下脚本打印出的 a为1，而不是脚本中设置的 3') }}</p>
                  <p class="tip-script">
                    <p class="text-white">a=3</p>
                    <p>
                      <span class="text-echo">echo</span><span class="text-a">$</span>{<span class="text-a">a</span>}
                    </p>
                  </p>
                  <p>{{ t('请知悉，编排流水线时， Bash 脚本中的局部变量请勿与流水线变量重名，避免出现同名赋值问题。') }}</p>
                </div>
              </template>
            </bk-alert>
          </div>
        </bk-form-item>
        <bk-form-item
          :label="t('命名规范提示')"
          :description="t('开启后，需填写流水线命名规范提示说明。规范提示说明将展示在「创建流水线」页面进行提示。')"
        >
          <bk-switcher
            v-model="projectData.properties.enablePipelineNameTips"
            size="large"
            theme="primary"
          />
          <bk-input
            class="textarea"
            v-show="projectData.properties.enablePipelineNameTips"
            v-model="projectData.properties.pipelineNameFormat"
            :placeholder="t('请输入流水线命名规范提示说明')"
            :rows="3"
            :maxlength="200"
            type="textarea"
          >
          </bk-input>
        </bk-form-item>
      </div>
    </div>
  </bk-form>

  <bk-dialog
    :title="t('设置项目最大可授权人员范围')"
    width="900"
    size="large"
    dialog-type="show"
    :is-show="showDialog"
    @closed="() => showDialog = false"
  >
    <IAMIframe
      ref="iframeRef"
      class="member-iframe"
      path="add-member-boundary"
      :query="{
        search_sence: 'add'
      }"
    />
  </bk-dialog>
  <bk-sideslider
      v-model:isShow="pipelineSideslider"
      :title="`${t('切换变量语法风格影响的流水线列表')}(${t('共X个', [pipelinePagination.count])})`"
      :width="640"
      renderDirective="if"
      class="pipeline-sideslider"
    >
      <bk-loading :loading="isLoading">
        <bk-table
          :max-height="600"
          :data="pipelineList"
          show-overflow-tooltip
          :pagination="pipelinePagination"
          remote-pagination
          @page-limit-change="pageLimitChange"
          @page-value-change="pageValueChange"
          >
          <bk-table-column
            :label="t('流水线名称')"
            prop="pipelineName"
          >
            <template #default="{row}">
              <span class="text-link edit-line" @click="handleToPipeline(row)">{{ row.pipelineName }}</span>
            </template>
          </bk-table-column>
        </bk-table>
      </bk-loading>
    </bk-sideslider>
</template>

<style lang="postcss" scoped>
  .textarea {
    :deep(textarea) {
        width: auto;
    }
    margin-top: 10px;
  }
  :deep(.bk-form-label) {
    font-size: 12px;
  }
  .logo-upload-tip {
    font-size: 12px;
    color: #979BA5;
  }
  .edit-line {
    cursor: pointer;
  }
  .member-iframe {
    height: 600px;
  }
  .bk-dropdown-box {
    width: 200px;
    margin-right: 12px;
    display: inline-block;
    vertical-align: middle;
  }
  .authSecrecy-item {
    border-bottom: 1px dashed #979ba5;
  }
  .error-tips {
    color: #ff5656;
    font-size: 12px;
    position: absolute;
    top: 26px;
  }
  .text-link {
    font-size: 12px;
    color: #3c96ff;
  }
</style>

<style lang="postcss">
  .dark {
    background: #26323d !important;
  }
  .bk-form-error {
    white-space: nowrap;
  }
  .project-tab {
    width: 100%;
    padding: 20px 30px;
    background-color: #fff;
    box-shadow: 0 2px 2px 0 #00000026;
    &.advanced {
      margin-bottom: 24px;
    }
    .title {
      margin-bottom: 16px;
      font-weight: 700;
      font-size: 14px;
      color: #63656E;
    }
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
  .alarm {
    color: #4D4F56;
    font-size: 12px;
    font-weight: 700;
  }
  .tip-script {
    display: flex;
    flex-direction: column;
    justify-content: center;
    width: 200px;
    height: 60px;
    line-height: 22px;
    padding-left: 20px;
    margin: 10px 0;
    background: #313238;
    font-size: 14px;
    color: #F8B64F;
  }
  .text-white {
    color: #F5F7FA;
  }
  .text-echo {
    color: #699DF4;
    margin-right: 10px;
  }
  .text-a {
    color: #65B6C3;
  }
  .tip-blue {
    cursor: pointer;
    color: #3A84FF; 
    font-weight: 700;
  }
  .confirm-switch {
    display: flex;
    flex-direction: column;
    align-items: start;
    margin-top: 16px;
  }
  .confirm-tit {
    color: #333333;
    font-size: 14px;
    margin-bottom: 10px;
  }
  .confirm-input {
    width: 576px;
    height: 32px;
    color: #63656e;
    font-size: 14px;
    border: 1px solid #C4C6CC;
    outline: none;
    box-sizing: border-box;
    padding: 5px;
  }
  .confirm-input:focus {
    border-color: #3a84ff;
  }
  .confirm-input::placeholder {
    color: #C4C6CC;
  }
  .pipeline-sideslider .bk-modal-content{
    padding: 16px 24px;
  }
</style>
