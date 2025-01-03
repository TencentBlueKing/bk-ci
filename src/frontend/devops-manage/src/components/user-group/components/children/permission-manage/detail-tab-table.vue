<template>
  <bk-loading :loading="loading" :zIndex="100">
    <bk-table
      class="detail-table"
      ref="refTable"
      :max-height="!isShowOperation && 464"
      :data="tableList"
      show-overflow-tooltip
      :pagination="pagination"
      :border="border"
      remote-pagination
      empty-cell-text="--"
      @page-limit-change="pageLimitChange"
      @page-value-change="pageValueChange"
    >
      <bk-table-column :label="groupName" prop="resourceName">
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
      <template v-if="!isAuthorizations">
        <bk-table-column :label="t('用户组')" prop="groupName" />
      </template>
      <template v-else>
        <bk-table-column :label="t('授权人')" prop="handoverFrom" />
      </template>
    </bk-table>
  </bk-loading>
</template>

<script setup name="TabTable">
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';
import { ref, defineProps, defineEmits, computed } from 'vue';

const props = defineProps({
  data: {
    type: Array,
    default: () => [],
  },
  pagination: Object,
  resourceType: String,
  resourceName: String,
  loading: Boolean,
  groupName: String,
  isAuthorizations: {
    type: Boolean,
    default: false
  },
  type: String
});
const emit = defineEmits([
  'pageLimitChange',
  'pageValueChange',
])
const route = useRoute();
const { t } = useI18n();
const refTable = ref(null);
const projectId = computed(() => route.params?.projectCode || route.query?.projectCode);
const tableList = computed(() => {
  return props.data.map(row => ({
    ...row,
    isLinkable: LINKABLE_RESOURCE_TYPES.includes(row.resourceType)
  }));
});
const border = ['row', 'outer'];
const LINKABLE_RESOURCE_TYPES = ['codecc_task', 'pipeline', 'pipeline_group'];
const URL_TEMPLATES = {
  pipeline: (projectId, row) => `${location.origin}/console/pipeline/${projectId}/${row.resourceCode}/history/permission/?groupId=${row.groupId}`,
  pipeline_group: (projectId, row) => `${location.origin}/console/pipeline/${projectId}/list/listAuth/${row.resourceCode}/${row.resourceName}?groupId=${row.groupId}`,
  codecc_task: (projectId, row) => `${location.origin}/console/codecc/${projectId}/task/${row.resourceCode}/settings/authority?groupId=${row.groupId}`
};

function pageLimitChange(limit) {
  emit('pageLimitChange',limit, props.resourceType, props.type);
}
function pageValueChange(value) {
  emit('pageValueChange',value, props.resourceType, props.type);
}

/**
 * 跳转页面
 */
function handleToResourcePage (row) {
  if (!row.isLinkable) return
  const url = URL_TEMPLATES[row.resourceType]?.(projectId.value, row);
  if (url) {
    window.open(url);
  }
}
</script>

<style lang="less" scoped>
.detail-table {
  margin-top: 4px;
  border: 1px solid #DCDEE5;

  .hover-link {
    cursor: pointer;
    &:hover {
      color: #3a84ff;
    }
  }
}
</style>
