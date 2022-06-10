<script setup lang="ts">
import {
  ref,
  onMounted,
  watch,
} from 'vue';
import http from '@/http/api';
import {
  sharedProps,
} from '../common/props-type';
import {
  useRouter,
} from 'vue-router';

const props = defineProps(sharedProps);
const isLoading = ref(false);
const columns = ref([]);
const tableData = ref([]);
const pagination = ref({
  current: 1,
  count: 0,
  limit: 20,
});
const router = useRouter()

const handlePageChange = (current) => {
  pagination.value.current = current;
  getData();
};

const handlePageLimitChange = (limit) => {
  pagination.value.limit = limit;
  getData();
};

const handleRowClick = (e, row) => {
  router.push({
    name: 'PluginFailAnalysis',
    query: {
      pipelineId: row.pipelineId,
    },
  })
}

const getData = () => {
  isLoading.value = true;
  http
    .getAtomStatisticsDetail(
      props.status,
      pagination.value.current,
      pagination.value.limit,
    )
    .then((data) => {
      Object.entries(data.headerInfo).forEach(([field, label]) => {
        columns.value.push({
          label,
          field,
        });
      });
      tableData.value = data.records?.map(record => {
        if (!record.classifyCode) {
          record.classifyCode = '--'
        }
        return {
          ...record,
          ...record.atomBaseInfo,
          ...record.atomFailInfos,
        }
      });
      pagination.value.count = data.count;
    })
    .finally(() => {
      isLoading.value = false;
    });
};

watch(
  props.status,
  getData,
);
onMounted(getData);
</script>

<template>
  <bk-loading
    class="overview-card mt20"
    :loading="isLoading"
  >
    <h3 class="g-card-title">Plugin stat</h3>
    <bk-table
      class="analysis-table"
      :columns="columns"
      :data="tableData"
      :pagination="pagination"
      @page-value-change="handlePageChange"
      @page-limit-change="handlePageLimitChange"
      @row-click="handleRowClick">
    </bk-table>
  </bk-loading>
</template>

<style lang="scss" scoped>
.analysis-table {
  margin-top: .15rem;
  margin-bottom: .08rem;
  ::v-deep .bk-table-body {
    cursor: pointer;
  }
}
</style>
