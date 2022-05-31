<script setup lang="ts">
import MetricsHeader from '@/components/metrics-header';
import AddPlugin from '../common/add-plugin.vue';
import AnalysisFilter from './children/analysis-filter.vue';
import AnalysisDoughnut from './children/analysis-doughnut.vue';
import AnalysisTable from './children/analysis-table.vue';
import {
  ref,
} from 'vue';

const status = ref({
  pipelineIds: [],
  pipelineLabelIds: [],
  startTime: '',
  endTime: '',
  errorTypes: [],
  errorCodes: [],
});

const handleFilterChange = (newStatus) => {
  status.value = {
    ...status.value,
    ...newStatus,
  };
};
</script>

<template>
  <metrics-header>
    <span>Plugin trend</span>
    <add-plugin></add-plugin>
  </metrics-header>
  <main class="g-content">
    <bk-alert theme="info" title="仅支持查询最近 6 个月内的统计数据!"></bk-alert>
    <analysis-filter
      :status="status"
      @change="handleFilterChange"
    />
    <analysis-doughnut
      :status="status"
    />
    <analysis-table
      :status="status"
    />
  </main>
</template>

<style lang="scss">

</style>
