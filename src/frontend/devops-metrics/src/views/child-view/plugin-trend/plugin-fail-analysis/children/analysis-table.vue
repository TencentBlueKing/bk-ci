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
const router = useRouter()

const props = defineProps(sharedProps);
const isLoading = ref(false);
const columns = [
  {
    label: 'Pipeline',
    field: 'pipelineName',
  },
  {
    label: 'Plugin',
    field: 'atomName',
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
  count: 500,
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
    .getErrorCodeInfoDetail(
      props.status,
      pagination.value.current,
      pagination.value.limit,
    )
    .then((data) => {
      pagination.value.count = data.count;
      tableData.value = data.records;
    })
    .finally(() => {
      isLoading.value = false;
    });
};

const handleRowClick = (e, row) => {
  const projectId = row.projectId
  const pipelineId = row.pipelineId
  const buildId = row.buildId
  const indexMap = row.atomPosition && row.atomPosition.split('-')

  let stageIndex, containerIndex, containerGroupIndex, elementIndex

  if (indexMap.length === 3) {
    stageIndex = indexMap[0]
    containerIndex = indexMap[1]
    elementIndex = indexMap[2]
  } else if (indexMap.length === 4) {
    stageIndex = indexMap[0]
    containerIndex = indexMap[1]
    containerGroupIndex = indexMap[2]
    elementIndex = indexMap[3]
  }
  
  if (row.channelCode === 'BS') {
    if (indexMap.length === 3) {
      window.open(`${window.WEB_URL_PREFIX}/pipeline/${projectId}/${pipelineId}/detail/${buildId}?stageIndex=${stageIndex}&containerIndex=${containerIndex}&elementIndex=${elementIndex}`, '_blank')
    } else {
      window.open(`${window.WEB_URL_PREFIX}/pipeline/${projectId}/${pipelineId}/detail/${buildId}?stageIndex=${stageIndex}&containerIndex=${containerIndex}&containerGroupIndex=${containerGroupIndex}&elementIndex=${elementIndex}`, '_blank')
    }
  } else {
    if (indexMap.length === 3) {
      window.open(`${window.STREAM_URL_PREFIX}/pipeline/${pipelineId}/detail/${buildId}/?page=1#/${projectId.split('_')[1]}?stageIndex=${stageIndex}&containerIndex=${containerIndex}&elementIndex=${elementIndex}`, '_blank')
    } else {
      window.open(`${window.STREAM_URL_PREFIX}/pipeline/${pipelineId}/detail/${buildId}/?page=1#/${projectId.split('_')[1]}?stageIndex=${stageIndex}&containerIndex=${containerIndex}&containerGroupIndex=${containerGroupIndex}&elementIndex=${elementIndex}`, '_blank')
    }
  }
}

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
