<script setup lang="ts">
import MetricsHeader from '@/components/metrics-header';
import AddPlugin from '../common/add-plugin.vue';
import AnalysisFilter from './children/analysis-filter.vue';
import AnalysisLines from './children/analysis-lines.vue';
import AnalysisTable from './children/analysis-table.vue';
import {
  onMounted,
  ref,
} from 'vue';
import http from '@/http/api';

import {
  useRoute,
} from 'vue-router';

const route = useRoute();
const status = ref({
  pipelineIds: <any[]>[route.query.pipelineId].filter(v => v),
  pipelineLabelIds: [],
  startTime: '',
  endTime: '',
  errorTypes: [],
  atomCodes: [],
});
const resetBtnDisabled = ref(false)
const handleChangeResetBtnDisabled = (val) => {
  resetBtnDisabled.value = val
}
const handleFilterChange = (newStatus) => {
  resetBtnDisabled.value = true
  status.value = {
    ...status.value,
    ...newStatus,
  };
};

const getPluginList = () => {
  http.getProjectShowPluginList().then(res =>{
    const atomCodes = res.atomBaseInfos.map(item => item.atomCode)
    handleFilterChange({ atomCodes })
  })
}

onMounted(() => {
    getPluginList()
})
</script>

<template>
  <metrics-header>
    <span>Plugin trend</span>
    <add-plugin @change="handleFilterChange"></add-plugin>
  </metrics-header>
  <main class="g-content">
    <bk-alert theme="info" title="仅支持查询最近 6 个月内的统计数据!"></bk-alert>
    <analysis-filter
      :reset-btn-disabled="resetBtnDisabled"
      :status="status"
      @change="handleFilterChange"
    />
    <analysis-lines
      :status="status"
    />
    <analysis-table
      :status="status"
      :reset-btn-disabled="resetBtnDisabled"
      @change="handleChangeResetBtnDisabled"
    />
  </main>
</template>
