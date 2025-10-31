<template>
  <bk-loading :loading="loading" :zIndex="100">
    <bk-table
      class="resource-table"
      ref="refTable"
      :max-height="!isShowOperation && 464"
      :data="tableList"
      show-overflow-tooltip
      :pagination="pagination"
      :border="border"
      :remote-pagination="isRemotePagination"
      empty-cell-text="--"
      selection-key="resourceCode"
      :checked="selectedResourceCode"
      :scroll-loading="scrollLoading"
      @select-all="handleSelectAll"
      @selection-change="handleSelectionChange"
      @page-limit-change="pageLimitChange"
      @page-value-change="pageValueChange"
    >
      <template #prepend>
        <div v-if="isCurrentAll" class="prepend">
          {{t('已选择全量数据X条', [groupTotal])}}
          <span class="prepend-line">|</span>
          <span @click="handleClear">{{t("清除选择")}}</span>
        </div>
        <div v-else-if="isShowOperation && selectedData[resourceType]?.length" class="prepend">
          {{t('已选择X条数据，', [selectedData[resourceType].length])}}
          <span @click="handleSelectAllData">{{ t('选择全量数据X条', [groupTotal]) }}</span>
          <span class="prepend-line">|</span>
          <span @click="handleClear">{{t("清除选择")}}</span>
        </div>
      </template>
      <bk-table-column type="selection" :min-width="50" width="50" align="center" v-if="isShowOperation" />
      <bk-table-column  v-if="resourceType !== 'project'" :label="groupName" prop="resourceName">
        <template #default="{ row }">
          <span
            :class="{
              'resource-name': true,
              'hover-link': row.isLinkable
            }" 
            @click="handleToResourcePage(row)"
          >{{ row.resourceName }}</span>
        </template>
      </bk-table-column>
      <bk-table-column :label="t('用户组')" prop="groupName">
        <template #default="{row}">
          {{ row.groupName }}
          <div class="overlay" v-if="shouldShowOverlay(row)">
            {{ row.unableMessage }}
            <span
              v-if="row.removeMemberButtonControl === 'TEMPLATE' && row.isLinkable"
              @click="handleToResourcePage(row)"
              class="text-blue"
            >
              [{{ row.groupName }}]
            </span>
          </div>
        </template>
      </bk-table-column>
      <bk-table-column :label="t('有效期')" prop="expiredAtDisplay" />
      <bk-table-column :label="t('加入时间')" prop="joinedTime" >
        <template #default="{row}">
          {{ timeFormatter(row.joinedTime) }}
        </template>
      </bk-table-column>
      <bk-table-column :label="t('加入方式')" prop="joinedType">
        <template #default="{row}">
          {{ row.joinedType === "DIRECT" ? "直接加入" : "用户组加入" }}
        </template>
      </bk-table-column>
      <bk-table-column :label="t('操作')" v-if="isShowOperation" :show-overflow-tooltip="false">
        <template #default="{row, index}">
          <div>
            <template
              v-if="row.removeMemberButtonControl === 'TEMPLATE'"
            >
              <bk-button
                text
                theme="primary"
                disabled
                v-bk-tooltips="{
                  content: t('通过用户组获得权限，请到流水线里续期整个用户组'),
                  placement: 'top',
                  disabled: row.removeMemberButtonControl !== 'TEMPLATE'
                }"
              >
                {{t("续期")}}
              </bk-button>
              <bk-button
                text
                theme="primary"
                class="operation-btn"
                disabled
                v-bk-tooltips="{
                  content: t('通过用户组获得权限，请到用户组里移出用户'),
                  placement: 'top'
                }"
              >
                {{t("移交")}}
              </bk-button>
            </template>
            <template v-else>
              <bk-button
                text
                theme="primary"
                @click="handleRenewal(row)"
                :disabled="row.expiredAtDisplay == t('永久') || asideItem?.departed"
                v-bk-tooltips="{
                  content: asideItem?.departed ? t('该用户已离职，无需续期') : t('无需续期'),
                  placement: 'top',
                  disabled: row.expiredAtDisplay !== t('永久') && !asideItem?.departed
                }"
              >
                {{t("续期")}}
              </bk-button>
              <bk-button
                text
                theme="primary"
                class="operation-btn"
                @click="handleHandOver(row, index)"
                :disabled="row.isExpired && row.removeMemberButtonControl === 'OTHER'"
                v-bk-tooltips="{
                  content: t('已过期，无需移交'),
                  placement: 'top',
                  disabled: row.removeMemberButtonControl !== 'OTHER' || !row.isExpired
                }"
              >
                {{t("移交")}}
              </bk-button>
            </template>
            <bk-button
              text
              theme="primary"
              :disabled="row.removeMemberButtonControl != 'OTHER'"
              @click="handleRemove(row, index)"
              v-bk-tooltips="{
                content: TOOLTIPS_CONTENT[row.removeMemberButtonControl] || '',
                disabled: row.removeMemberButtonControl === 'OTHER'
              }"
            >
              {{t("移出")}}
            </bk-button>
          </div>
        </template>
      </bk-table-column>
      <template #appendLastRow v-if="remainingCount > 0 && !pagination && data.length">
        <div class="prepend appendLastRow">
          {{ t("剩余X条数据", [remainingCount]) }}
          <span @click="handleLoadMore"> {{t("加载更多")}} </span>
        </div>
      </template>
    </bk-table>
  </bk-loading>
</template>

<script setup name="TabTable">
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { ref, defineProps, defineEmits, computed } from 'vue';
import { timeFormatter } from '@/common/util.ts'
import useManageAside from "@/store/manageAside";
import { storeToRefs } from 'pinia';

const LINKABLE_RESOURCE_TYPES = ['codecc_task', 'pipeline', 'pipeline_group', 'repertory', 'env_node'];
const URL_TEMPLATES = {
  pipeline: (projectId, row) => `${location.origin}/console/pipeline/${projectId}/${row.resourceCode}/history/permission/?groupId=${row.groupId}`,
  pipeline_group: (projectId, row) => `${location.origin}/console/pipeline/${projectId}/list/listAuth/${row.resourceCode}/${row.resourceName}?groupId=${row.groupId}`,
  codecc_task: (projectId, row) => `${location.origin}/console/codecc/${projectId}/task/${row.resourceCode}/settings/authority?groupId=${row.groupId}`,
  repertory: (projectId, row) => `${location.origin}/console/codelib/${projectId}/?searchName=${row.resourceName}&id=${row.resourceCode}`,
  env_node: (projectId, row) => `${location.origin}/console/environment/${projectId}/node/allNode`,
};
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
  resourceName: String,
  groupTotal: Number,
  selectedData: Object,
  hasNext: Boolean,
  loading: Boolean,
  groupName: String,
  batchFlag: String,
  isRemotePagination:{
    type: Boolean,
    default: true,
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
  'pageLimitChange',
  'pageValueChange',
])
const route = useRoute();
const { t } = useI18n();
const manageAsideStore = useManageAside();
const {
  asideItem
} = storeToRefs(manageAsideStore);
const refTable = ref(null);
const curSelectedData = ref([]);
const isCurrentAll = ref(false);
const selectedResourceCode = computed(() => isCurrentAll.value ? tableList.value.map(i => i.resourceCode) : curSelectedData.value.map(i => i.resourceCode));
const remainingCount = computed(()=> props.groupTotal - props.data.length);
const TOOLTIPS_CONTENT = {
  UNIQUE_MANAGER: t('唯一管理员，不可直接移出。请交接或停用项目'),
  UNIQUE_OWNER: t('唯一拥有者，不可直接移出。请交接或删除资源'),
  TEMPLATE: t('通过用户组加入，不可直接移出。如需调整，请编辑用户组')
}
const projectId = computed(() => route.params?.projectCode || route.query?.projectCode);
const tableList = computed(() => props.data.map(item => ({
    ...item,
    unableMessage: getUnableMessage(item),
    isExpired: item.expiredAt < Date.now() && item.removeMemberButtonControl === 'OTHER',
    isLinkable: LINKABLE_RESOURCE_TYPES.includes(item.resourceType)
  }))
);
const border = ['row', 'outer'];
function shouldShowOverlay(row){
  if (props.isShowOperation) {
    return false;
  }

  switch (props.batchFlag) {
    case 'renewal':
      return row.expiredAtDisplay === t('永久') || row.removeMemberButtonControl === 'TEMPLATE';
    case 'handover':
      return row.removeMemberButtonControl === 'TEMPLATE' || row.isExpired;
    case 'remove':
      return row.removeMemberButtonControl !== 'OTHER';
    default:
      return false;
  }
}

function getUnableMessage(row){
  switch (props.batchFlag) {
    case 'renewal':
      if(row.expiredAtDisplay === t('永久')){
        return t("无需续期");
      } else if (row.removeMemberButtonControl === 'TEMPLATE') {
        return t("通过用户组获得权限，请到") + props.resourceName + t("里续期整个用户组")
      }
    case 'handover':
      if (row.removeMemberButtonControl === 'TEMPLATE') {
        return t('通过用户组获得权限，请到用户组里移出用户');
      } else if (row.isExpired) {
        return t('已过期，无需移交')
      }
    case 'remove':
      let message = TOOLTIPS_CONTENT[row.removeMemberButtonControl];
      return message;
    default:
      return '';
  }
}
/**
 * 当前页全选事件
 */
function handleSelectAll({checked}) {
  if (checked) {
    emit('getSelectList', tableList.value, props.resourceType);
    curSelectedData.value = tableList.value;
    isCurrentAll.value = false;
  } else {
    handleClear()
  }
}
/**
 * 多选事件
 */
function handleSelectionChange() {
  const selectionList = refTable.value.getSelection();
  emit('getSelectList', selectionList, props.resourceType);
  curSelectedData.value = selectionList;
  isCurrentAll.value = props.data.length === selectionList
};
/**
 * 全量数据选择
 */
function handleSelectAllData() {
  const selectLength = refTable.value.getSelection().length
  if (selectLength != props.data.length) {
    refTable.value.toggleAllSelection();
  }
  emit('handleSelectAllData', props.resourceType)
  isCurrentAll.value = true;
}
/**
 * 清除选择
 */
function handleClear() {
  refTable.value.clearSelection();
  isCurrentAll.value = false;
  curSelectedData.value = [];
  emit('handleClear', props.resourceType);
}
/**
 * 续期按钮点击
 * @param row 行数据
 */
function handleRenewal(row) {
  emit('handleRenewal', row, props.resourceType, refTable.value);
}
/**
 * 移交按钮点击
 * @param row 行数据
 */
function handleHandOver(row, index) {
  emit('handleHandOver', row, props.resourceType, index);
}
/**
 * 移出按钮点击
 * @param row 行数据
 */
function handleRemove(row, index) {
  emit('handleRemove', row, props.resourceType, index);
}
/**
 * 加载更多
 */
function handleLoadMore() {
  emit('handleLoadMore', props.resourceType);
}

function pageLimitChange(limit) {
  emit('pageLimitChange',limit, props.resourceType);
}
function pageValueChange(value) {
  emit('pageValueChange',value, props.resourceType);
}

function handleToResourcePage (row) {
  if (!row.isLinkable) return
  const url = URL_TEMPLATES[row.resourceType]?.(projectId.value, row);
  if (url) {
    window.open(url);
  }
}
</script>

<style lang="less" scoped>
.resource-table{
  margin-top: 4px;
  border: 1px solid #DCDEE5;
  .prepend {
    width: 100%;
    height: 42px;
    line-height: 42px;
    background: #F0F1F5;
    text-align: center;
    box-shadow: 0 -1px 0 0 #DCDEE5;
    .prepend-line {
      padding: 0 4px;
    }
    span {
      font-family: MicrosoftYaHei;
      font-size: 12px;
      color: #3A84FF;
      letter-spacing: 0;
      line-height: 20px;
      cursor: pointer;
    }
  }
  .operation-btn{
    margin: 0 8px;
  }
  .appendLastRow{
    background-color: #fff;
  }
  .overlay{
    position: absolute;
    left: 0;
    transform: translateY(-42px);
    width: 100%;
    height: 42px;
    background: rgba(255, 232, 195, .7);
    font-family: MicrosoftYaHei;
    font-size: 12px;
    color: #63656E;
    text-align: center;
  }
  .hover-link {
    cursor: pointer;
    &:hover {
      color: #3a84ff;
    }
  }
}
.appendLastRow{
  background-color: #fff;
}
.overlay{
  position: absolute;
  left: 0;
  transform: translateY(-42px);
  width: 100%;
  height: 42px;
  background: rgba(255, 232, 195, .7);
  font-family: MicrosoftYaHei;
  font-size: 12px;
  color: #63656E;
  text-align: center;
}
.text-blue{
  cursor: pointer;
  color: #699DF4;
}
</style>
