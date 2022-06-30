<script setup lang="ts">
import {
  ref,
  onMounted,
  watch,
  h,
} from 'vue';
import http from '@/http/api';
import {
  sharedProps,
} from '../common/props-type';
import {
  useRouter
} from 'vue-router';

import useFilter from '@/composables/use-filter';
const emit = defineEmits(['change']);

const {
  handleChange
} = useFilter(emit);

const props = defineProps(sharedProps);
const isLoading = ref(false);
const router = useRouter()
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
  limit: 10,
});

const goToPipelineDetail = (row) => {
  console.log(row, arguments);
}

const handlePageChange = (current) => {
  pagination.value.current = current;
  getData();
};

const handleRowClick = (e, row) => {
  const projectId = row.projectId
  const pipelineId = row.pipelineId
  const buildId = row.buildId
  http.getPipelineType({
    projectId,
    pipelineId
  }).then(res => {
    if (res.channelCode === 'BS') {
      window.open(`https://${row.domain}/console/pipeline/${projectId}/${pipelineId}/detail/${buildId}`, '_blank')
    }
    window.open(`https://${row.domain}/pipeline/${pipelineId}/detail/${buildId}/?page=1#/${projectId.split('_')[1]}`, '_blank')
  })
}

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
      handleChange(false)
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
      remote-pagination
      :pagination="pagination"
      @page-value-change="handlePageChange"
      @page-limit-change="handlePageLimitChange"
      @row-click="handleRowClick"
    >
    </bk-table>
  </bk-loading>
</template>

<style lang="scss" scoped>
.error-table {
  margin-top: .15rem;
  margin-bottom: .08rem;
  ::v-deep .bk-table-body {
    cursor: pointer;
  }
}
</style>
