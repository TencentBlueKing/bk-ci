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

const props = defineProps(sharedProps);
const isLoading = ref(false);
const columns = [
  {
    label: 'Pipeline',
    field: 'pipelineName',
  },
  {
    label: 'Branch',
    field: 'branch',
  },
  {
    label: 'Start Time',
    field: 'startTime',
  },
  {
    label: 'Username',
    field: 'startUser',
  },
  {
    label: 'Error Type',
    field: 'errorTypeName',
  },
  {
    label: 'Error Code',
    field: 'errorCode',
  },
  {
    label: 'Error Message',
    field: 'errorMsg',
  },
];
const tableData = ref([]);
const pagination = ref({
  current: 1,
  count: 0,
  limit: 20,
});

const handlePageChange = (current) => {
  pagination.value.current = current;
  getData();
};

const handlePageLimitChange = (limit) => {
  pagination.value.limit = limit;
  getData();
};

const getData = () => {
  isLoading.value = true;
  http
    .getPipelineFailDetail(
      props.status,
      pagination.value.current,
      pagination.value.limit,
    )
    .then((data) => {
      pagination.value.count = data.count;
      tableData.value = data.records?.map(record => ({
        ...record,
        ...record.pipelineBuildInfo,
        ...record.errorInfo,
      }));
    })
    .finally(() => {
      isLoading.value = false;
    });
};

watch(
  () => props.status,
  getData,
);
onMounted(getData);
</script>

<template>
  <bk-loading
    class="overview-card mt20"
    :loading="isLoading"
  >
    <h3 class="g-card-title">Details</h3>
    <bk-table
      class="error-table"
      :columns="columns"
      :data="tableData"
      :pagination="pagination"
      @page-value-change="handlePageChange"
      @page-limit-change="handlePageLimitChange">
    </bk-table>
  </bk-loading>
</template>

<style lang="scss" scoped>
.error-table {
  margin-top: .15rem;
  margin-bottom: .08rem;
}
</style>
