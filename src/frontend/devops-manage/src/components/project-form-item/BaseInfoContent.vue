<template>
  <div>
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
        :rows="3"
        :maxlength="255"
        :placeholder="t('请输入项目描述')"
        @change="handleChangeForm"
        type="textarea"
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
    <bk-form-item :label="t('项目类型')" property="projectType" required>
      <bk-select
        class="project-select"
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
      :label="t('项目成本归属')"
      :required="true"
    >
      <div class="cost-attribution-box">
        <div class="cost-description" v-if="showKpiCode">{{ t('用于货币化结算场景，可在 tea.woa.com 查看业务信息。') }}</div>
        <bk-form-item
          property="productId"
          :rules="[{ required: true, message: t('请选择所属运营产品'), trigger: 'change' }]"
        >
          <template class="cost-item">
            <span class="cost-label">{{ t('运营产品') }}</span>
            <bk-select
              class="cost-select"
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
              @change="handleChangeProduct"
            />
          </template>
        </bk-form-item>
        <bk-form-item
          property="kpiCode"
          :rules="kpiCodeRules"
          v-if="showKpiCode"
        >
          <div class="cost-item">
            <span class="cost-label">{{ t('KPI 代码') }}</span>
            <bk-select
              class="cost-select"
              v-model="projectData.kpiCode"
              :placeholder="t('所属的 KPI 业务代码')"
              :scroll-height="160"
              name="kpiCode"
              filterable
              enable-virtual-render
              :list="KPIList"
              :loading="deptLoading.kpiCode"
              :searchPlaceholder="t('请输入/修改关键字重新搜索数据')"
              :noDataText="t('请输入/修改关键字重新搜索数据')"
              searchable
              :remoteMethod="handleKpiCodeSearch"
              @change="handleChangeKpiCode"
              @clear="handleKpiCodeClear"
            />
          </div>
        </bk-form-item>
      </div>
    </bk-form-item>
  </div>
</template>

<script setup lang="ts" name="BaseInfoContent">
import http from '@/http/api';
import { ref, computed, watch, shallowRef, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import { Message, Popover } from 'bkui-vue';
import { Dept } from "@/components/project-form.vue";

interface OperationalProduct {
  ProductId: string;
  ProductName: string;
  icosProductCode?: string;
  icosProductName?: string;
  value: string;
  label: string;
  id: string;
}

interface KPIItem {
  value: string;
  label: string;
}

const { t } = useI18n();
const props = defineProps({
  data: {
    type: Object,
    required: true
  },
  type: String,
  curDeptInfo: Array
});
const emits = defineEmits(['handleChangeForm', 'clearValidate', 'setProjectDeptProp', 'updateKpiCodeConfig']);

const projectData = ref(props.data);
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
const englishNameReg = /^[a-z][a-z0-9-]{1,32}$/;
const validateProjectNameTips = ref('');
const validateEnglishNameTips = ref('');

const orgValue = ref<string[]>([]);
const operationalList = ref<OperationalProduct[]>([]);
const KPIList = ref<KPIItem[]>([]);
const orgTree = shallowRef<Dept[]>([]);
const deptMap = shallowRef(new Map());
const inited = ref(false);
const centerList = ref<Dept[]>([]);
const deptLoading = ref({
  dept: false,
  center: false,
  product: false,
  kpiCode: false,
});

// KPI代码字段控制
const showKpiCode = ref(false)
// 组件初始化状态标记
const isComponentInitialized = ref(false)

// KPI代码验证规则
const kpiCodeRules = computed(() => {
  if (!showKpiCode.value) {
    return [];
  }
  return [
    {
      required: true,
      message: t('请选择KPI代码'),
      trigger: 'change'
    }
  ];
});

// 设置KPI字段状态并通知父组件
function setKpiCodeState(isRequired: boolean, shouldTriggerFormChange = true) {
  showKpiCode.value = isRequired
  
  // 统一通知父组件KPI配置变化
  emits('updateKpiCodeConfig', isRequired)
  
  // 如果不需要KPI字段，清空相关数据
  if (!isRequired) {
    projectData.value.kpiCode = ''
    projectData.value.kpiName = ''
    KPIList.value = [];
  }
  
  // 只有在非初始化时才触发表单变更
  if (shouldTriggerFormChange) {
    handleChangeForm()
  }
}

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

watch(() => projectData.value.projectName, (val) => {
  if (props.type === 'apply' && val) {
    http.validateProjectName(val)
      .then(() => {
        validateProjectNameTips.value = '';
      })
      .catch(() => {
        emits('clearValidate');
        validateProjectNameTips.value = t('项目名称已存在');
      });
  } else if (!val) {
    validateProjectNameTips.value = '';
  }
}, {
  deep: true,
});

watch(() => projectData.value.englishName, (val) => {
  if (props.type === 'apply' && val && englishNameReg.test(val)) {
    http.validateEnglishName(val)
      .then(() => {
        validateEnglishNameTips.value = '';
      })
      .catch(() => {
        emits('clearValidate');
        validateEnglishNameTips.value = t('项目ID已存在');
      });
  } else if (!val || !englishNameReg.test(val)) {
    validateEnglishNameTips.value = '';
  }
}, {
  deep: true,
});

watch(()=>props.curDeptInfo, (newVal)=>{
  if (newVal) {
    fetchCenterList();
    fetchDepartmentList(newVal)
  }
}, { immediate:true,deep:true })

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
    // 标记组件初始化完成
    isComponentInitialized.value = true
    // 在组织数据设置完成后检查KPI显示状态
    checkKpiCodeVisibility(true)
  });
  deptLoading.value.dept = false;
};


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
    if (parent && !parent.leaf) {
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

async function fetchCenterList () {
  let deptInfos: Dept[] = props.curDeptInfo as Dept[];

  centerList.value = await getDepartment({
    id: deptInfos[deptInfos.length - 1].id,
    type: 'center',
  });
  if (deptInfos.length && deptInfos[1].type === 'bg') {
    fetchOperationalList(deptInfos[1].name);
  }
  
  // 延迟检查KPI代码显示状态，确保组织数据已经设置完成
  nextTick(() => {
    checkKpiCodeVisibility(true)
  });
}

async function fetchOperationalList (bgName) {
  if (!bgName) return;
  deptLoading.value.product = true;
  const res = await http.getOperationalList(bgName);
  operationalList.value = res.map(i => ({
    ...i,
    value: i.ProductId,
    label: i.ProductName,
    id: i.ProductId,
  }));
  deptLoading.value.product = false;

  // 如果已经选了运营产品，且kpiCode不存在，那么给KPI代码填充默认值
  if (projectData.value.productId && !projectData.value.kpiCode) {
    handleChangeProduct(projectData.value.productId);
  } else if (projectData.value.kpiCode) {
    // 如果kpi存在，那么调用kpi列表接口，正确回显kpi代码
    fetchApilist(projectData.value.kpiName)
  }
};

function setProjectDeptProp (dept: Dept) {
 emits('setProjectDeptProp', dept)
}

async function handleChangeDept (deptPath) {
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
    const dept = deptMap.value.get(deptId);
    if (dept.type === 'bg') {
      fetchOperationalList(dept.name);
      projectData.value.productId = ''
      projectData.value.kpiCode = ''
      projectData.value.kpiName = ''
    }
    setProjectDeptProp(deptMap.value.get(deptId));
  });
  handleChangeForm();
  if (!deptPath.length) {
    ['bg', 'businessLine', 'dept', 'center'].forEach((type) => {
      projectData.value[`${type}Id`] = '';
      projectData.value[`${type}Name`] = '';
    });
    
    // 清空运营产品和KPI相关数据
    projectData.value.productId = '';
    
    // 检查KPI代码显示状态
    checkKpiCodeVisibility()
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
  
  checkKpiCodeVisibility()
};

function handleChangeCenter (id: any) {
  handleChangeForm();
  const name = centerList.value.find(i => i.id === id)?.name;
  projectData.value.centerName = name ?? '';
  
  checkKpiCodeVisibility();
};

function handleChangeProduct (productId: any) {
  if (productId === '') {
    handleKpiCodeClear()
  }
  const selectedProduct = operationalList.value.find(i => i.ProductId === productId);
  projectData.value.productName = selectedProduct?.ProductName || '';
  if (selectedProduct?.icosProductCode && selectedProduct?.icosProductName) {
    projectData.value.kpiCode = selectedProduct.icosProductCode || '';
    projectData.value.kpiName = selectedProduct.icosProductName || '';
    fetchApilist(projectData.value.kpiName)
  }
  handleChangeForm();
};

function handleKpiCodeClear() {
  projectData.value.kpiCode = '';
  projectData.value.kpiName = '';
  handleChangeForm();
  KPIList.value = []
}

function handleKpiCodeSearch(value) {
  if (value === '') {
    KPIList.value = []
  } else {
    fetchApilist(value)
  }
}

function handleChangeKpiCode (kpiCode: any) {
  const selectedKpi = KPIList.value.find(i => i.value === kpiCode);
  if (selectedKpi?.label) {
    projectData.value.kpiName = selectedKpi.label;
  }
  handleChangeForm();
};

function handleChangeForm() {
  // 只有在组件初始化完成后才触发表单变更事件
  if (isComponentInitialized.value) {
    emits('handleChangeForm')
  }
}

async function handleUploadLogo (res: any) {
  emits('handleChangeForm');
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

async function fetchApilist(kpiName) {
  try {
    deptLoading.value.kpiCode = true;
    const res = await http.getKpiCodeList(kpiName)
    KPIList.value = (res || []).map(i => ({
      value: i?.product_code,
      label: i?.product_name,
    }));
  } catch (err: any) {
    Message({
      theme: 'danger',
      message: err.message || err,
    });
  } finally {
    deptLoading.value.kpiCode = false;
  }
}

// 检查是否需要显示KPI代码字段
async function checkKpiCodeVisibility(isInitialLoad = false) {
  try {
    const orgParams = getOrganizationParams()
    
    // 如果没有组织信息，不显示KPI字段
    if (Object.keys(orgParams).length === 0) {
      setKpiCodeState(false, !isInitialLoad)
      return
    }
    
    const needMonetization = await http.checkNeedMonetization(orgParams)
    setKpiCodeState(needMonetization, !isInitialLoad)
    
    // 只有在非初始化时才触发表单变更事件
    if (!isInitialLoad) {
      handleChangeForm()
    }
  } catch (err: any) {
    console.log('检查KPI代码显示状态失败:', err)
    setKpiCodeState(false, !isInitialLoad)
  }
}

// 获取组织信息参数
function getOrganizationParams() {
  const params: any = {}
  
  if (projectData.value.bgId) params.bgId = projectData.value.bgId
  if (projectData.value.businessLineId) params.businessLineId = projectData.value.businessLineId
  if (projectData.value.deptId) params.deptId = projectData.value.deptId
  if (projectData.value.centerId) params.centerId = projectData.value.centerId
  return params
}

</script>

<style lang="scss" scoped>
.error-tips {
  color: #ff5656;
  font-size: 12px;
  position: absolute;
  top: 26px;
}
.bk-dropdown-box {
  width: 200px;
  margin-right: 12px;
  display: inline-block;
  vertical-align: middle;
}
.logo-upload-tip {
  font-size: 12px;
  color: #979BA5;
}
.authSecrecy-item {
  border-bottom: 1px dashed #979ba5;
}
.cost-attribution-box {
  width: 100%;
  padding: 12px 24px;
  background-color: #f6f7fa;
  border-radius: 2px;
  .cost-description {
    font-size: 14px;
    color: #a8abae;
  }
  .cost-item {
    display: flex;
    align-items: center;
    font-size: 14px;
    margin-top: 10px;
    &:not(:last-child) {
      margin-bottom: 12px;
    }
    .cost-label {
      display: inline-block;
      padding: 0 12px;
      height: 32px;
      margin-top: 1px;
      border: 1px solid #c4c6cc;
      border-radius: 2px;
      border-right: 0;
    }
    .cost-select {
      width: 100px;
      flex: 1;
    }
  }
}
</style>