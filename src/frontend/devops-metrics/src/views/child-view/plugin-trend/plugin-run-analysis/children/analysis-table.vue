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
import dayjs from 'dayjs';
import duration from 'dayjs/plugin/duration';
dayjs.extend(duration);

interface IShowTime {
  h?: number,
  m?: number,
  s: number
}

const { t } = useI18n();

const emit = defineEmits(['change', 'clear']);

const {
  handleChange
} = useFilter(emit);
const props = defineProps(sharedProps);
const isLoading = ref(false);
const columns = ref([]);
const tableData = ref([]);
const pagination = ref({
  current: 1,
  count: 0,
  limit: 10,
});
const router = useRouter()

const handlePageChange = (current) => {
  pagination.value.current = current;
  columns.value = []
  getData();
};

const handlePageLimitChange = (limit) => {
  pagination.value.limit = limit;
  columns.value = []
  getData();
};

const classifyCodeMap = {
  scm: 'SCM',
  compileBuild: t('编译'),
  test: t('测试'),
  deploy: t('编译'),
  security: t('部署'),
  common: t('其它'),
}

const emptyType = computed(() => {
  return (
    props.status.pipelineIds.length
    ||  props.status.pipelineLabelIds.length
    ||  props.status.errorTypes.length
    ||  props.status.atomCodes.length
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
    startTime: '',
    endTime: '',
    errorTypes: [],
    atomCodes: [],
  })
}

const timeFormatter = (val) => {
  const time = dayjs.duration(val);
  const h = time.hours();
  const m = time.minutes();
  const s = time.seconds();
  const showTime: IShowTime = {
    s,
  };
  if (m) showTime.m = m;
  if (h) showTime.h = h;
  return showTime;
};

const getData = () => {
  isLoading.value = true;
  http.
    getAtomStatisticsDetail(
      props.status,
      pagination.value.current,
      pagination.value.limit,
    )
    .then((data) => {
      Object.entries(data.headerInfo).forEach(([field, label]) => {
        const column = {
          label,
          field,
          showOverflowTooltip: true,
          sort: true,
        }
        if (field === 'atomCode') {
          column.field = 'atomName'
          column['render'] = ({ cell, row }) => {
            return h(
              'span',
              {
                style: {
                  cursor: 'pointer',
                  color: '#3a84ff',
                },
                onClick () {
                  router.push({
                    name: 'PluginFailAnalysis',
                    query: {
                      pipelineId: row.pipelineId,
                      atomCode: row.atomCode,
                    },
                  })
                },
              },
              [
                cell,
              ]
            );
          }
        }
        if (field === 'atomCode' || field === 'classifyCode') {
          column.sort = false
        }
        if (field === 'successRate') {
          column.field = 'successRate'
          column['render'] = ({ cell, row }) => {
            return h(
              'span',
              {
                style: {
                  color: cell < 90 ? 'red' : '',
                }
              },
              [
                cell,
                '%'
              ]
            );
          }
        }
        columns.value.push(column);
      });
      tableData.value = data.records?.map(record => {
        if (!record.classifyCode) {
            record.classifyCode = '--'
        }
        const timeMap = timeFormatter(record.avgCostTime)
        let timeStr = ''
        if (timeMap.h) {
            timeStr += timeMap.h + 'h'
        }
        if (timeMap.m) {
            timeStr += timeMap.m + 'm'
        }
        if (timeMap.s) {
            timeStr += timeMap.s + 's'
        }
        record.avgCostTime = timeStr || '0'

        Object.keys(data.headerInfo).forEach(i => {
          if (i.includes('errorCount') && !record.atomFailInfos[i]) {
            record.atomFailInfos[i] = 0
          }
        });
        record.classifyCode = classifyCodeMap[record.classifyCode] || record.classifyCode
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
      handleChange(false)
    });
};
watch(
  () => props.status, () => {
    columns.value = []
    tableData.value = []
    getData()
  },
);
</script>

<template>
  <bk-loading
    class="overview-card mt20"
    :loading="isLoading"
  >
    <h3 class="g-card-title">{{ t('Plugin stat') }}</h3>
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
  padding: 10px 24px !important;
}
</style>
