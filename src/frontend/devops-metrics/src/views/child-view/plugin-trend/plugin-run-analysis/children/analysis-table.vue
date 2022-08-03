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
  useRouter,
} from 'vue-router';
import useFilter from '@/composables/use-filter';
import { useI18n } from "vue-i18n";
const { t } = useI18n();

const emit = defineEmits(['change']);

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

const getData = () => {
  isLoading.value = true;
  http.getAtomStatisticsDetail(
      props.status,
      pagination.value.current,
      pagination.value.limit,
    )
    .then((data) => {
      Object.entries(data.headerInfo).forEach(([field, label]) => {
        const column = {
            label,
            field,
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
                ' #',
                row.buildNum,
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
        record.successRate += '%'
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
  }
  ,
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
