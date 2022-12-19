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
import { useI18n } from "vue-i18n";
const { t } = useI18n();

const status = ref({
  pipelineIds: [],
  pipelineLabelIds: [],
  startTime: '',
  endTime: '',
});

const handleFilterChange = (newStatus) => {
  resetBtnDisabled.value = true
  status.value = {
    ...status.value,
    ...newStatus,
  };
};

const resetBtnDisabled = ref(false)
const handleChangeResetBtnDisabled = (val) => {
  resetBtnDisabled.value = val
}
</script>

<template>
  <metrics-header :title="t('Overview')" />
  <main class="g-content">
    <overview-filter
      :reset-btn-disabled="resetBtnDisabled"
      :status="status"
      @change="handleFilterChange"
    />
    <overview-num :status="status" />
    <run-trend :status="status" />
    <time-trend :status="status" />
    <stage-time
      :status="status"
      :reset-btn-disabled="resetBtnDisabled"
      @change="handleChangeResetBtnDisabled"
    />
  </main>
</template>
