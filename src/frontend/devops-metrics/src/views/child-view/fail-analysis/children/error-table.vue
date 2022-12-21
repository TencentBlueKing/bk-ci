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
import { useI18n } from "vue-i18n";

import useFilter from '@/composables/use-filter';
const emit = defineEmits(['change']);
const { t } = useI18n();

const {
  handleChange
} = useFilter(emit);
const props = defineProps(sharedProps);
const isLoading = ref(false);
const router = useRouter()
const columns = [
  {
    label: t('Pipeline'),
    field: 'pipelineName',
    render ({ cell, row }) {
      return h(
        'span',
        {
          style: {
            cursor: 'pointer',
            color: '#3a84ff',
          },
          onClick () {
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
              window.open(`https://${row.domain}/pipeline/${pipelineId}/detail/${buildId}/?page=1#${projectId.split('_')[1]}`, '_blank')
            })
          },
        },
        [
          cell,
          ' #',
          row.buildNum
        ]
      );
    },
  },
  {
    label: t('Branch'),
    field: 'branch',
  },
  {
    label: t('Start Time'),
    field: 'startTime',
  },
  {
    label: t('Username'),
    field: 'startUser',
  },
  {
    label: t('Error Type'),
    field: 'errorTypeName',
  },
  {
    label: t('Error Code'),
    field: 'errorCode',
  },
  {
    label: t('Error Message'),
    field: 'errorMsg',
    render ({ cell, row }) {
      return h(
        'span',
        {
          title: row.errorMsg, 
        },
        [
          cell,
          row.errorMsg
        ]
      );
    },
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
    <h3 class="g-card-title">{{ t('Details') }}</h3>
    <bk-table
      class="error-table"
      :columns="columns"
      :data="tableData"
      remote-pagination
      settings
      :pagination="pagination"
      @page-value-change="handlePageChange"
      @page-limit-change="handlePageLimitChange"
    >
    </bk-table>
  </bk-loading>
</template>

<style lang="scss" scoped>
.error-table {
  margin-top: .15rem;
  margin-bottom: .08rem;
}
::v-deep(.bk-table .bk-table-body table td .cell) {
  font-size: 12px;
}
::v-deep(.bk-table .bk-table-head table th .cell) {
  font-size: 12px;
  color: #313238;

}
</style>
