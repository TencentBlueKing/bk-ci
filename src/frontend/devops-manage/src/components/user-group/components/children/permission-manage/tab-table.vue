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
    <bk-table-column label="用户组" prop="groupName" style="position: relative;">
      <template #default="{row}">
        {{ row.groupName }}
        <div v-if="!isShowOperation && row.removeMemberButtonControl !== 'OTHER'"  class="overlay">唯一管理员无法移出</div>
      </template>
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
      <template #default="{row, index}">
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
            @click="handleHandOver(row, index)"
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
import { ref, defineProps, defineEmits, computed } from 'vue';

const total = ref(0);
const fixedBottom = {
  position: 'relative',
  height: 42,
};
const refTable = ref(null);
const groupId = computed(() => props.groupId);
const groupTotal = computed(() => props.groupTotal);

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
    type: Number,
  },
  groupTotal: {
    type: Number,
  },
  selectedData: {
    type: Object,
  },
});
const emit = defineEmits([
  'handleRenewal',
  'handleHandOver',
  'handleRemove',
  'getSelectList',
  'handleLoadMore',
  'handleSelectAllData',
  'handleClear',
])
const isCurrentAll = ref(false);
/**
 * 当前页全选事件
 */
function handleSelectAll(val) {
  isCurrentAll.value = true;
  emit('getSelectList', Object.assign(val, {isAll:true}), groupId.value);
}
/**
 * 多选事件
 * @param val
 */
function handleSelectionChange(val) {
  isCurrentAll.value = false;
  emit('getSelectList', val, groupId.value);
};
/**
 * 全量数据选择
 */
function handleSelectAllData() {
  const selectLength = refTable.value.getSelection().length
  if(!isCurrentAll.value && selectLength != groupTotal.value) {
    refTable.value.toggleAllSelection();
  }
  emit('handleSelectAllData', groupId.value)
}
/**
 * 清除选择
 */
function handleClear() {
  refTable.value.clearSelection();
  emit('handleClear', groupId.value);
}
/**
 * 续期按钮点击
 * @param row 行数据
 */
function handleRenewal(row) {
  emit('handleRenewal', row, groupId.value);
}
/**
 * 移交按钮点击
 * @param row 行数据
 */
function handleHandOver(row, index) {
  emit('handleHandOver', row, groupId.value, index);
}
/**
 * 移出按钮点击
 * @param row 行数据
 */
function handleRemove(row) {
  emit('handleRemove', row, groupId.value);
}
/**
 * 加载更多
 */
function handleLoadMore() {
  emit('handleLoadMore',groupId.value);
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
.overlay{
  position: absolute;
  left: 0;
  backdrop-filter: blur(0.5px);
  transform: translateY(-42px);
  width: 100%;
  height: 42px;
  background: rgba(255,229,180, .6);
  font-family: MicrosoftYaHei;
  font-size: 12px;
  color: #63656E;
  text-align: center;
}
</style>
