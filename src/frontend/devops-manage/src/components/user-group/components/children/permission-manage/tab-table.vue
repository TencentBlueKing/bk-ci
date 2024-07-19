<template>
  <bk-loading :loading="loading" :zIndex="100">
    <bk-table
      class="table"
      ref="refTable"
      max-height="464"
      min-height="84"
      :fixed-bottom="fixedBottom"
      :data="data"
      show-overflow-tooltip
      :pagination="pagination"
      :border="['row', 'outer']"
      remote-pagination
      empty-cell-text="--"
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
      <template #fixedBottom v-if="remainingCount && !pagination">
        <div class="prepend">
          {{ t("剩余X条数据",[remainingCount]) }}
          <span @click="handleLoadMore"> {{t("加载更多")}} </span>
        </div>
      </template>
      <bk-table-column type="selection" :min-width="50" width="50" align="center" v-if="isShowOperation" />
      <bk-table-column :label="groupName" prop="groupName">
        <template #default="{row}">
          {{ row.groupName }}
          <div v-if="!isShowOperation && row.removeMemberButtonControl === 'UNIQUE_MANAGER'"  class="overlay">{{t("唯一管理员无法移出")}}</div>
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
          {{ row.operateSource === "DIRECT" ? "直接加入" : "API加入" }}{{ row.operator ? '/' + row.operator : '' }}
        </template>
      </bk-table-column>
      <bk-table-column :label="t('操作')" v-if="isShowOperation">
        <template #default="{row, index}">
          <div class="operation-btn">
            <bk-button
              text
              theme="primary"
              @click="handleRenewal(row)"
              :disabled="row.expiredAtDisplay == t('永久')"
            >{{t("续期")}}</bk-button>
            <bk-button
              text
              theme="primary"
              style="margin:0 8px"
              @click="handleHandOver(row, index)"
            >{{t("移交")}}</bk-button>
            <span
              v-bk-tooltips="{
                content: TOOLTIPS_CONTENT[row.removeMemberButtonControl] || '',
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
import { TOOLTIPS_CONTENT } from '@/utils/constants'

const props = defineProps({
  isShowOperation: {
    type: Boolean,
    default: true,
  },
  data: {
    type: Array,
    default: () => [],
  },
  pagination: Object,
  scrollLoading: Boolean,
  resourceType: String,
  groupTotal: Number,
  selectedData: Object,
  hasNext: Boolean,
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
const { t } = useI18n();
const refTable = ref(null);
const isCurrentAll = ref(false);
const resourceType = computed(() => props.resourceType);
const groupTotal = computed(() => props.groupTotal);
const remainingCount = computed(()=> props.groupTotal - props.data.length);
const scrollLoading = computed(()=>props.scrollLoading);
const fixedBottom = {
  position: 'relative',
  height: 42,
  loading: scrollLoading.value
};
/**
 * 当前页全选事件
 */
function handleSelectAll({checked}) {
  if(checked){
    emit('getSelectList', refTable.value.getSelection(), resourceType.value);
    isCurrentAll.value = false;
  } else {
    handleClear()
  }
}
/**
 * 多选事件
 */
function handleSelectionChange() {
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
.table{
  margin-top: 4px;
  border: 1px solid #DCDEE5;
}
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
