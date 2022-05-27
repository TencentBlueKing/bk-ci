<script setup lang="ts">
import DoubleYAreaLine, { IData as IRunTimeData } from '@/components/charts/double-y-area-line';
import Bar, { IData as IRunFailData } from '@/components/charts/bar';
import {
  ref,
  watch,
  onBeforeMount,
} from 'vue';
import {
  sharedProps,
} from '../common/props-type';
import http from '@/http/api';
// 状态
const props = defineProps(sharedProps);
const isLoading = ref(false);
const runTimeTrend = ref<{
  data: Array<IRunTimeData>,
  labels: Array<string>
}>({
  data: [
    {
      list: [],
      label: 'Total',
      backgroundColor: 'rgba(43, 124, 255,0.3)',
      borderColor: 'rgba(43, 124, 255,1)',
    },
    {
      list: [],
      label: 'Failure',
      backgroundColor: 'rgba(255, 86, 86,0.3)',
      borderColor: 'rgba(255, 86, 86, 1)',
    },
  ],
  labels: [],
});
const runFailTrend = ref<{
  data: Array<IRunFailData>,
  labels: Array<string>
}>({
  data: [],
  labels: [],
});
const barColorList = ['#FF5656', '#FF9700', '#FFD695', '#A73AFF'];

const init = () => {
  isLoading.value = true;
  Promise
    .all([
      http.getPipelineRunTimeTrend(props.status),
      http.getPipelineRunFailTrend(props.status),
    ])
    .then(([
      { pipelineTrendInfo = [] },
      runFailData,
    ]) => {
      pipelineTrendInfo.forEach((runTime) => {
        runTimeTrend.value.data[0].list.push(runTime.totalAvgCostTime);
        runTimeTrend.value.data[1].list.push(runTime.failAvgCostTime);
        runTimeTrend.value.labels.push(runTime.statisticsTime);
      });

      runFailData.forEach((runFail, index) => {
        runFailTrend.value.data.push({
          list: runFail.failInfos.map(failInfo => failInfo.errorCount),
          label: runFail.name,
          backgroundColor: barColorList[index],
        });
        runFailTrend.value.labels = runFail.failInfos.map(failInfo => failInfo.statisticsTime);
      });
    })
    .finally(() => {
      isLoading.value = false;
    });
};

watch(
  () => props.status,
  init,
);
onBeforeMount(init);
</script>

<template>
  <bk-loading
    class="run-trend mt20"
    :loading="isLoading"
  >
    <section class="run-trend-card overview-card">
      <h3 class="g-card-title">Pipeline run times trend</h3>
      <double-y-area-line
        :data="runTimeTrend.data"
        :labels="runTimeTrend.labels"
        :titles="['Total runs', 'Failed runs']"
      />
    </section>
    <section class="run-trend-card overview-card">
      <h3 class="g-card-title">Pipeline run fails trend</h3>
      <bar
        :data="runFailTrend.data"
        :labels="runFailTrend.labels"
        title="Run times"
      />
    </section>
  </bk-loading>
</template>

<style lang="scss" scoped>
.run-trend {
  display: flex;

  .run-trend-card {
    width: calc(50% - 10px);
    height: 2.8rem;

    &:first-child {
      margin-right: 10px;
    }

    &:last-child {
      margin-left: 10px;
    }
  }
}
</style>
