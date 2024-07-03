<template>
  <div>
    <div class="permission-wrapper">
      <ul class="aside">
        <li
          class="aside-item"
          :class="activeIndex == index ? 'aside-active' : ''"
          v-for="(item, index) in permissionList"
          :key="index"
          @click="handleAsideClick(item, index)"
        >
          <p>
            {{ item.label }}
          </p>
        </li>
      </ul>
      <div class="content">
        <div class="content-btn">
          <bk-button @click="handleReset">{{ t('批量重置') }}</bk-button>
          <div class="filter-bar">
            <bk-date-picker
              v-model="dateTimeRange"
              type="datetimerange"
              @clear="handleClearDaterange"
              @change="handleChangeDaterange"
              @pick-success="handlePickSuccess"
            />
            <bk-search-select
              v-model="searchValue"
              :data="searchData"
              unique-select
              max-height="32"
              class="content-btn-search"
              :placeholder="filterTips"
              :key="searchName"
            />
          </div>
        </div>
        <bk-loading class="content-table" :loading="isLoading">
          <bk-table
            ref="refTable"
            checked
            :data="tableData"
            :columns="columns"
            height="100%"
            show-overflow-tooltip
            :key="resourceType"
            :scroll-loading="isScrollLoading"
            @select-all="handleSelectAll"
            @selection-change="handleSelectionChange"
            @scroll-bottom="getTableList"
          >
            <template #prepend>
              <div v-if="isSelectAll" class="prepend">
                {{ t('已选择全量数据X条', [totalCount]) }}
                <span @click="handleClear">{{ t('清除选择') }}</span>
              </div>
              <div v-else-if="selectList.length" class="prepend">
                {{ t('已选择X条数据，', [selectList.length]) }}
                <span @click="handleSelectAllData"> {{ t('选择全量数据X条', [totalCount]) }}</span> 
                &nbsp; | &nbsp;
                <span @click="handleClear">{{ t('清除选择') }}</span> 
              </div>
            </template>
          </bk-table>
        </bk-loading> 
      </div>
    </div>
    <bk-dialog
      :is-show="showResetDialog"
      :key="resourceType"
      :theme="'primary'"
      :width="640"
      :title="t('批量重置')"
      @closed="dialogClose"
    >
      <div class="dialog">
        <bk-tag radius="20px" class="tag">{{ t('已选择X个XX', [isSelectAll ? totalCount : selectList.length, searchName]) }}</bk-tag>
        <bk-form
          ref="formRef"
          :model="resetFormData"
          class="reset-form"
        >
          <bk-form-item
            required
            :label="t('重置授权人')"
            property="name"
            labelWidth=""
          >
            <bk-input
              v-model="resetFormData.name"
              :placeholder="t('输入授权人，按回车进行校验')"
              clearable
              :disabled="dialogLoading"
              @enter="handleCheckReset()"
            />
          </bk-form-item>
        </bk-form>
  
        <div v-if="isResetFailure" class="check-failure">
          <div class="failed-tips">
            <div class="manage-icon manage-icon-warning-circle-fill warning-icon"></div>
            <i18n-t keypath="检测到以下X项授权将无法重置，请前往处理或继续重置其余代码库授权" tag="div">
              <span style="color: #ef9b30; padding: 0 5px; font-weight: 700;">{{ failedCount }}</span>
              <span style="color: #63656E; font-weight: 700;">{{ t('前往处理') }}</span>
              <span style="color: #63656E; font-weight: 700;">{{ t('继续重置其余') }}</span>
              <span style="color: #63656E; font-weight: 700;">{{ searchName }}</span>
              <span style="color: #63656E; font-weight: 700;">{{ t('授权') }}</span>
            </i18n-t>
          </div>
          <bk-table
            ref="resetTable"
            :data="resetTableData"
            :border="['outer', 'row']"
            show-overflow-tooltip
          >
            <bk-table-column :label="t('代码库')" prop="resourceName" />
            <bk-table-column :label="t('失败原因')" prop="handoverFailedMessage" />
          </bk-table>
        </div>

        <div v-else-if="isResetSuccess" class="check-success">
          <Success class="check-success-icon"></Success>
          <span>{{ t('授权校验通过') }}</span>
        </div>
      </div>
      <template #footer>
        <bk-button
          class="mr5"
          theme="primary"
          :loading="dialogLoading"
          :disabled="disabledResetBtn"
          @click="confirmReset">
            {{ t('重置') }}
        </bk-button>
        <bk-button
          :loading="dialogLoading"
          @click="dialogClose">
          {{ t('取消') }}
        </bk-button>
      </template>
    </bk-dialog>
  </div>
</template>

<script setup name="PermissionManage">
import http from '@/http/api';
import { useI18n } from 'vue-i18n';
import { ref, onMounted, computed, h, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { convertTime } from '@/utils/util'
import { Message } from 'bkui-vue';
import { Success } from 'bkui-vue/lib/icon';

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

const tableData = ref([]);
const resetTableData = ref([]);
const activeIndex = ref(0);
const formRef = ref(null);
const refTable = ref(null);
const selectList = ref([]);
const searchValue = ref([]);
const isLoading = ref(true);
const showResetDialog = ref(false);
const dialogLoading = ref(false);
const isResetFailure = ref(false);
const isResetSuccess = ref(false);
const isScrollLoading = ref(false);
const page = ref(1);
const pageSize = ref(20);
const projectId = computed(() => route.params?.projectCode);
const resourceType = ref('repertory');
const hasNext = ref(true);
const totalCount = ref(0);
const isSelectAll = ref(false);  // 选择全量数据
const dateTimeRange = ref(['', '']);
const daterangeCache = ref(['', '']);
const disabledResetBtn = ref(true);
const failedCount = ref(0);
const searchName = computed(() => {
  const nameMap = {
    'pipeline': t('流水线'),
    'env_node': t('部署节点'),
    'repertory': t('代码库')
  }
  return nameMap[resourceType.value]
});
const filterTips = computed(() => {
  return searchData.value.map(item => item.name).join(' / ');
});

const resetParams = computed(() => {
  const resourceAuthorizationHandoverList = !isSelectAll.value && selectList.value.map(item => {
    return {
      projectCode: projectId.value,
      resourceType: resourceType.value,
      resourceName: item.resourceName,
      resourceCode: item.resourceCode,
      handoverFrom: item.handoverFrom,
      handoverTo: resetFormData.value.name,
    }
  })
  const params = {
    projectCode: projectId.value,
    resourceType: resourceType.value,
    fullSelection: isSelectAll.value,
    handoverChannel: 'MANAGER',
    resourceAuthorizationHandoverList: isSelectAll.value ? [] : resourceAuthorizationHandoverList,
  }
  if (isSelectAll.value) {
    params.handoverTo = resetFormData.value.name;
    params.greaterThanHandoverTime = dateTimeRange.value[0];
    params.lessThanHandoverTime = dateTimeRange.value[1];
    Object.assign(params, filterQuery.value)
  }
  return params;
})
const permissionList = ref([
  {
    name: 'codeBase',
    label: t('代码库授权'),
    resourceType: 'repertory'
  },
  {
    name: 'pipeline',
    label: t('流水线执行授权'),
    resourceType: 'pipeline'
  },
  {
    name: 'deployNode',
    label: t('部署节点授权'),
    resourceType: 'env_node'
  },
]);
const searchData = ref([
  {
    name: searchName,
    id: 'resourceName', 
  },
  {
    name: t('授权人'),
    id: 'handoverFrom',
  },
]);
const columns = ref([
  {
    type: "selection",
    width: 30,
    align: 'center'
  },
  {
    label: searchName,
    field: "resourceName",
  },
  {
    label: t('授权人'),
    field: "handoverFrom",
  },
  {
    label: t('授权时间'),
    field: "handoverTime",
    render ({ cell, row }) {
      return h(
        'span',
        [
          convertTime(cell)
        ]
      );
    },
  },
])
const resetFormData = ref({
  name: ''
})
const filterQuery = computed(() => {
  return searchValue.value.reduce((query, item) => {
      query[item.id] = item.values?.map(value => value.id).join(',');
      return query;
  }, {})
})
watch(() => searchValue.value, (val, oldVal) => {
  page.value = 1;
  isSelectAll.value = false;
  hasNext.value = true;
  getTableList();
});

onMounted(() => {
  init();
});

function init () {
  page.value = 1;
  tableData.value = [];
  hasNext.value = true;
  searchValue.value = [];
  isSelectAll.value = false;
  selectList.value = [];
  dateTimeRange.value = [];
};
/**
 * 获取列表数据
 */
async function getTableList () {
  if (!hasNext.value) return;

  if (page.value === 1) {
    isLoading.value = true;
    refTable.value.clearSelection();
    selectList.value = [];
    tableData.value = [];
  } else {
    isScrollLoading.value = true;
  }
  try {
    const res = await http.getResourceAuthList(projectId.value, {
      page: page.value,
      pageSize: pageSize.value,
      projectCode: projectId.value,
      resourceType: resourceType.value,
      ...filterQuery.value,
      greaterThanHandoverTime: dateTimeRange.value[0],
      lessThanHandoverTime: dateTimeRange.value[1],
    });
    tableData.value = [...tableData.value, ...res.records];
    page.value += 1;
    hasNext.value = res.count > tableData.value.length;
    totalCount.value = res.count;

    isLoading.value = false;
    isScrollLoading.value = false;
  } catch (e) {
    isLoading.value = false;
    isScrollLoading.value = false;
    console.error(e);
  }
}
/**
 * aside点击事件
 */
function handleAsideClick(item, index) {
  if (activeIndex.value === index) return;
  activeIndex.value = index;
  resourceType.value = item.resourceType;
  init();
};
/**
 * 批量重置
 */
function handleReset() {
  if(!selectList.value.length) {
    Message({
      theme: 'error',
      message: t('请先选择数据'),
    });
    return;
  }
  showResetDialog.value = true;
  resetFormData.value.name = '';
  setTimeout(() => {
    formRef.value?.clearValidate();
  });
}
/**
 * 当前页全选事件
 */
function handleSelectAll(){
  selectList.value = refTable.value.getSelection();
}
/**
 * 多选事件
 * @param val
 */
function handleSelectionChange() {
  isSelectAll.value = false;
  selectList.value = refTable.value.getSelection();
};
/**
 * 全量数据选择
 */
function handleSelectAllData() {
  if (!isSelectAll.value && selectList.value.length !== tableData.value.length) {
    refTable.value.toggleAllSelection();
  }
  isSelectAll.value = true;

  selectList.value = tableData.value.map((item) => item.id);
}
/**
 * 清除选择
 */
function handleClear() {
  refTable.value.clearSelection();
  isSelectAll.value = false;
  selectList.value = [];
}
/**
 * 弹窗关闭
 */
function dialogClose() {
  showResetDialog.value = false;
  isResetFailure.value = false;
  isResetSuccess.value = false;
  isSelectAll.value = false;
  resetFormData.value.name = '';
  formRef.value?.clearValidate();
}

async function handleCheckReset () {
  if (!resetFormData.value.name) return
  try {
    const res = await http.resetAuthorization(projectId.value, {
      ...resetParams.value,
      preCheck: true
    }, resourceType.value)
    
    failedCount.value = res['FAILED']?.length || 0
    if (failedCount) {
      resetTableData.value = res['FAILED'].splice(0,6)
    }
    isResetFailure.value = !!failedCount.value;
    isResetSuccess.value = !failedCount.value;

    disabledResetBtn.value = failedCount.value === selectList.value.length;
  } catch (e) {
    console.error(e)
  }
}
/**
 * 弹窗提交
 */
function confirmReset() {
  dialogLoading.value = true;
  formRef.value?.validate().then(async () => {
    try {
      await http.resetAuthorization(projectId.value, resetParams.value, resourceType.value)
      
      dialogLoading.value = false;
      showResetDialog.value = false;

      Message({
        theme: 'success',
        message: t('授权已成功重置', [searchName.value]),
      });

      page.value = 1;
      getTableList();
      
      isSelectAll.value = false;
      hasNext.value = true;
      disabledResetBtn.value = true;
      isResetSuccess.value = false;
      isResetFailure.value = false;
    } catch (e) {
      console.error(e)
    }
  })
};

function handleChangeDaterange (date) {
  const startTime = new Date(date[0]).getTime() || ''
  const endTime = new Date(date[1]).getTime() || ''
  daterangeCache.value = [startTime, endTime]
}
function handleClearDaterange () {
  dateTimeRange.value = ['', '']
  page.value = 1;
  hasNext.value = true;
  getTableList();
}
function handlePickSuccess () {
  dateTimeRange.value = daterangeCache.value;
  page.value = 1;
  hasNext.value = true;
  getTableList();
}

</script>

<style scoped lang="scss">
.permission-wrapper {
  width: 100%;
  height: 100%;
  display: flex;

  .aside {
    width: 240px;
    height: 100%;
    padding-top: 8px;
    background: #FAFBFD;
    box-shadow: 1px 0 0 0 #DCDEE5;

    .aside-item {
      height: 40px;
      font-size: 14px;
      color: #63656E;
      line-height: 40px;
      cursor: pointer;

      p {
        margin-left: 22px;
      }
    }

    .aside-active {
      background: #E1ECFF;
      color: #3A84FF;
    }
  }

  .content {
    flex: 1;
    padding: 24px;

    .content-btn {
      display: flex;
      justify-content: space-between;
      margin-bottom: 17px;

      &-search {
        width: 500px;
        margin-left: 10px;
      }
      .filter-bar {
        display: flex;
        align-items: center;
      }
    }

    .content-table{
      border: 1px solid #DCDEE5;
      height: calc(100vh - 158px);

      .prepend{
        width: 100%;
        height: 32px;
        line-height: 32px;
        background: #F0F1F5;
        text-align: center;
        box-shadow: 0 -1px 0 0 #DCDEE5;

        span{
          font-family: MicrosoftYaHei;
          font-size: 12px;
          color: #3A84FF;
          letter-spacing: 0;
          line-height: 20px;
          cursor: pointer;
        }
      }
    }
  }
}
.dialog{
  .tag{
    padding: 16px;
    font-size: 12px;
    color: #63656E;
    line-height: 20px;
    margin-bottom: 16px;
  }

  .check-failure{
    border-top: 1px solid #DCDEE5;
    margin-bottom: 25px;

    p{
      margin: 8px 0 18px 0;
      color: #63656e;
      font-size: 12px;

      span{
        font-weight: 700;
      }
    }
    .failed-tips {
      display: flex;
      align-items: center;
      margin: 10px 0;
    }
    .warning-icon {
      font-size: 18px;
      color: #ff9c01;
      margin-right: 5px;
    }
  }
  .check-success {
    display: flex;
    align-items: center;
  }
}
.check-success-icon {
  color: #2DCB56;
  margin-right: 5px;
}
.reset-form {
  .bk-form-item {
    margin-bottom: 15px !important;
  }
}
</style>