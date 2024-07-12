<template>
  <bk-loading :loading="loading">
    <bk-table
      ref="refTable"
      max-height="464"
      min-height="84"
      :fixed-bottom="fixedBottom"
      :data="data"
      show-overflow-tooltip
      :pagination="pagination"
      :border="['row', 'outer']"
      remote-pagination
      @select-all="handleSelectAll"
      @selection-change="handleSelectionChange"
      @page-limit-change="pageLimitChange"
      @page-value-change="pageValueChange"
    >
      <template #prepend>
        <div v-if="isCurrentAll" class="prepend">
          {{t('已选择全量数据X条', [groupTotal])}}
          &nbsp; | &nbsp;
          <span @click="handleClear">{{t("清除选择")}}</span>
        </div>
        <div v-else-if="isShowOperation && selectedData[resourceType]?.length" class="prepend">
          {{t('已选择X条数据，', [selectedData[resourceType].length])}}
          <span @click="handleSelectAllData">{{ t('选择全量数据X条', [groupTotal]) }}</span>
          &nbsp; | &nbsp;
          <span @click="handleClear">{{t("清除选择")}}</span>
        </div>
      </template>
      <template #fixedBottom v-if="hasNext && !pagination">
        <div class="prepend">
          {{ t("剩余X条数据",[remainingCount]) }}
          <span @click="handleLoadMore"> {{t("加载更多")}} </span>
        </div>
      </template>
      <bk-table-column type="selection" :min-width="30" width="30" align="center" v-if="isShowOperation" />
      <bk-table-column :label="groupName" prop="groupName">
        <template #default="{row}">
          {{ row.groupName }}
          <div v-if="!isShowOperation && row.removeMemberButtonControl !== 'OTHER'"  class="overlay">{{t("唯一管理员无法移出")}}</div>
        </template>
      </bk-table-column>
      <bk-table-column :label="t('用户描述')" prop="groupDesc" />
      <bk-table-column :label="t('有效期')" prop="expiredAtDisplay" />
      <bk-table-column :label="t('加入时间')" prop="joinedTime" >
        <template #default="{row}">
          {{ timeFormatter(row.joinedTime) }}
        </template>
      </bk-table-column>
      <bk-table-column :label="t('加入方式/操作人')" prop="operateSource">
        <template #default="{row}">
          {{ row.operateSource === "DIRECT" ? "直接加入" : "API加入" }}/{{ row.operator }}
        </template>
      </bk-table-column>
      <bk-table-column :label="t('操作')" v-if="isShowOperation">
        <template #default="{row, index}">
          <div class="operation-btn">
            <bk-button
              text
              theme="primary"
              @click="handleRenewal(row)"
            >{{t("续期")}}</bk-button>
            <bk-button
              text
              theme="primary"
              style="margin:0 8px"
              @click="handleHandOver(row, index)"
            >{{t("移交")}}</bk-button>
            <span
              v-bk-tooltips="{
                content: row.removeMemberButtonControl==='UNIQUE_MANAGER'?
                  '唯一管理员，不可移出。请添加新的管理员后再移出。':
                  row.removeMemberButtonControl==='TEMPLATE'?
                  '通过用户组加入，不可直接移出。如需调整，请编辑用户组。':
                  row.removeMemberButtonControl==='UNIQUE_OWNER'?
                  '唯一拥有者，不可移出。请添加新的拥有者后再移出。': ''
                  ,
                disabled: row.removeMemberButtonControl === 'OTHER'
              }"
            >
              <bk-button
                text
                theme="primary"
                :disabled="row.removeMemberButtonControl!='OTHER'"
                @click="handleRemove(row, index)"
              >{{t("移出")}}</bk-button>
            </span>
          </div>
        </template>
      </bk-table-column>
    </bk-table>
  </bk-loading>
</template>

<script setup name="TabTable">
import { useI18n } from 'vue-i18n';
import { ref, defineProps, defineEmits, computed } from 'vue';
import { timeFormatter } from '@/common/util.ts'

const { t } = useI18n();
const fixedBottom = {
  position: 'relative',
  height: 42,
};
const refTable = ref(null);
const isCurrentAll = ref(false);
const resourceType = computed(() => props.resourceType);
const groupTotal = computed(() => props.groupTotal);

const props = defineProps({
  isShowOperation: {
    type: Boolean,
    default: true,
    required: true,
  },
  pagination: Object,
  remainingCount: Number,
  data: {
    type: Array,
  },
  resourceType: {
    type: String,
  },
  groupTotal: {
    type: Number,
  },
  selectedData: {
    type: Object,
  },
  hasNext: {
    type: Boolean,
  },
  loading: Boolean,
  groupName: String,
});
const emit = defineEmits([
  'handleRenewal',
  'handleHandOver',
  'handleRemove',
  'getSelectList',
  'handleLoadMore',
  'handleSelectAllData',
  'handleClear',
  'pageLimitChange',
  'pageValueChange',
])
/**
 * 当前页全选事件
 */
function handleSelectAll({ checked, data }) {
  emit('getSelectList', refTable.value.getSelection(), resourceType.value);
  isCurrentAll.value = false;
}
/**
 * 多选事件
 */
function handleSelectionChange({checked}) {
  emit('getSelectList', refTable.value.getSelection(), resourceType.value);
  isCurrentAll.value = props.data.length === refTable.value.getSelection()
};
/**
 * 全量数据选择
 */
function handleSelectAllData() {
  const selectLength = refTable.value.getSelection().length
  if (selectLength != props.data.length) {
    refTable.value.toggleAllSelection();
  }
  emit('handleSelectAllData', resourceType.value)
  isCurrentAll.value = true;
}
/**
 * 清除选择
 */
function handleClear() {
  refTable.value.clearSelection();
  isCurrentAll.value = false;
  emit('handleClear', resourceType.value);
}
/**
 * 续期按钮点击
 * @param row 行数据
 */
function handleRenewal(row) {
  emit('handleRenewal', row, resourceType.value);
}
/**
 * 移交按钮点击
 * @param row 行数据
 */
function handleHandOver(row, index) {
  emit('handleHandOver', row, resourceType.value, index);
}
/**
 * 移出按钮点击
 * @param row 行数据
 */
function handleRemove(row, index) {
  emit('handleRemove', row, resourceType.value, index);
}
/**
 * 加载更多
 */
function handleLoadMore() {
  emit('handleLoadMore',resourceType.value);
}

function pageLimitChange(limit) {
  emit('pageLimitChange',limit, resourceType.value);
}
function pageValueChange(value) {
  emit('pageValueChange',value, resourceType.value);
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
