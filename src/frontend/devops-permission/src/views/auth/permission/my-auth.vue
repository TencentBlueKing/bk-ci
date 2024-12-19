<template>
    <div>
      <div class="permission-wrapper">
        <div class="content">
          <div class="content-btn">
            <bk-button @click="handleReset">{{ t('申请批量重置') }}</bk-button>
            <div class="filter-bar">
              <bk-select
                v-model="projectId"
                class="project-select"
                :prefix="t('所属项目')"
                filterable
                @change="handleChangeProject"
              >
                <bk-option
                  v-for="(item, index) in projectList"
                  :id="item.englishName"
                  :key="index"
                  :name="item.projectName"
                />
              </bk-select>
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
                max-height="32"
                class="content-btn-search"
                :placeholder="filterTips"
                :key="searchName"
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
              :key="`${projectId}-${resourceType}`"
              :pagination="pagination"
              remote-pagination
              :is-row-select-enable="disabledRowSelect"
              @select-all="handleSelectAll"
              @selection-change="handleSelectionChange"
              @page-value-change="handlePageChange"
              @page-limit-change="handlePageLimitChange"
            >
          
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
                  {{ t('已选择全量数据') }}
                  <span @click="handleClear">{{ t('清除选择') }}</span>
                </div>
                <div v-else-if="selectList.length" class="prepend">
                  {{ t('已选择X条数据，', [selectList.length]) }}
                  <span @click="handleSelectAllData"> {{ t('选择全量数据X条', [pagination.count]) }}</span> 
                  <span class="prepend-line">|</span>
                  <span @click="handleClear">{{ t('清除选择') }}</span> 
                </div>
              </template>
            </bk-table>
          </bk-loading> 
        </div>
      </div>
      <bk-dialog
        esc-close="reset-dialog"
        :is-show="showResetDialog"
        :key="resourceType"
        theme="primary"
        :width="740"
        :title="t('申请批量重置')"
        @closed="dialogClose"
      >
        <div class="dialog">
          <bk-tag radius="20px" class="tag">
            {{ t('已选择X个XX', [isSelectAll ? pagination.count : selectList.length, searchName]) }}
          </bk-tag>
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
              <project-user-selector
                :project-id="projectId"
                :key="showResetDialog"
                @change="handleChangeName"
                @removeAll="handleClearName"
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
              <div class="permission-icon permission-icon-warning-circle-fill warning-icon"></div>
              <i18n-t keypath="检测到以下X项授权将无法重置，请给授权人添加Y权限或Z" tag="div">
                <span class="tips-count">{{ failedCount }}</span><span class="tips-text">{{ searchName }}</span><span class="tips-text">{{ t('切换授权人') }}</span>
              </i18n-t>
            </div>
            <bk-table
              ref="resetTable"
              :data="resetTableData"
              :pagination="resetTablePagination"
              :border="['outer', 'row']"
            >
              <bk-table-column :label="searchName" width="200" prop="resourceName" show-overflow-tooltip />
              <bk-table-column :label="t('失败原因')" prop="handoverFailedMessage" show-overflow-tooltip>
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
                        v-html="row.handoverFailedMessage"
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
  import { ref, onMounted, computed, h, watch } from 'vue';
  import http from '@/http/api';
  import tools from '@/utils/tools';
  import { useI18n } from 'vue-i18n';
  import { useRoute } from 'vue-router';
  import { convertTime } from '@/utils/util'
  import { InfoBox, Message } from 'bkui-vue';
  import { Success, Spinner  } from 'bkui-vue/lib/icon';
  import { cacheProjectCode } from '@/store/useCacheProjectCode'
  import ProjectUserSelector from '@/components/project-user-selector'
  import normalIcon from '@/css/svg/normal.svg'

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
  const defaultProjectId = computed(() => route?.params.projectCode || route?.query.projectCode || route?.query.project_code || cacheProjectCode.get() || tools.getCookie('X-DEVOPS-PROJECT-ID') || '');
  const projectId = ref('');
  const userId = computed(() => window.top.userInfo.username);
  const resourceType = computed(() => route.name || 'repertory');
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
  const projectList = ref([]);
  
  const searchName = computed(() => {
    const nameMap = {
      'pipeline': t('流水线执行授权'),
      'envNode': t('部署节点授权'),
      'repertory': t('代码库授权')
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
      handoverChannel: 'OTHER',
      handoverFrom: userId.value,
      handoverTo: resetFormData.value.name,
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
      render ({ cell, row }) {
        return h('div', { style: { 'display': 'flex' } }, cell 
          ? [
            h(Spinner, { style: { color: '#3A84FF', marginRight: '5px'} }),
            h('span', t('移交中，待X确认', [row.approver]))
          ]
          : [
            h('img', { src: normalIcon, style: { width: '14px', marginRight: '3px' } }),
            h('span', t('正常'))
          ]
        )
      }
    },
  ])
  const resetFormData = ref({
    name: ''
  })
  const filterQuery = computed(() => {
    const query = searchValue.value.reduce((query, item) => {
        query[item.id] = item.values?.map(value => value.id);
        if (typeof(query[item.id][0]) === 'boolean') {
          query[item.id] = query[item.id][0]
        } else {
          query[item.id] = query[item.id]?.join(',')
        }
        return query;
    }, {})
    if (query)
    return query
  });
  
  const searchData = computed(() => {
    const data = [
      {
        name: searchName.value,
        id: 'resourceName',
        default: true,
      },
      {
        name: t('状态'),
        id: 'queryHandover',
        children: [
          { name: t('移交中'), id: true },
          { name: t('正常'), id: false }
        ]
      },
    ]
    return data.filter(data => {
      return !searchValue.value.find(val => val.id === data.id)
    })
  });
  const isEmpty = computed(() => !(searchValue.value.length || dateTimeRange.value.length));
 
  watch(() => [searchValue.value, resourceType.value], () => {
    pagination.value.current = 1;
    isSelectAll.value = false;
    refTable.value?.clearSelection();
    selectList.value = [];
    getTableList();
  });
  
  onMounted(async () => {
    init();
    await fetchProjectList();
    await getTableList();
  });
  
  const init = () => {
    pagination.value.current = 1;
    tableData.value = [];
    isSelectAll.value = false;
    selectList.value = [];
    dateTimeRange.value = [];
  };
  /**
   * 获取项目列表
   */
  const fetchProjectList = async () => {
    try {
      projectList.value = await http.fetchProjectList()
      if (defaultProjectId.value) {
        const curProject = projectList.value.find(i => i.englishName === defaultProjectId.value);
        projectId.value = curProject?.englishName || projectList.value[0].englishName;
      }
    } catch (e) {
      console.error(e)
    }
  }

  const handleChangeProject = (val) => {
    projectId.value = val;
    cacheProjectCode.set(val);
    init();
    getTableList();
  }

  const disabledRowSelect = ({ row, index, isCheckAll }) => {
    return !row.beingHandover
  }

  const clearSearchValue = () => {
    searchValue.value = [];
    dateTimeRange.value = [];
  }

  /**
   * 获取列表数据
   */
  const getTableList = async () => {
    try {
      if (!projectId.value) return
      isLoading.value = true;
      const res = await http.getResourceAuthList(projectId.value, {
        page: pagination.value.current,
        pageSize: pagination.value.limit,
        projectCode: projectId.value,
        resourceType: resourceType.value,
        ...filterQuery.value,
        greaterThanHandoverTime: dateTimeRange.value[0],
        lessThanHandoverTime: dateTimeRange.value[1],
        handoverFrom: userId.value,
      });
      tableData.value = res.records;
      pagination.value.count = res.count;
  
      isLoading.value = false;
    } catch (e) {
      isLoading.value = false;
      console.error(e);
    }
  }
 
  /**
   * 批量重置
   */
  const handleReset = () => {
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
  const handleSelectAll = () => {
    selectList.value = refTable.value.getSelection();
  }
  /**
   * 多选事件
   * @param val
   */
  const handleSelectionChange = () => {
    isSelectAll.value = false;
    selectList.value = refTable.value.getSelection();
  };
  /**
   * 全量数据选择
   */
  const handleSelectAllData = () => {
    if (!isSelectAll.value && selectList.value.length !== tableData.value.filter(i => !i.beingHandover).length) {
      refTable.value.toggleAllSelection();
    }
    isSelectAll.value = true;
  }
  /**
   * 清除选择
   */
  const handleClear = () => {
    refTable.value?.clearSelection();
    isSelectAll.value = false;
    selectList.value = [];
  }
  /**
   * 弹窗关闭
   */
  const dialogClose = () => {
    showResetDialog.value = false;
    canLoading.value = true;
    isResetFailure.value = false;
    isResetSuccess.value = false;
    isSelectAll.value = false;
    disabledResetBtn.value = true;
    resetFormData.value.name = '';
    formRef.value?.clearValidate();
  }
  
  const handleClearName = () => {
    canLoading.value = true;
    isResetFailure.value = false;
    isResetSuccess.value = false;
    disabledResetBtn.value = true;
    resetTableData.value = [];
    failedCount.value = 0;
  }
  
  const handleCheckReset = async () => {
    if (!resetFormData.value.name) return
    if (canLoading.value) {
      isChecking.value = true;
      disabledResetBtn.value = true;
    }
    try {
      const res = await http.checkAuthorization(projectId.value, {
        ...resetParams.value,
        preCheck: true,
        checkPermission: false
      })
  
      if (!resetFormData.value.name) return // 点击input输入框清空按钮，会触发失焦事件
      failedCount.value = res['FAILED']?.length || 0
      if (failedCount.value) {
        resetTableData.value = res['FAILED']?.map(i => {
          return {
            ...i,
            failedArr: i.handoverFailedMessage.includes('<br/>') ? i.handoverFailedMessage.split('<br/>') : [],
            isExpand: false
          }
        })
        resetTablePagination.value.count = failedCount.value
      }
      isResetFailure.value = !!failedCount.value;
      isResetSuccess.value = !failedCount.value;
      isChecking.value = false;
      canLoading.value = false;
      const selectFlag = isSelectAll.value ? pagination.value.count : selectList.value.length
      disabledResetBtn.value = failedCount.value === selectFlag;
    } catch (e) {
      isChecking.value = false;
      canLoading.value = false;
      console.error(e)
    }
  }
  /**
   * 弹窗提交
   */
  const confirmReset = () => {
    if (canLoading.value) dialogLoading.value = true;
    formRef.value?.validate().then(async () => {
      try {
        const flowNo = await http.resetAuthorization(projectId.value, resetParams.value)
        
        dialogLoading.value = false;
        showResetDialog.value = false;
        canLoading.value = true;
        InfoBox({
          type: 'success',
          title: t('重置授权申请提交成功'),
          confirmText: t('查看进度'),
          cancelText: t('关闭'),
          onConfirm: () => {
            window.open(`${window.location.origin}/console/permission/my-handover?flowNo=${flowNo}&type=handoverFromMe`, '_blank')
          },
          content: h(
            'div',
            {
              style: {
                padding: '12px',
                background: '#F5F6FA',
                color: '#4D4F56'
              },
            },
            t('已成功提交「重置授权」申请，等待交接人确认。可在“我的交接”中查看进度。'),
          ),
        });
  
        await getTableList();
        
        disabledResetBtn.value = true;
        isResetSuccess.value = false;
        isResetFailure.value = false;
        refTable.value?.clearSelection();
        selectList.value = [];
        isSelectAll.value = false;
      } catch (e) {
        console.error(e)
      }
    })
  };
  
  const handleChangeDaterange = (date) => {
    const startTime = new Date(date[0]).getTime() || ''
    const endTime = new Date(date[1]).getTime() || ''
    daterangeCache.value = [startTime, endTime]
  }
  const handleClearDaterange = () => {
    dateTimeRange.value = ['', '']
    pagination.value.current = 1;
    getTableList();
  }
  const handlePickSuccess = () => {
    dateTimeRange.value = daterangeCache.value;
    pagination.value.current = 1;
    getTableList();
  }
  
  const handlePageChange = (page) => {
    refTable.value?.clearSelection();
    isSelectAll.value = false;
    selectList.value = [];
    pagination.value.current = page;
    getTableList();
  }
  
  const handlePageLimitChange = (limit) => {
    refTable.value?.clearSelection();
    isSelectAll.value = false;
    selectList.value = [];
    pagination.value.current = 1;
    pagination.value.limit = limit;
    getTableList();
  }
  
  const handleChangeName  = ({ list }) => {
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
    display: flex;
    width: 100%;
    height: 100%;
  
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
        .project-select {
          margin-right: 10px;
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
  .exception-part {
    // ::v-deep .bk-exception-img {
    //   height: 200px !important;
    // }
    .text-blue{
      color: #699DF4 !important;
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