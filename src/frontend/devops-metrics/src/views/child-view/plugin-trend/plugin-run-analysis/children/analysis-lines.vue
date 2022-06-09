<script setup lang="ts">
import LineChart, { IData } from '@/components/charts/line';
import {
  ref,
  onMounted,
  watch,
} from 'vue';
import http from '@/http/api';
import {
  sharedProps,
} from '../common/props-type';

interface ITrend {
  data: Array<IData>,
  labels: Array<string>
}

const props = defineProps(sharedProps);
const isLoading = ref(false);
const analysisData = ref<{
  rateTrend: ITrend,
  timeTrend: ITrend
}>({
  rateTrend: {
    data: [],
    labels: [],
  },
  timeTrend: {
    data: [],
    labels: [],
  },
});

const getData = () => {
  isLoading.value = true;
  http
    .getAtomStatisticsTrendInfo(props.status)
    .then(({ atomTrendInfos = [] }) => {
      atomTrendInfos?.forEach((atomTrendInfo) => {
        const lables = atomTrendInfo.atomTrendInfos.map(data => data.statisticsTime);
        // rate trend
        analysisData.value.rateTrend.data.push({
          label: atomTrendInfo.atomName,
          list: atomTrendInfo.atomTrendInfos.map(data => data.successRate),
        });
        analysisData.value.rateTrend.labels = lables;
        // time trend
        analysisData.value.timeTrend.data.push({
          label: atomTrendInfo.atomName,
          list: atomTrendInfo.atomTrendInfos.map(data => data.avgCostTime),
        });
        analysisData.value.timeTrend.labels = lables;
      });
    })
    .finally(() => {
      isLoading.value = false;
    });
};

watch(
  () => props.status, () => {
    analysisData.value.rateTrend.data = []
    analysisData.value.rateTrend.labels = []
    analysisData.value.timeTrend.data = []
    analysisData.value.timeTrend.labels = []
    getData();
  }
);
onMounted(getData);
</script>

<template>
  <bk-loading
    class="analysis-home"
    :loading="isLoading"
  >
    <section class="analysis-line overview-card mt20">
      <h3 class="g-card-title">Success rate trend</h3>
      <line-chart
        :data="analysisData.rateTrend.data"
        :labels="analysisData.rateTrend.labels"
      />
    </section>
    <section class="analysis-line overview-card mt20">
      <h3 class="g-card-title">Average time trend</h3>
      <line-chart
        :data="analysisData.timeTrend.data"
        :labels="analysisData.timeTrend.labels"
      />
    </section>
  </bk-loading>
</template>

<style lang="scss" scoped>
.analysis-home::after {
  content: '';
  display: table;
  clear: both;
}

.analysis-line {
  height: 2.8rem;
  width: calc(50% - 10px);
  float: left;

  &:first-child {
    margin-right: 10px;
  }

  &:last-child {
    margin-left: 10px;
  }
}
</style>
