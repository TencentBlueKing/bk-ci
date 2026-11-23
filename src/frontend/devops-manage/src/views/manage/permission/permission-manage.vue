<template>
  <div>
    <bk-resize-layout
      placement="left"
      :initial-divide="220"
      :min="220"
      :max="360"
      :border="false"
      class="permission-wrapper"
    >
      <template #aside>
        <ul class="aside">
          <li
            class="aside-item"
            :class="resourceType == item.resourceType ? 'aside-active' : ''"
            v-for="(item, index) in permissionList"
            :key="index"
            @click="handleAsideClick(item, index)"
          >
            <p>
              {{ item.label }}
            </p>
          </li>
        </ul>
      </template>
      <template #main>
        <div class="content">
        <div class="content-btn">
          <bk-button @click="handleReset">{{ t('申请更换授权人') }}</bk-button>
          <div class="filter-bar">
            <bk-date-picker
              v-model="dateTimeRange"
              type="datetimerange"
              :placeholder="t('授权时间')"
              @clear="handleClearDaterange"
              @change="handleChangeDaterange"
              @pick-success="handlePickSuccess"
            />
            <bk-search-select
              v-model="searchValue"
              :data="searchData"
              unique-select
              class="content-btn-search"
              :placeholder="filterTips"
              :key="searchName"
              :get-menu-list="getMenuList"
              value-behavior="need-key"
            />
          </div>
        </div>
        <bk-loading class="content-table" :loading="isLoading">
          <bk-table
            ref="refTable"
            :data="tableData"
            :columns="columns"
            height="100%"
            max-height="100%"
            show-overflow-tooltip
            :key="resourceType"
            :pagination="pagination"
            remote-pagination
            :is-row-select-enable="isRowSelectEnable"
            @select-all="handleSelectAll"
            @selection-change="handleSelectionChange"
            @page-value-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
          >
            <!-- :scroll-loading="isScrollLoading"
            @scroll-bottom="getTableList" -->
            <template #empty>
              <bk-exception
                class="exception-part"
                :description="t('没有数据')"
                scene="part"
                :type="!isEmpty ? 'search-empty' : 'empty'"
              >
                <i18n-t
                  v-if="!isEmpty"
                  tag="div"
                  keypath="可以尝试 调整关键词 或 清空筛选条件"
                >
                  <button class="text-blue" @click='clearSearchValue'>{{t('清空筛选条件')}}</button>
                </i18n-t>
              </bk-exception>
            </template>
            <template #prepend>
              <div v-if="isSelectAll" class="prepend">
                {{ t('已选择全量数据X条', [selectableCount]) }}
                <span @click="handleClear">{{ t('清除选择') }}</span>
              </div>
              <div v-else-if="selectList.length" class="prepend">
                {{ t('已选择X条数据，', [selectList.length]) }}
                <span @click="handleSelectAllData"> {{ t('选择全量数据X条', [selectableCount]) }}</span> 
                <span class="prepend-line">|</span>
                <span @click="handleClear">{{ t('清除选择') }}</span> 
              </div>
            </template>
          </bk-table>
        </bk-loading> 
        </div>
      </template>
    </bk-resize-layout>
    <bk-dialog
      esc-close="reset-dialog"
      :is-show="showResetDialog"
      :key="resourceType"
      theme="primary"
      :width="740"
      :title="t('申请更换授权人')"
      @closed="dialogClose"
    >
      <div class="dialog">
        <p>
          <bk-tag radius="20px" class="tag">
            {{ t('已选择X个XX', [isSelectAll ? selectableCount : selectList.length, searchName]) }}
          </bk-tag>
          <span class="apply-tip">
            {{ isSelfSelected ? t('授权人将立即改为你') : t('对方同意前授权人不变') }}
          </span>
        </p>
        <bk-form
          ref="formRef"
          :model="resetFormData"
          class="reset-form"
        >
          <bk-form-item
            required
            :label="t('新授权人')"
            property="name"
            labelWidth=""
          >
            <project-user-selector
              @change="handleChangeName"
              @removeAll="handleClearName"
              :key="showResetDialog"
            >
            </project-user-selector>
          </bk-form-item>
        </bk-form>
        

        <div v-if="isChecking">
          <Spinner class="check-checking-icon" />
          <span>{{ t('授权校验中') }}</span>
        </div>
        <div v-else-if="isResetFailure" class="check-failure">
          <div class="failed-tips">
            <div class="manage-icon manage-icon-warning-circle-fill warning-icon"></div>
            <div v-if="isAllAlreadyAuthorizer">
              {{ t('所选X的授权人已是该用户，无需更换', [searchName]) }}
            </div>
            <div v-else-if="isPartAlreadyAuthorizer">
              {{ t('以下X个X的授权人已是所选用户，无需更换，将提交其余X', [alreadyAuthorizerCount, searchName, searchName]) }}
            </div>
            <i18n-t v-else keypath="检测到以下X项无法更换授权人，请按失败原因处理或继续提交其余代码库授权" tag="div">
              <span class="tips-count">
                {{ failedCount }}</span><span class="tips-text">{{ t('按失败原因处理') }}</span><span class="tips-text">{{ t('继续提交其余') }}</span><span class="tips-text">{{ searchName }}</span>
            </i18n-t>
          </div>
          <bk-table
            v-if="!isAllAlreadyAuthorizer"
            ref="resetTable"
            :data="resetTableData"
            :pagination="resetTablePagination"
            :border="['outer', 'row']"
          >
            <bk-table-column :label="searchName" width="200" prop="resourceName" show-overflow-tooltip />
            <bk-table-column :label="t('失败原因')" prop="failedMessage" show-overflow-tooltip>
              <template v-slot="{ row }">
                <template v-if="row?.failedArr?.length">
                  <div class="failed-wrapper">
                    <div
                      v-for="(item, index) in row.failedArr" :key="item" class="failed-item failed-msg" v-html="item"
                      v-show="(index <= 2 && !row.isExpand) || row.isExpand"
                    />
                    <div
                        v-if="row.failedArr.length > 3 && !row.isExpand"
                        class="failed-item failed-msg"
                    >
                        ...
                    </div>
                    <div
                      class="failed-item failed-msg expand-btn"
                      v-if="row.failedArr.length > 3"
                      text
                      @click="row.isExpand = !row.isExpand"
                    >
                      {{ row.isExpand ? t('收起') : t('展开') }}
                    </div>
                  </div>
                </template>
                <template v-else>
                  <span
                    class="failed-msg"
                    v-html="row.failedMessage"
                  >
                  </span>
                </template>
              </template>
            </bk-table-column>
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
            {{ t('提交') }}
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
import { ref, onMounted, computed, h, watch } from 'vue';
import http from '@/http/api';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { convertTime } from '@/utils/util'
import { Message, InfoBox } from 'bkui-vue';
import { Success, Spinner } from 'bkui-vue/lib/icon';
import ProjectUserSelector from '@/components/project-user-selector'
const { t } = useI18n();
const route = useRoute();
const tableData = ref([]);
const resetTableData = ref([]);
const resetTablePagination = ref({
  count: 0,
  limit: 6,
  current: 1,
  limitList: [6, 10, 20]
})
const formRef = ref(null);
const refTable = ref(null);
const selectList = ref([]);
const searchValue = ref([]);
const isLoading = ref(true);
const showResetDialog = ref(false);
const dialogLoading = ref(false);
const isResetFailure = ref(false);
const isResetSuccess = ref(false);
const isChecking = ref(false);
const canLoading = ref(true);
const currentUserName = window.top?.userInfo?.username || '';
// 新授权人是否是申请人本人（本人提交立即生效并生成已结束的申请）
const isSelfSelected = computed(() => !!resetFormData.value.name && resetFormData.value.name === currentUserName);
const checkResult = ref({});
const alreadyAuthorizerCount = ref(0);
const isAllAlreadyAuthorizer = ref(false);
const isPartAlreadyAuthorizer = ref(false);
const projectId = computed(() => route.params?.projectCode || route.query?.projectCode);
const userIds = computed(() => route.query?.userId);
const initialResourceType  = computed(() => route.query?.resourceType || 'repertory');
const resourceType = ref(initialResourceType.value);
const isSelectAll = ref(false);  // 选择全量数据
const dateTimeRange = ref(['', '']);
const daterangeCache = ref(['', '']);
const disabledResetBtn = ref(true);
const failedCount = ref(0);
const pagination = ref({
  count: 0,
  limit: 20,
  current: 1,
});
// 待同意资源总数（轻量查询，随列表刷新）
const beingHandoverCount = ref(0);
// 全量选择的可选条数 = 当前筛选总数 - 待同意数
const selectableCount = computed(() => Math.max(pagination.value.count - beingHandoverCount.value, 0));
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
    handoverTo: resetFormData.value.name,
    resourceAuthorizationHandoverList: isSelectAll.value ? [] : resourceAuthorizationHandoverList,
  }
  if (isSelectAll.value) {
    params.greaterThanHandoverTime = dateTimeRange.value[0];
    params.lessThanHandoverTime = dateTimeRange.value[1];
    Object.assign(params, filterQuery.value)
  }
  return params;
})
/**
 * 提交参数：勾选模式下只提交预检通过的资源，全选模式由后端过滤
 */
const submitParams = computed(() => {
  const params = { ...resetParams.value };
  if (!params.fullSelection && checkResult.value?.items?.length) {
    const passedCodes = new Set(checkResult.value.items.filter(item => item.passed).map(item => item.resourceCode));
    params.resourceAuthorizationHandoverList = (params.resourceAuthorizationHandoverList || [])
      .filter(item => passedCodes.has(item.resourceCode));
  }
  return params;
});
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
const columns = ref([
  {
    type: "selection",
    maxWidth: 60,
    minWidth: 60,
    align: 'center'
  },
  {
    label: searchName,
    field: "resourceName",
    render ({ cell, row }) {
      return h(
        'span',
        {
          class: resourceType.value === 'env_node' ? '' : 'resource-name-cell',
          onClick () {
            if (resourceType.value === 'env_node') return
            if (resourceType.value === 'repertory') {
              window.open(`${location.origin}/console/codelib/${row.projectCode}?searchName=${row.resourceName}&id=${row.resourceCode}`, '_blank')
            } else {
              window.open(`${location.origin}/console/pipeline/${row.projectCode}/${row.resourceCode}/history/delegation`, '_blank')
            }
          }
        },
        [
          cell
        ]
      )
    }
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
  {
    label: t('状态'),
    field: "beingHandover",
    render ({ row }) {
      return row.beingHandover
        ? h('span', { style: { color: '#3a84ff' } }, t('待X同意', [row.approver]))
        : t('正常');
    },
  },
])
const resetFormData = ref({
  name: ''
})
const filterQuery = computed(() => {
  return searchValue.value.reduce((query, item) => {
      if (item.id === 'handoverFroms') {
        query[item.id] = item.values?.map(value => value.id);
      } else if (item.id === 'queryHandover') {
        // 待同意/正常筛选，映射为 queryHandover 布尔值
        query[item.id] = item.values?.[0]?.id === 'true';
      } else {
        query[item.id] = item.values?.map(value => value.id).join(',');
      }
      return query;
  }, {})
});

const searchData = computed(() => {
  const data = [
    {
      name: t('授权人'),
      id: 'handoverFroms',
      default: true,
      multiple: true
    },
    {
      name: searchName.value,
      id: 'resourceName',
    },
    {
      name: t('状态'),
      id: 'queryHandover',
    },
  ]
  return data.filter(data => {
    return !searchValue.value.find(val => val.id === data.id)
  })
});
const isEmpty = computed(() => !(searchValue.value.length || dateTimeRange.value.length));
watch(() => searchValue.value, (val, oldVal) => {
  pagination.value.current = 1;
  isSelectAll.value = false;
  refTable.value?.clearSelection();
  selectList.value = [];
  getTableList();
});

onMounted(() => {
  init();
});

function init () {
  pagination.value.current = 1;
  tableData.value = [];
  isSelectAll.value = false;
  selectList.value = [];
  dateTimeRange.value = [];
  if (userIds.value) {
    searchValue.value = [
      {
        id: 'handoverFroms',
        name: t('授权人'),
        values: userIds.value.split(",").map(item=>({id:item,name:item}))
      },
    ]
  } else {
    searchValue.value = [];
  }
};
function clearSearchValue(){
  searchValue.value = [];
  dateTimeRange.value = [];
}
/**
 * 获取列表数据
 */
async function getTableList () {
  try {
    isLoading.value = true;
    const res = await http.getResourceAuthList(projectId.value, {
      page: pagination.value.current,
      pageSize: pagination.value.limit,
      projectCode: projectId.value,
      resourceType: resourceType.value,
      operateChannel: 'MANAGER',
      ...filterQuery.value,
      greaterThanHandoverTime: dateTimeRange.value[0],
      lessThanHandoverTime: dateTimeRange.value[1],
    });
    tableData.value = res.records;
    pagination.value.count = res.count;

    // 刷新待同意资源总数（全量选择可选条数 = 总数 - 待同意数）
    getBeingHandoverCount();

    isLoading.value = false;
  } catch (e) {
    isLoading.value = false;
    console.error(e);
  }
}
/**
 * 获取待同意资源总数（轻量请求 pageSize=1 只取 count）
 */
async function getBeingHandoverCount() {
  try {
    const res = await http.getResourceAuthList(projectId.value, {
      projectCode: projectId.value,
      resourceType: resourceType.value,
      operateChannel: 'MANAGER',
      ...filterQuery.value,
      greaterThanHandoverTime: dateTimeRange.value[0],
      lessThanHandoverTime: dateTimeRange.value[1],
      // 覆盖筛选条件中的 queryHandover，只统计待同意资源
      queryHandover: true,
      page: 1,
      pageSize: 1,
    });
    beingHandoverCount.value = res.count ?? 0;
  } catch (e) {
    console.error(e);
  }
}
/**
 * aside点击事件
 */
function handleAsideClick(item, index) {
  if (item.resourceType === resourceType.value) return;
  resourceType.value = item.resourceType;
  init();
};
/**
 * 申请更换授权人
 */
function handleReset() {
  if(!selectList.value.length) {
    Message({
      theme: 'error',
      message: t('请先选择X', [searchName.value]),
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
 * 待同意的资源不允许勾选
 */
function isRowSelectEnable ({ row }) {
  return !row.beingHandover;
}
/**
 * 当前页全选事件（点击表头全选框，退出全量选择模式）
 */
function handleSelectAll(){
  isSelectAll.value = false;
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
  refTable.value.toggleAllSelection(true);
  isSelectAll.value = true;
}
/**
 * 清除选择
 */
function handleClear() {
  refTable.value?.clearSelection();
  isSelectAll.value = false;
  selectList.value = [];
}
/**
 * 弹窗关闭
 */
function dialogClose() {
  showResetDialog.value = false;
  canLoading.value = true;
  isResetFailure.value = false;
  isResetSuccess.value = false;
  isAllAlreadyAuthorizer.value = false;
  isPartAlreadyAuthorizer.value = false;
  isSelectAll.value = false;
  disabledResetBtn.value = true;
  resetFormData.value.name = '';
  checkResult.value = {};
  formRef.value?.clearValidate();
}

function handleClearName () {
  canLoading.value = true;
  isResetFailure.value = false;
  isResetSuccess.value = false;
  isAllAlreadyAuthorizer.value = false;
  isPartAlreadyAuthorizer.value = false;
  disabledResetBtn.value = true;
  resetTableData.value = [];
  failedCount.value = 0;
  checkResult.value = {};
}

async function handleCheckReset () {
  if (!resetFormData.value.name) return
  if (canLoading.value) {
    isChecking.value = true;
    disabledResetBtn.value = true;
  }
  try {
    const res = await http.checkAuthorizationApplication(projectId.value, resetParams.value)

    if (!resetFormData.value.name) return // 点击input输入框清空按钮，会触发失焦事件
    checkResult.value = res || {};
    const {
      totalCount = 0,
      validCount = 0,
      alreadyAuthorizerCount: alreadyCount = 0,
      checkFailedCount = 0,
    } = checkResult.value;
    alreadyAuthorizerCount.value = alreadyCount;
    failedCount.value = totalCount - validCount;
    const failedItems = (checkResult.value.items || []).filter(item => !item.passed);
    resetTableData.value = failedItems.map(i => {
      return {
        ...i,
        failedArr: i.failedMessage?.includes('<br/>') ? i.failedMessage.split('<br/>') : [],
        isExpand: false
      }
    })
    resetTablePagination.value.count = failedItems.length

    isAllAlreadyAuthorizer.value = validCount === 0 && alreadyCount === totalCount;
    isPartAlreadyAuthorizer.value = alreadyCount > 0 && checkFailedCount === 0 && validCount > 0;
    isResetFailure.value = failedCount.value > 0;
    isResetSuccess.value = failedCount.value === 0;
    isChecking.value = false;
    canLoading.value = false;
    // 全部无法进入申请时，提交按钮置灰
    disabledResetBtn.value = validCount === 0;
  } catch (e) {
    isChecking.value = false;
    canLoading.value = false;
    console.error(e)
  }
}
/**
 * 弹窗提交
 */
function confirmReset() {
  if (canLoading.value) dialogLoading.value = true;
  formRef.value?.validate().then(async () => {
    try {
      const res = await http.submitAuthorizationApplication(projectId.value, submitParams.value)

      dialogLoading.value = false;
      showResetDialog.value = false;
      canLoading.value = true;
      if (res?.immediateEffect) {
        // 新授权人是申请人本人：授权立即生效，同时生成一张已结束的申请
        InfoBox({
          type: 'success',
          title: t('授权人已改为你'),
          content: () => h('div', {
            style: {
              padding: '12px',
              background: 'rgb(245, 246, 250)',
              color: 'rgb(77, 79, 86)',
              textAlign: 'left',
              fontSize: '12px',
              lineHeight: '16px',
            },
          }, [
            h('p', { style: { margin: 0 } }, t('共X项已生效', [res?.effectedCount ?? 0])),
            h('p', { style: { margin: 0 } }, t('可在我的交接中查看')),
          ]),
          confirmText: t('查看结果'),
          cancelText: t('关闭'),
          onConfirm: () => handleViewProgress(res?.flowNo || ''),
        });
      } else {
        InfoBox({
          type: 'success',
          title: t('申请提交成功'),
          content: () => h('div', {
            style: {
              padding: '12px',
              background: 'rgb(245, 246, 250)',
              color: 'rgb(77, 79, 86)',
              textAlign: 'left',
              fontSize: '12px',
              lineHeight: '16px',
            },
          }, [
            h('p', { style: { margin: 0 } }, t('已提交申请，等待X同意', [resetFormData.value.name])),
            h('p', { style: { margin: 0 } }, t('同意前授权人不变')),
            h('p', { style: { margin: 0 } }, t('可在我的交接中查看进度')),
          ]),
          confirmText: t('查看进度'),
          cancelText: t('关闭'),
          onConfirm: () => handleViewProgress(res?.flowNo || ''),
        });
      }

      await getTableList();
      
      disabledResetBtn.value = true;
      isResetSuccess.value = false;
      isResetFailure.value = false;
      isAllAlreadyAuthorizer.value = false;
      isPartAlreadyAuthorizer.value = false;
      checkResult.value = {};
      refTable.value?.clearSelection();
      selectList.value = [];
      isSelectAll.value = false;
    } catch (e) {
      dialogLoading.value = false;
      // 过滤后没有可提交的资源，关闭弹窗并刷新列表
      if (e?.code === 2121107) {
        showResetDialog.value = false;
        await getTableList();
      }
      console.error(e)
    }
  })
};

/**
 * 查看申请进度
 */
function handleViewProgress (flowNo) {
  window.open(`/console/permission/my-handover?flowNo=${flowNo}&type=handoverFromMe`, '_blank');
}

function handleChangeDaterange (date) {
  const startTime = new Date(date[0]).getTime() || ''
  const endTime = new Date(date[1]).getTime() || ''
  daterangeCache.value = [startTime, endTime]
}
function handleClearDaterange () {
  dateTimeRange.value = ['', '']
  pagination.value.current = 1;
  getTableList();
}
function handlePickSuccess () {
  dateTimeRange.value = daterangeCache.value;
  pagination.value.current = 1;
  getTableList();
}

function handlePageChange (page) {
  refTable.value?.clearSelection();
  isSelectAll.value = false;
  selectList.value = [];
  pagination.value.current = page;
  getTableList();
}

function handlePageLimitChange (limit) {
  refTable.value?.clearSelection();
  isSelectAll.value = false;
  selectList.value = [];
  pagination.value.current = 1;
  pagination.value.limit = limit;
  getTableList();
}

async function getMenuList (item, keyword) {
  if (item.id === 'queryHandover') {
    return [
      { id: 'true', name: t('待同意') },
      { id: 'false', name: t('正常') },
    ]
  }
  if (item.id !== 'handoverFroms') return []
  const query = {
    memberType: 'user',
    page: 1,
    pageSize: 400
  }
  if (item.id === 'handoverFroms' && keyword) {
    query.userName = keyword
  }
  const res = await http.getProjectMembers(projectId.value, query)
  return res.records.map(i => {
    return {
      ...i,
      displayName: i.name || i.id,
      name: i.type === 'user' ? (!i.name ? i.id : `${i.id} (${i.name})`) : i.name,
    }
  })
}

function handleChangeName ({ list }) {
  if (!list) {
    handleClearName()
  } else {
    const val = list.join(',')
    resetFormData.value.name = val
    handleCheckReset()
  }
}

</script>

<style lang="scss" scoped>
.permission-wrapper {
  width: 100%;
  height: 100%;
  
  .aside {
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
    padding: 24px;
    flex: 1;

    .content-btn-search {
      background-color: #fff;
    }

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
      width: 100%;

      .prepend{
        width: 100%;
        height: 32px;
        line-height: 32px;
        background: #F0F1F5;
        text-align: center;
        box-shadow: 0 -1px 0 0 #DCDEE5;

        .prepend-line {
          padding: 0 4px;
        }
        
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
  .apply-tip {
    margin: -2px 0 16px 0;
    font-size: 12px;
    color: #979BA5;
  }

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

      .tips-count {
        color: #ef9b30;
        padding: 0 5px;
        font-weight: 700;
      }

      .tips-text {
        color: #63656E;
        font-weight: 700;
      }
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
.check-checking-icon {
  color: #3A84FF;
  margin-right: 5px;
}
.reset-form {
  .bk-form-item {
    margin-bottom: 15px !important;
  }
}
.exception-part{
  // ::v-deep .bk-exception-img {
  //   height: 200px !important;
  // }
  .text-blue{
    color: #699DF4;
  }
}
</style>

<style lang="scss">
  .resource-name-cell {
    cursor: pointer;
    &:hover {
      color: #3a84ff;
    }
  }
  .manage-user-selector {
    display: inline-flex;
    width: 100%;
    .bk-tag-input-trigger {
      width: 100%;
    }
  }
  .content-btn-search {
    .bk-search-select-container {
      background: #fff;
    }
  }
  .reset-dialog {
    max-height: 800px;
  }
  .failed-wrapper {
    padding: 10px 0;
    .failed-item {
      line-height: 20px;
      text-wrap: wrap;
    }
    .expand-btn {
      color: #3A84FF;
      cursor: pointer;
    }
  }
  .failed-msg {
    a {
      color: #3A84FF;
    }
  }
</style>