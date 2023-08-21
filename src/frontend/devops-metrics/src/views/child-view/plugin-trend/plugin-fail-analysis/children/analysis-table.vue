<script setup lang="ts">
import {
  ref,
  onMounted,
  watch,
  computed,
  h,
} from 'vue';
import http from '@/http/api';
import {
  sharedProps,
} from '../common/props-type';

import {
  useRouter,
} from 'vue-router';
import useFilter from '@/composables/use-filter';
import EmptyTableStatus from '@/components/empty-table-status.vue'
import { useI18n } from "vue-i18n";
const { t } = useI18n();
const router = useRouter()

const emit = defineEmits(['change', 'clear']);

const {
  handleChange
} = useFilter(emit);

const props = defineProps(sharedProps);
const isLoading = ref(false);
const columns = [
  {
    label: t('Pipeline'),
    field: 'pipelineName',
    showOverflowTooltip: true,
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
                window.open(`https://${row.domain}/console/pipeline/${projectId}/${pipelineId}/detail/${buildId}?stageIndex=${stageIndex}&containerIndex=${containerIndex}&elementIndex=${elementIndex}`, '_blank')
              } else {
                window.open(`https://${row.domain}/console/pipeline/${projectId}/${pipelineId}/detail/${buildId}?stageIndex=${stageIndex}&containerIndex=${containerIndex}&containerGroupIndex=${containerGroupIndex}&elementIndex=${elementIndex}`, '_blank')
              }
            } else if (row.channelCode === 'GIT') {
              if (indexMap.length === 3) {
                window.open(`https://${row.domain}/pipeline/${pipelineId}/detail/${buildId}/?page=1&stageIndex=${stageIndex}&containerIndex=${containerIndex}&elementIndex=${elementIndex}#${projectId.split('_')[1]}`, '_blank')
              } else {
                window.open(`https://${row.domain}/pipeline/${pipelineId}/detail/${buildId}/?page=1&stageIndex=${stageIndex}&containerIndex=${containerIndex}&containerGroupIndex=${containerGroupIndex}&elementIndex=${elementIndex}#${projectId.split('_')[1]}`, '_blank')
              }
            }
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
    label: t('Plugin'),
    field: 'atomName',
    showOverflowTooltip: true,
  },
  {
    label: t('Start Time'),
    field: 'startTime',
    showOverflowTooltip: true,
    sort: true,
  },
  {
    label: t('Username'),
    field: 'startUser',
    showOverflowTooltip: true,
  },
  {
    label: t('Error Type'),
    field: 'errorTypeName',
    showOverflowTooltip: true,
  },
  {
    label: t('Error Code'),
    field: 'errorCode',
    showOverflowTooltip: true,
    sort: true,
  },
  {
    label: t('Error Message'),
    field: 'errorMsg',
    showOverflowTooltip: true,
    sort: true,
    render ({ cell, row }) {
      return h(
        'span',
        [
          cell
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

const emptyType = computed(() => {
  return (
    props.status.pipelineIds.length
    ||  props.status.pipelineLabelIds.length
    ||  props.status.errorTypes.length
    ||  props.status.errorCodes.length
    ||  props.status.startTime
    ||  props.status.endTime
  )
    ? 'search-empty'
    : 'empty'

})

const handleClear = () => {
  emit('clear', {
    pipelineIds: [],
    pipelineLabelIds: [],
    errorTypes: [],
    errorCodes: [],
    startTime: '',
    endTime: '',
  })
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
      handleChange(false);
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
      class="analysis-table"
      :columns="columns"
      :data="tableData"
      remote-pagination
      settings
      :pagination="pagination"
      @page-value-change="handlePageChange"
      @page-limit-change="handlePageLimitChange">
      <template #empty>
        <EmptyTableStatus :type="emptyType" @clear="handleClear" />
      </template>
    </bk-table>
  </bk-loading>
</template>

<style lang="scss" scoped>
.analysis-table {
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
::v-deep(.setting-content .setting-head) {
  padding: 10px 24px;
}
</style>
