<template>
  <bk-table
    ref="refTable"
    max-height="464"
    min-height="84"
    :fixed-bottom="fixedBottom"
    :data="data"
    show-overflow-tooltip
    :pagination="pagination"
    @select-all="handleSelectAll"
    @selection-change="handleSelectionChange"
  >
    <template #prepend>
      
      <div v-if="isShowOperation && selectedData[groupId]" class="prepend">
        已选择 {{ selectedData[groupId].length }} 条数据，
        <span @click="handleSelectAllData"> 选择全量数据 {{ total }} 条 </span>
        &nbsp; | &nbsp;
        <span @click="handleClear">清除选择</span>
      </div>
    </template>
    <template #fixedBottom v-if="!pagination">
      <div class="prepend">
        剩余{{ 22 }} 条数据，
        <span @click="handleLoadMore"> 加载更多 </span>
      </div>
    </template>
    <bk-table-column type="selection" :min-width="30" width="30" align="center" v-if="isShowOperation" />
    <bk-table-column label="用户组" prop="groupName">
    </bk-table-column>
    <bk-table-column label="用户描述" prop="groupDesc" />
    <bk-table-column label="有效期" prop="validityPeriod" />
    <bk-table-column label="加入时间" prop="joinedTime" />
    <bk-table-column label="加入方式/操作人" prop="operateSource">
      <template #default="{row}">
        {{ row.operateSource }}/{{ row.operator }}
      </template>
    </bk-table-column>
    <bk-table-column label="操作" v-if="isShowOperation">
      <template #default="{row}">
        <div class="operation-btn">
          <bk-button
            text
            theme="primary"
            @click="handleRenewal(row)"
          >续期</bk-button>
          <bk-button
            text
            theme="primary"
            style="margin:0 8px"
            @click="handleHandOver(row)"
          >移交</bk-button>
          <span
            v-bk-tooltips="{
              content: row.removeMemberButtonControl==='UNIQUE_MANAGER'?
                '唯一管理员，不可移出。请添加新的管理员后再移出。':
                row.removeMemberButtonControl==='TEMPLATE'?
                '通过用户组加入，不可直接移出。如需调整，请编辑用户组。':
                row.removeMemberButtonControl==='UNIQUE_MANAGER'?
                '唯一拥有者，不可移出。请添加新的拥有者后再移出。': ''
                ,
              disabled: row.removeMemberButtonControl === 'OTHER'
            }"
          >
            <bk-button
              text
              theme="primary"
              :disabled="row.removeMemberButtonControl!='OTHER'"
              @click="handleRemove(row)"
            >移出</bk-button>
          </span>
        </div>
      </template>
    </bk-table-column>
  </bk-table>
</template>

<script setup name="TabTable">
import { ref, defineProps, defineEmits, computed, inject } from 'vue';

const total = ref(0);
const fixedBottom = {
  position: 'relative',
  height: 42,
};
const refTable = ref(null);

const props = defineProps({
  isShowOperation: {
    type: Boolean,
    default: true,
    required: true,
  },
  pagination: {
    type: Object,
  },
  data: {
    type: Array,
  },
  groupId: {
    type: Number
  },
  selectedData: {
    type: Object,
  },
});
const groupId = computed(() => props.groupId);
const handlers = inject('handlers');
/**
 * 当前页全选事件
 */
function handleSelectAll(val) {
  if (handlers) {
    handlers.getSelectList( Object.assign(val, {isAll:true}), groupId.value);
  }
}
/**
 * 多选事件
 * @param val
 */
function handleSelectionChange(val) {
  if (handlers) {
    handlers.getSelectList( val, groupId.value);
  }
};
/**
 * 全量数据选择
 */
function handleSelectAllData() {
  refTable.value.toggleAllSelection();
  if (handlers) {
    handlers.handleSelectAllData(groupId.value);
  }
}
/**
 * 清除选择
 */
function handleClear() {
  refTable.value.clearSelection();
  if (handlers) {
    handlers.handleClear(groupId.value);
  }
}
/**
 * 续期按钮点击
 * @param row 行数据
 */
function handleRenewal(row) {
  if (handlers) {
    handlers.handleRenewal(row);
  }
}
/**
 * 移交按钮点击
 * @param row 行数据
 */
function handleHandOver(row) {
  if (handlers) {
    handlers.handleHandOver(row);
  }
}
/**
 * 移出按钮点击
 * @param row 行数据
 */
function handleRemove(row) {
  if (handlers) {
    handlers.handleRemove(row);
  }
}
/**
 * 加载更多
 */
function handleLoadMore() {
  if (handlers) {
    handlers.handleLoadMore(groupId.value);
  }
}

</script>

<style lang="less" scoped>
.prepend {
  width: 100%;
  height: 32px;
  line-height: 32px;
  background: #F0F1F5;
  text-align: center;
  box-shadow: 0 -1px 0 0 #DCDEE5;

  span {
    font-family: MicrosoftYaHei;
    font-size: 12px;
    color: #3A84FF;
    letter-spacing: 0;
    line-height: 20px;
    cursor: pointer;
  }
}
</style>
