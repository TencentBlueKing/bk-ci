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
            unique-select
            max-height="32"
            class="search-select"
            :placeholder="filterTips"
            value-behavior="need-key"
          />
      </div>
    </div>
    <bk-loading class="handover-table" :loading="isLoading">
      <bk-table
        ref="refTable"
        :data="tableData"
        :columns="columns"
        height="100%"
        max-height="100%"
        show-overflow-tooltip
        :pagination="pagination"
        remote-pagination
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
            {{ t('已选择全量数据X条', [pagination.count]) }}
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

  </section>
</template>
  

<script lang="ts" setup>
  import { ref, computed, h } from 'vue';
  import { useI18n } from 'vue-i18n';

  const { t } = useI18n();
  const isLoading = ref(false);
  const tableData = ref([]);
  const pagination = ref({
    count: 0,
    limit: 20,
    current: 1,
  });
  const isGiven = ref(true); // true: 我移交的  false: 移交给我的
  const searchValue = ref([]);
  const selectList = ref([]);
  const isSelectAll = ref(false);  // 选择全量数据
  const columns = computed(() => {
    const list = [
      {
        label: t('单号'),
        field: "flowNo",
        render ({ cell, row }) {
          return h(
            'span',
            {
              style: {
                cursor: 'pointer',
                color: '#3a84ff',
              },
              onClick () {
                window.open(``, '_blank')
              }
            },
            [
              cell
            ]
          )
        },
      },
      {
        label: t('移交详情'),
        field: "handoverFrom",
      },
      {
        label: t('提单人'),
        field: "handoverTime",
        render ({ cell, row }) {
          return h(
            'span',
            [
              cell
            ]
          );
        },
      },
      {
        label: t('提单时间'),
        field: "handoverFrom",
      },
      {
        label: t('状态'),
        field: "handoverFrom",
      },
      {
        label: t('操作'),
        field: "handoverFrom",
      },
    ]
 
    if (!isGiven.value) {
      list.unshift({
        type: "selection",
        maxWidth: 60,
        minWidth: 60,
        align: 'center'
      })
    }
    return list
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
  const searchData = computed(() => {
  const data = [
      {
        name: t('单号'),
        id: 'flowNo',
        default: true,
      },
      {
        name: t('提单人'),
        id: 'memberID',
      },
      {
        name: t('状态'),
        id: 'handoverStatus'
      }
    ]
    return data.filter(data => {
      return !searchValue.value.find(val => val.id === data.id)
    })
  });
  const filterTips = computed(() => {
    return searchData.value.map(item => item.name).join(' / ');
  })
  const handleChangeHandoverType = (value) => {
    isGiven.value = value;
  }
  const handleBatchProcess = () => {

  }
  const handleSelectAll = () => {

  }
  const handleSelectionChange = () => {

  }
  const handlePageChange = () => {

  }
  const handlePageLimitChange = () => {

  }
  const clearSearchValue = () => {

  }
</script>

<style lang="scss">
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
    }
  }
</style>
