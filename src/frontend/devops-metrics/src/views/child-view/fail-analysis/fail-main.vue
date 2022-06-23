<script setup lang="ts">
import MetricsHeader from '@/components/metrics-header';
import FailFilter from './children/fail-filter.vue';
import ErrorDoughnut from './children/error-doughnut.vue';
import ErrorTable from './children/error-table.vue';
import {
  ref,
} from 'vue';
import {
  useRoute,
} from 'vue-router';

const route = useRoute();
const status = ref({
  pipelineIds: [],
  pipelineLabelIds: [],
  startTime: <string>route.query.time || '',
  endTime: <string>route.query.time || '',
  errorTypes: <number[]>[],
});

const handleFilterChange = (newStatus) => {
  status.value = {
    ...status.value,
    ...newStatus,
  };
};

if (route.query.errorType) {
  handleFilterChange({
    errorTypes: [Number(route.query.errorType)]
  })
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
      @change="handleFilterChange"
    />
    <error-table
      :status="status"
    />
  </main>
</template>
