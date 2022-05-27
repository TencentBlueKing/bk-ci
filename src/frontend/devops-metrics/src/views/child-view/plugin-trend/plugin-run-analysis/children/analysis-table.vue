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
const columns = ref([]);
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
    .getAtomStatisticsDetail(
      props.status,
      pagination.value.current,
      pagination.value.limit,
    )
    .then((data) => {
      Object.entries(data.headerInfo).forEach(([label, field]) => {
        columns.value.push({
          label,
          field,
        });
      });
      tableData.value = data.records?.map(record => ({
        ...record,
        ...record.atomBaseInfo,
      }));
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
    <h3 class="g-card-title">Details</h3>
    <bk-table
      class="analysis-table"
      :columns="columns"
      :data="tableData"
      :pagination="pagination"
      @page-value-change="handlePageChange"
      @page-limit-change="handlePageLimitChange">
    </bk-table>
  </bk-loading>
</template>

<style lang="scss" scoped>
.analysis-table {
  margin-top: .15rem;
  margin-bottom: .08rem;
}
</style>
