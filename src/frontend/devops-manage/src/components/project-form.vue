<script setup lang="ts">
import http from '@/http/api';
import { Message, Popover } from 'bkui-vue';
import {
  EditLine,
} from 'bkui-vue/lib/icon';
import {
  computed,
  getCurrentInstance,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  shallowRef,
  watch,
} from 'vue';
import { useI18n } from 'vue-i18n';
import IAMIframe from './IAM-Iframe';
const {
  t,
} = useI18n();
const emits = defineEmits(['change', 'approvedChange', 'initProjectForm', 'productIdChange']);

interface Dept {
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
});

const logoFiles = computed(() => {
  const { logoAddr } = projectData.value;
  const files: {url: string}[] = [];
  if (logoAddr) {
    files.push({
      url: logoAddr,
    });
  }
  return files;
});
const englishNameReg = /^[a-z][a-z0-9-]{1,32}$/;
const inited = ref(false);
const projectForm = ref<any>(null);
const iframeRef = ref(null);
const operationalList = ref([]);
const vm = getCurrentInstance();
const rules = {
  englishName: [
    {
      validator: value => props.type !== 'apply' || englishNameReg.test(value),
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

const orgValue = ref<string[]>([]);

const deptLoading = ref({
  dept: false,
  center: false,
  product: false,
});

const centerList = ref<Dept[]>([]);
const deptMap = shallowRef(new Map());

const showDialog = ref(false);

const orgTree = shallowRef<Dept[]>([]);

const getDepartment = async ({ id, type = 'dept' }: Partial<Dept>, resolve?) => {
  try {
    if (!id) return [];
    const res = await http.getOrganizations({
      type,
      id,
    });
    const parent = deptMap.value.get(id);
    res.forEach((i) => {
      if (deptMap.value.has(i.id)) return;
      deptMap.value.set(i.id, i);
    });
    if (parent) {
      parent.children = res.map((i) => {
        if (deptMap.value.has(i.id)) return deptMap.value.get(i.id);
        return i;
      });
      deptMap.value.set(id, parent);
    }
    resolve?.(res);
    return res;
  } catch (err: any) {
    Message({
      message: err.message || err,
      theme: 'danger',
    });
  }
};

const setProjectDeptProp = (dept: Dept) => {
  if (!dept) return;
  const { id, name, type } = dept;
  projectData.value[`${type}Id`] = id;
  projectData.value[`${type}Name`] = name;
};

const handleChangeDept = async (deptPath) => {
  if (!inited.value) return;  // 防止初始化时触发
  [{
    type: 'businessLine',
    id: '',
    name: '',
  }, {
    type: 'center',
    id: '',
    name: '',
  }].forEach(setProjectDeptProp);

  deptPath.forEach((deptId: string) => {
    setProjectDeptProp(deptMap.value.get(deptId));
  });
  handleChangeForm();
  if (!deptPath.length) {
    ['bg', 'businessLine', 'dept', 'center'].forEach((type) => {
      projectData.value[`${type}Id`] = '';
      projectData.value[`${type}Name`] = '';
    });
    return;
  }
  const deptId = deptPath[deptPath.length - 1];
  const lastOne = deptMap.value.get(deptId);
  if (Array.isArray(lastOne?.children) && lastOne?.children.length > 0) {
    centerList.value = lastOne.children;
  } else {
    centerList.value = await getDepartment({
      id: deptId,
      type: 'center',
    });
  }
};

const handleChangeCenter = (id: any) => {
  handleChangeForm();
  const name = centerList.value.find(i => i.id === id)?.name;
  projectData.value.centerName = name ?? '';
};

const fetchDepartmentList = async (deptInfos: Dept[]) => {
  deptLoading.value.dept = true;
  deptInfos.forEach((item, index) => {
    deptMap.value.set(item.id, {
      ...item,
      parentId: index > 0 ? (deptInfos[index - 1]?.id ?? '0') : '-1',
      ...(index < deptInfos.length - 1 ? {
        children: [],
      } : {
        leaf: true,
      }),
    });
  });
  const depts = deptInfos.slice(0, -1);

  await Promise.all(depts.map(dept => getDepartment({
    id: dept.id,
    type: 'dept',
  })));
  orgTree.value = Array.from(deptMap.value.values()).filter(i => i.parentId === '0');
  orgValue.value = deptInfos.slice(1).map((i) => {
    setProjectDeptProp(i);
    return i.id;
  });

  nextTick(() => {
    inited.value = true;
  });
  deptLoading.value.dept = false;
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
          (vm?.refs?.iframeRef as any)?.$el?.firstElementChild?.contentWindow?.postMessage?.(
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
    ...(projectData.value.businessLineId ? [{
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
  } else { // 申请创建项目
    const res = await http.getUserDetail();
    deptInfos = res.deptInfos;
    centerId = res.centerId;
    centerName = res.centerName;
  }
  console.log(deptInfos, 123);
  if (centerId) {
    setProjectDeptProp({
      type: 'center',
      id: centerId,
      name: centerName,
    });
  }
  centerList.value = await getDepartment({
    id: deptInfos[deptInfos.length - 1].id,
    type: 'center',
  });
  return deptInfos;
};

const showMemberDialog = () => {
  showDialog.value = true;
};

const fetchOperationalList = async () => {
  deptLoading.value.product = true;
  await http.getOperationalList().then((res) => {
    operationalList.value = res.map(i => ({
      ...i,
      value: i.ProductId,
      label: i.ProductName,
      id: i.ProductId,
    }));
    deptLoading.value.product = false;
  });
};

const validateProjectNameTips = ref('');
watch(() => projectData.value.projectName, (val) => {
  if (props.type === 'apply' && val) {
    http.validateProjectName(val)
      .then(() => {
        validateProjectNameTips.value = '';
      })
      .catch(() => {
        projectForm.value?.clearValidate();
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
  if (props.type === 'apply' && val && englishNameReg.test(val)) {
    http.validateEnglishName(val)
      .then(() => {
        validateEnglishNameTips.value = '';
      })
      .catch(() => {
        projectForm.value?.clearValidate();
        validateEnglishNameTips.value = t('项目ID已存在');
      });
  } else if (!val || !englishNameReg.test(val)) {
    validateEnglishNameTips.value = '';
  }
}, {
  deep: true,
});

watch(() => [projectData.value.authSecrecy, projectData.value.subjectScopes], () => {
  projectForm.value.validate();
  emits('approvedChange', true);
}, {
  deep: true,
});

watch(() => projectData.value.productId, (id) => {
  emits('productIdChange', {
    id,
    list: operationalList.value,
  });
}, {
  deep: true,
});

onMounted(async () => {
  const deptInfos = await fetchUserDetail();
  await Promise.all([
    fetchDepartmentList(deptInfos),
    fetchOperationalList(),
  ]);
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
    :label-width="160"
  >
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
    <bk-form-item :label="t('项目类型')" property="projectType" :required="true">
      <bk-select
        v-model="projectData.projectType"
        :placeholder="t('请选择项目类型')"
        name="center"
        searchable
        @change="handleChangeForm"
      >
        <bk-option
          v-for="projectType in projectTypeList"
          :value="projectType.id"
          :key="projectType.id"
          :label="projectType.name"
        />
      </bk-select>
    </bk-form-item>
    <bk-form-item :label="t('项目所属组织')" property="bgId" :required="true">
      <div class="bk-dropdown-box">
        <bk-cascader
          :list="orgTree"
          v-model="orgValue"
          filterable
          is-remote
          :placeholder="t('请选择部门')"
          :loading="deptLoading.dept"
          :remote-method="getDepartment"
          @change="handleChangeDept"
        />
      </div>
      <div class="bk-dropdown-box">
        <bk-select
          v-model="projectData.centerId"
          :placeholder="t('请选择中心')"
          name="center"
          :loading="deptLoading.center"
          filterable
          @change="handleChangeCenter"
        >
          <bk-option
            v-for="center in centerList"
            :value="center.id"
            :key="center.id"
            :label="center.name"
          />
        </bk-select>
      </div>
    </bk-form-item>
    <bk-form-item
      :label="t('项目所属运营产品')"
      property="productId"
      :required="true"
      :description="t('公司OBS结算业务的运营产品，1个项目仅支持关联1个运营产品，多个项目可关联到同一运营产品')"
    >
      <bk-select
        class="product-select"
        v-model="projectData.productId"
        :placeholder="t('请选择所属运营产品')"
        :scroll-height="160"
        name="center"
        filterable
        enable-virtual-render
        :list="operationalList"
        :input-search="false"
        :loading="deptLoading.product"
        searchable
        @change="handleChangeForm"
      >
      </bk-select>
    </bk-form-item>
    <bk-form-item
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
    <div>
      <slot></slot>
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
</template>

<style lang="postcss" scoped>
  .textarea {
    :deep(textarea) {
        width: auto;
    }
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
    height: 100%;
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
</style>
