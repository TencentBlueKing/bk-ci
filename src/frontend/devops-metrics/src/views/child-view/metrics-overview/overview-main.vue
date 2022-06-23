<script setup lang="ts">
import MetricsHeader from '@/components/metrics-header';
import OverviewFilter from './children/overview-filter.vue';
import OverviewNum from './children/overview-nums.vue';
import RunTrend from './children/run-trend.vue';
import TimeTrend from './children/time-trend.vue';
import StageTime from './children/stage-time.vue';
import {
  ref,
} from 'vue';

const status = ref({
  pipelineIds: [],
  pipelineLabelIds: [],
  startTime: '',
  endTime: '',
});

const handleFilterChange = (newStatus) => {
  status.value = {
    ...status.value,
    ...newStatus,
  };
};
</script>

<template>
  <metrics-header title="Overview" />
  <main class="g-content">
    <overview-filter
      :status="status"
      @change="handleFilterChange"
    />
    <overview-num :status="status"></overview-num>
    <run-trend :status="status"></run-trend>
    <time-trend :status="status"></time-trend>
    <stage-time :status="status"></stage-time>
  </main>
</template>
