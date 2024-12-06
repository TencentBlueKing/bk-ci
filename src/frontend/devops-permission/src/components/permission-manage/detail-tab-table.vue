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
              'hover-link': ['codecc_task', 'pipeline', 'pipeline_group'].includes(row.resourceType)
            }" 
            @click="handleToResourcePage(row)"
          >{{ row.resourceName }}</span>
        </template>
      </bk-table-column>
      <template v-if="!isAuthorizations">
        <bk-table-column :label="t('用户组')" prop="groupName" />
        <bk-table-column :label="t('用户组描述')" prop="groupDesc" />
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
const resourceType = computed(() => props.resourceType);
const projectId = computed(() => route.params?.projectCode || route.query?.projectCode);
const tableList = computed(() => props.data);
const border = ['row', 'outer'];

function pageLimitChange(limit) {
  emit('pageLimitChange',limit, resourceType.value, props.type);
}
function pageValueChange(value) {
  emit('pageValueChange',value, resourceType.value, props.type);
}

/**
 * 跳转页面
 */
function handleToResourcePage (row) {
  if (!(['codecc_task', 'pipeline', 'pipeline_group'].includes(row.resourceType))) return
  switch (row.resourceType) {
    case 'pipeline':
      window.open(`${location.origin}/console/pipeline/${projectId.value}/${row.resourceCode}/history/permission/?groupId=${row.groupId}`)
      return
    case 'pipeline_group':
      window.open(`${location.origin}/console/pipeline/${projectId.value}/list/listAuth/${row.resourceCode}/${row.resourceName}?groupId=${row.groupId}`)
      return
    case 'codecc_task':
      window.open(`${location.origin}/console/codecc/${projectId.value}/task/${row.resourceCode}/settings/authority?groupId=${row.groupId}`)
      return
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
