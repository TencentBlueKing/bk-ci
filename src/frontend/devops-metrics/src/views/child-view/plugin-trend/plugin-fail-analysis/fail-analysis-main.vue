<script setup lang="ts">
import MetricsHeader from '@/components/metrics-header';
import AddPlugin from '../common/add-plugin.vue';
import AnalysisFilter from './children/analysis-filter.vue';
import AnalysisDoughnut from './children/analysis-doughnut.vue';
import AnalysisTable from './children/analysis-table.vue';
import {
  ref,
} from 'vue';
import {
  useRoute,
  useRouter,
} from 'vue-router';
import { useI18n } from "vue-i18n";
const { t } = useI18n();
const route = useRoute();
const router = useRouter();
const status = ref({
  pipelineIds: <any[]>[route.query.pipelineId].filter(v => v),
  pipelineLabelIds: [],
  startTime: '',
  endTime: '',
  errorTypes: [],
  errorCodes: [],
  atomCodes: <any[]>[route.query.atomCode].filter(v => v),
});
const atomCode = ref(route.query.atomCode || '');
const resetBtnDisabled = ref(false);
const handleChangeResetBtnDisabled = (val) => {
  resetBtnDisabled.value = val
};
const handleFilterChange = (newStatus) => {
  resetBtnDisabled.value = true
  status.value = {
    ...status.value,
    ...newStatus,
  };
};

const handleToPluginTrend = () => {
  router.push({
    name: 'PluginRunAnalysis'
  })
};

const handleClear = (payload) => {
  handleFilterChange(payload);
};
</script>

<template>
  <metrics-header>
    <div class="header">
      <a class="plugin-trend-title" @click="handleToPluginTrend">{{ t('Plugin trend') }}</a>
      <span class="crumbs-icon"> > </span>
      <span>{{ t('Plugin fail analtysis') }}: {{ route.query.atomCode }}</span>
    </div>
    <!-- <add-plugin></add-plugin> -->
  </metrics-header>
  <main class="g-content">
    <bk-alert theme="info" :title="t('You can only query the statistics in the last 6 months!')"></bk-alert>
    <analysis-filter
      :reset-btn-disabled="resetBtnDisabled"
      :status="status"
      :atom-code="atomCode"
      @change="handleFilterChange"
    />
    <analysis-doughnut
      :status="status"
    />
    <analysis-table
      :status="status"
      :reset-btn-disabled="resetBtnDisabled"
      @change="handleChangeResetBtnDisabled"
      @clear="handleClear"
    />
  </main>
</template>

<style lang="scss">
  .header {
    font-size: 14px;
  }
  .crumbs-icon {
    color: #C4C6CC;
    padding: 0 10px;
  }
  .plugin-trend-title {
    color: #3A84FF;
    cursor: pointer;
  }
</style>
