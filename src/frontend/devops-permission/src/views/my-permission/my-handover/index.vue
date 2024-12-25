<template>
  <section class="handover-wrapper">
    <div class="handover-main-header">
      <div class="left">
        <div class="handover-type-btn">
          <span
            v-for="item in handoverTypeMap"
            :key="item.value"
            :class="['item', {
              'active': item.value === isGiven
            }]"
            @click="handleChangeHandoverType(item.value)"
          >
            {{ item.label }}
          </span>
        </div>
        <bk-button v-if="!isGiven" @click="handleBatchProcess">{{ t('批量处理') }}</bk-button>
      </div>
      <div class="right">
        <bk-search-select
            v-model="searchValue"
            :data="searchData"
            :key="isGiven"
            unique-select
            max-height="32"
            class="search-select"
            :placeholder="filterTips"
            value-behavior="need-key"
          />
      </div>
    </div>
    <bk-dialog
      v-model:is-show="showBatchApproval"
      class="batch-approval-dialog"
      :width="600"
      :title="t('批量审批')"
      header-align="center"
      quick-close
    >
      <bk-form
        :model="batchApprovalData"
        label-width="100"
      >
        <bk-form-item
          :label="t('审批结果')" 
          property="name"
        >
          <bk-radio-group
            v-model="batchApprovalData.handoverAction"
          >
            <bk-radio label="AGREE">{{ t('通过') }}</bk-radio>
            <bk-radio label="REJECT">{{ t('拒绝') }}</bk-radio>
          </bk-radio-group>
          
        </bk-form-item>
        <bk-form-item
          :label="t('审批意见')"
        >
          <bk-input
            v-model="batchApprovalData.remark"
            :placeholder="t('请输入审核意见，拒绝时必填')"
            clearable
            type="textarea"
            :rows="3"
            :maxlength="100"
          />
        </bk-form-item>
      </bk-form>
      <template #footer>
        <bk-button
          :loading="isFetchLoading"
          class="mr5"
          theme="primary"
          :disabled="batchApprovalData.handoverAction === 'REJECT' && !batchApprovalData.remark"
          v-bk-tooltips="{
            content: t('拒绝时，审批意见必填'),
            disabled: batchApprovalData.handoverAction === 'REJECT' && !!batchApprovalData.remark
          }"
          @click="handleConfirmBatch"
        >
          {{ t('确定') }}
        </bk-button>
        <bk-button
          :loading="isFetchLoading"
          @click="handleCancelBatch"
        >
          {{ t('取消') }}
        </bk-button>
      </template>
    </bk-dialog>
    <bk-loading class="handover-table" :loading="isLoading">
      <bk-table
        ref="refTable"
        :data="handoverList"
        :columns="columns"
        height="100%"
        max-height="100%"
        border="outer"
        show-overflow-tooltip
        :pagination="pagination"
        remote-pagination
        :key="isGiven"
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
    <bk-sideslider
      v-model:isShow="showHandoverDetail"
      ext-cls="handover-sideslider"
      width="960"
    >
      <template #header>
          {{ curHandoverInfo?.flowNo }}
          <Copy
            class="copy-icon"
            @click="handleCopyFlowNo"
          />
      </template>
      <template #default>
        <handover-detail
          v-if="showHandoverDetail"
          :is-given="isGiven"
          :is-show="showHandoverDetail"
          :data="curHandoverInfo"
          @success="handleApprovalSuccess"
        />
      </template>
    </bk-sideslider>
  </section>
</template>
  

<script setup>
  import http from '@/http/api';
  import { h, ref, computed, watch, onMounted, resolveDirective, withDirectives } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { Message } from 'bkui-vue'
  import { Spinner, Copy  } from 'bkui-vue/lib/icon';
  import { useRoute } from 'vue-router';
  import abnormalIcon from '@/css/svg/abnormal.svg'
  import unknownIcon from '@/css/svg/unknown.svg'
  import normalIcon from '@/css/svg/normal.svg'
  import HandoverDetail from './handover-detail.vue'
  const bkTooltips = resolveDirective('bk-tooltips');
  const { t } = useI18n();
  const route = useRoute();
  const refTable = ref(null);
  const isLoading = ref(false);
  const isFetchLoading = ref(false);
  const handoverList = ref([]);
  const pagination = ref({
    count: 0,
    limit: 20,
    current: 1,
  });
  const isGiven = ref(true); // true: 我移交的  false: 移交给我的
  const searchValue = ref([]);
  const selectList = ref([]);
  const isSelectAll = ref(false);  // 选择全量数据
  const curHandoverInfo = ref({});
  const showHandoverDetail = ref(false);
  const showBatchApproval = ref(false);
  const batchApprovalData = ref({
    remark: '',
    handoverAction: 'AGREE'
  })
  const queryFlowNo = ref ('');
  const userId = computed(() => window.top.userInfo.username);
  const columns = computed(() => {
    return [
      ...(
        !isGiven.value ? [
          {
            type: "selection",
            maxWidth: 60,
            minWidth: 60,
            align: 'center'
          }
        ] : []
      ),
      {
        label: t('单号'),
        field: "flowNo",
        showOverflowTooltip: true,
        render ({ cell, row }) {
          return h(
            'span',
            {
              style: {
                cursor: 'pointer',
                color: '#3a84ff',
              },
              onClick () {
                curHandoverInfo.value = row
                showHandoverDetail.value = true;
              }
            },
            [
              cell
            ]
          )
        },
      },
      {
        label: t('项目名称'),
        field: 'projectName',
        showOverflowTooltip: true,
      },
      {
        label: t('移交详情'),
        field: 'title',
        showOverflowTooltip: true,
      },
      ...(
        !isGiven.value ? [
          {
            label: t('提单人'),
            field: 'applicant',
            showOverflowTooltip: true,
          },
        ] : []
      ),
      {
        label: t('提单时间'),
        field: 'createTime',
        showOverflowTooltip: true,
      },
      ...(
        isGiven.value ? [
          {
            label: t('当前处理人'),
            field: 'approver',
            showOverflowTooltip: true,
          },
        ] : []
      ),
      {
        label: t('状态'),
        field: 'handoverStatus',
        showOverflowTooltip: true,
        render ({ cell, row}) {
          return h('div', { style: { 'display': 'flex', 'align-items': 'center' } },
            cell === 'PENDING'
            ? [
              h(Spinner, { style: { 'color': '#3A84FF', 'margin-right': '5px'} }),
              h('span', t('待处理')),
            ]
            : [
              h('img', { src: getStatusMap(cell)?.icon, style: { 'width': '14px', 'margin-right': '3px' } }),
              h('span', getStatusMap(cell)?.text),
              ...(
                row.remark ? [withDirectives(h('p', {
                  class: 'permission-icon permission-icon-info',
                  style: { 'margin-left': '5px' }
                }), [[bkTooltips, row.remark]])] : []
              )
            ]
          )
        }
      },
      ...(
        !isGiven.value ? [
          {
            label: t('操作'),
            field: 'operation',
            showOverflowTooltip: true,
            render ({ cell, row }) {
              return h('div', {
                style: {
                  'color': row.handoverStatus !== 'PENDING' ? '#C4C6CC' : '#3A84FF',
                  'cursor': row.handoverStatus !== 'PENDING' ? 'not-allowed' : 'pointer'
                },
                onClick () {
                  if (row.handoverStatus !== 'PENDING') return
                  curHandoverInfo.value = row
                  showHandoverDetail.value = true;
                }
              }, t('去处理'))
            }
          },
        ] : []
      )
    ]
  })
  const isEmpty = computed(() => !searchValue.value.length);
  const handoverTypeMap = computed(() => {
    return [
      {
        label: t('我移交的'),
        value: true
      },
      {
        label: t('移交给我的'),
        value: false
      }
    ]
  })
  const typeMapping = {
    handoverFromMe: true,
    handoverToMe: false
  }
  const searchData = computed(() => {
    const data = [
      {
        name: t('单号'),
        id: 'flowNo',
        default: true,
      },
      {
        name: t('项目名称'),
        id: 'projectName',
        default: true,
      },
      {
        name: isGiven.value ? t('审批人') : t('提单人'),
        id: isGiven.value ? 'approver' : 'applicant',
      },
      {
        name: t('状态'),
        id: 'handoverStatus',
        children: [
          { name: t('已通过'), id: 'SUCCEED' },
          { name: t('已拒绝'), id: 'REJECT' },
          { name: t('待处理'), id: 'PENDING' },
          { name: t('已撤销'), id: 'REVOKE' },
        ]
      }
    ]
    return data.filter(data => {
      return !searchValue.value.find(val => val.id === data.id)
    })
  });
  const filterTips = computed(() => {
    return searchData.value.map(item => item.name).join(' / ');
  })
  const filterQuery = computed(() => {
    return searchValue.value.reduce((query, item) => {
        query[item.id] = item.values?.map(value => value.id).join(',');
        return query;
    }, {})
  });

  watch(() => isGiven.value, () => {
    searchValue.value = []
  })
  watch(() => searchValue.value, (val) => {
    init()
  })
  const disabledRowSelect = ({ row, index, isCheckAll }) => {
    return row.handoverStatus === 'PENDING'
  }
  const getStatusMap = (status) => {
    const statusMap = {
      'REJECT': {
        icon: abnormalIcon,
        text: t('已拒绝')
      },
      'SUCCEED': {
        icon: normalIcon,
        text: t('已通过')
      },
      'REVOKE': {
        icon: unknownIcon,
        text: t('已撤销')
      }
    }
    return statusMap[status]
  }
  const fetchHandoverList = async () => {
    try {
      isLoading.value = true
      const param = {
        memberId: userId.value,
        page: pagination.value.current,
        pageSize: pagination.value.limit,
        ...filterQuery.value
      }
      if (isGiven.value) {
        param['applicant'] = userId.value
      } else {
        param['approver'] = userId.value
      }
      const res = await http.fetchHandoverOverviewList(param)
      handoverList.value = res.records
      pagination.value.count = res.count
    } catch (e) {
      console.error(e)
    } finally {
      isLoading.value = false
    }
  }
  const handleChangeHandoverType = (value) => {
    isGiven.value = value;
  }
  const handleBatchProcess = () => {
    if (!selectList.value.length) {
      Message({
        theme: 'error',
        message: t('请先选择交接单')
      })
      return
    }
    showBatchApproval.value = true;
  }
  const handleConfirmBatch = async () => {
    const params = {
      allSelection: isSelectAll.value,
      flowNos: isSelectAll.value ? [] : selectList.value.map(i => i.flowNo),
      operator: userId.value,
      handoverAction: batchApprovalData.value.handoverAction,
      remark: batchApprovalData.value.remark,
    }
    try {
      isFetchLoading.value = true;
      const res = await http.handleBatchHandovers(params);
      if (res) {
        Message({
          theme: 'success',
          message: batchApprovalData.value.handoverAction === 'AGREE' ? t('移交权限已通过') : t('移交权限已拒绝')
        });
        fetchHandoverList();
        refTable.value?.clearSelection();
        selectList.value = [];
        isSelectAll.value = false;
      }
    } catch (e) {
      Message({
        theme: 'error',
        message: e.message || e
      })
    } finally {
      isFetchLoading.value = false
      handleCancelBatch()
    }
  }

  const handleCancelBatch = () => {
    showBatchApproval.value = false;
    batchApprovalData.value.remark = '';
    batchApprovalData.value.handoverAction = 'AGREE';
  }
  const handleSelectAll = () => {
    selectList.value = refTable.value.getSelection();
  }
  const handleSelectAllData = () => {
    if (!isSelectAll.value && selectList.value.length !== handoverList.value.filter(i => i.handoverStatus === 'PENDING').length) {
      refTable.value.toggleAllSelection();
    }
    isSelectAll.value = true;
  }
  const handleClear = () => {
    refTable.value?.clearSelection();
    isSelectAll.value = false;
    selectList.value = [];
  }
  const handleSelectionChange = () => {
    isSelectAll.value = false;
    selectList.value = refTable.value.getSelection();
  }
  const handlePageChange = (page) => {
    refTable.value?.clearSelection();
    isSelectAll.value = false;
    selectList.value = [];
    pagination.value.current = page;
    fetchHandoverList();
  }

  const handlePageLimitChange = (limit) => {
    refTable.value?.clearSelection();
    isSelectAll.value = false;
    selectList.value = [];
    pagination.value.current = 1;
    pagination.value.limit = limit;
    fetchHandoverList();
  }
  const clearSearchValue = () => {
    searchValue.value = [];
  }
  const handleCopyFlowNo = (value) => {
    const textarea = document.createElement('textarea')
      document.body.appendChild(textarea)
      textarea.value = curHandoverInfo.value.flowNo
      textarea.select()
      if (document.execCommand('copy')) {
        document.execCommand('copy')
        Message({
          theme: 'success',
          message: t('复制成功')
        })
      }
      document.body.removeChild(textarea)
  }
  const handleApprovalSuccess = () => {
    showHandoverDetail.value = false
    fetchHandoverList();
  }
  const init = () => {
    pagination.value.current = 1;
    handoverList.value = [];
    selectList.value = [];
    isSelectAll.value = false;
    fetchHandoverList();
  }
  onMounted(async () => {
    const { type, flowNo } = route.query;

    if (type) {
      isGiven.value = typeMapping[type] ?? isGiven.value;
    }
    // isGiven参数获取之后调用接口
    await fetchHandoverList();

    if (type && flowNo) {
      searchValue.value = [
        {
          name: t('单号'),
          id: 'flowNo',
          values: [{ id: flowNo, name: flowNo }]
        }
      ];
      queryFlowNo.value = flowNo;
      curHandoverInfo.value = handoverList.value.find(i => i.flowNo === queryFlowNo.value);
      showHandoverDetail.value = true;
    }
  })
</script>

<style lang="scss" scoped>
  .handover-wrapper {
    background: #F4F5F9;
    padding: 20px;
    .handover-main-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 20px;
      .left {
        display: flex;
      }
    }
    .handover-type-btn {
      display: flex;
      align-items: center;
      height: 32px;
      background: #EAEBF0;
      border-radius: 4px; 
      padding: 0 4px;
      font-size: 12px;
      margin-right: 16px;
      .item {
        height: 24px;   
        line-height: 24px;
        border-radius: 4px;
        padding: 0 8px;
        cursor: pointer;
        &:nth-child(1) {
          margin-right: 4px;
        }
        &.active {
          background: #fff;     
        }
      }
    }
    .search-select {
      width: 520px;
    }
    .handover-table {
      height: calc(100% - 52px);
      .exception-part{
        .text-blue{
          color: #699DF4;
        }
      }
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
          font-size: 12px;
          color: #3A84FF;
          letter-spacing: 0;
          line-height: 20px;
          cursor: pointer;
        }
      }
    }
  }
  .handover-sideslider {
    .copy-icon {
      margin-left: 10px;
      cursor: pointer;
    }
    ::v-deep .bk-modal-body {
      background-color: #F0F1F5;
    }
  }
  .batch-approval-dialog {
    .mr5 {
      margin-right: 5px;
    }
  }
</style>
