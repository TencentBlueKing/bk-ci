<template>
  <bk-loading :loading="loading" :zIndex="100">
    <bk-table
      class="detail-table"
      ref="refTable"
      :max-height="!isShowOperation && 464"
      :data="data"
      show-overflow-tooltip
      :pagination="pagination"
      :border="border"
      remote-pagination
      empty-cell-text="--"
      @page-limit-change="pageLimitChange"
      @page-value-change="pageValueChange"
    >
      <bk-table-column :label="groupName" prop="resourceName" />
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
const { t } = useI18n();
const refTable = ref(null);
const border = ['row', 'outer'];

function pageLimitChange(limit) {
  emit('pageLimitChange',limit, props.resourceType, props.type);
}
function pageValueChange(value) {
  emit('pageValueChange',value, props.resourceType, props.type);
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
