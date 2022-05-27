<script setup lang="ts">
import MetricsHeader from '@/components/metrics-header';
import FailFilter from './children/fail-filter.vue';
import ErrorDoughnut from './children/error-doughnut.vue';
import ErrorTable from './children/error-table.vue';
import {
  ref,
} from 'vue';

const status = ref({
  pipelineIds: [],
  pipelineLabelIds: [],
  startTime: '',
  endTime: '',
  errorTypes: [],
});

const handleFilterChange = (newStatus) => {
  status.value = {
    ...status.value,
    ...newStatus,
  };
};
</script>

<template>
  <metrics-header title="Fail analysis" />
  <main class="g-content">
    <fail-filter
      :status="status"
      @change="handleFilterChange"
    />
    <error-doughnut
      :status="status"
    />
    <error-table
      :status="status"
    />
  </main>
</template>
