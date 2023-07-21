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
import { useI18n } from "vue-i18n";
const { t } = useI18n();
const route = useRoute();
const status = ref({
  pipelineIds: [],
  pipelineLabelIds: [],
  startTime: <string>route.query.time || '',
  endTime: <string>route.query.time || '',
  errorTypes: <number[]>[],
});

const handleFilterChange = (newStatus) => {
  resetBtnDisabled.value = true
  status.value = {
    ...status.value,
    ...newStatus,
  };
};

const handleClear = (payload) => {
  handleFilterChange(payload);
}
const resetBtnDisabled = ref(false)

const handleChangeResetBtnDisabled = (val) => {
  resetBtnDisabled.value = val
}

if (route.query.errorType) {
  handleFilterChange({
    errorTypes: [Number(route.query.errorType)]
  })
};
</script>

<template>
  <metrics-header :title="t('Fail analysis')" />
  <main class="g-content">
    <fail-filter
      :status="status"
      :reset-btn-disabled="resetBtnDisabled"
      @change="handleFilterChange"
    />
    <error-doughnut
      :status="status"
      @change="handleFilterChange"
    />
    <error-table
      :reset-btn-disabled="resetBtnDisabled"
      :status="status"
      @change="handleChangeResetBtnDisabled"
      @clear="handleClear"
    />
  </main>
</template>
