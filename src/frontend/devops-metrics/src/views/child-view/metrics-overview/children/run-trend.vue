<script setup lang="ts">
import DoubleYAreaLine, { IData as IRunTimeDate } from '@/components/charts/double-y-area-line';
import Bar, { IData } from '@/components/charts/bar';
import {
  ref,
  watch,
  onBeforeMount,
} from 'vue';
import {
  sharedProps,
} from '../common/props-type';
import { useI18n } from "vue-i18n";
const { t } = useI18n();
import http from '@/http/api';
import { useRouter } from 'vue-router';

interface IRunFailData extends IData {
  errorType: string | number
}

// 状态
const props = defineProps(sharedProps);
const isLoading = ref(false);
const runTimeTrend = ref<{
  data: Array<IRunTimeDate>,
  labels: Array<string>
}>({
  data: [
    {
      list: [],
      label: t('Total'),
      backgroundColor: 'rgba(43, 124, 255,0.3)',
      borderColor: 'rgba(43, 124, 255,1)',
    },
    {
      list: [],
      label: t('Failure'),
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
const router = useRouter()

const handleRunTimePointClick = ([{ datasetIndex, index }]) => {
  if (datasetIndex === 1) {
    router.push({
      name: 'FailAnalysis',
      query: {
        time: runTimeTrend.value.labels[index]
      }
    })
  }
}

const handleRunTimePointHover = ([{ datasetIndex, index }]) => {
  if (datasetIndex) {
    document.getElementsByClassName('double-line-canvas')[0]['style']['cursor'] = 'pointer'
  }
}

const handleRunFailPointClick = ([{ datasetIndex, index }]) => {
  router.push({
    name: 'FailAnalysis',
    query: {
      errorType: runFailTrend.value.data[datasetIndex].errorType,
      time: runFailTrend.value.labels[index]
    }
  })
}

const handleRunFailPointHover = ([{ datasetIndex, index }]) => {
  document.getElementsByClassName('bar-canvas')[0]['style']['cursor'] = 'pointer'
}

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
        runTimeTrend.value.data[0].list.push(runTime.totalExecuteCount);
        runTimeTrend.value.data[1].list.push(runTime.failedExecuteCount);
        runTimeTrend.value.labels.push(runTime.statisticsTime);
      });

      runFailData.forEach((runFail, index) => {
        runFailTrend.value.data.push({
          list: runFail.failInfos.map(failInfo => failInfo.errorCount),
          label: runFail.name,
          errorType: runFail.errorType,
          backgroundColor: barColorList[index],
          borderColor: barColorList[index],
        });
        runFailTrend.value.labels = runFail.failInfos.map(failInfo => failInfo.statisticsTime);
      });
    })
    .finally(() => {
      isLoading.value = false;
    });
};

watch(
  () => props.status, () => {
    runTimeTrend.value.data[0].list = []
    runTimeTrend.value.data[1].list = []
    runTimeTrend.value.labels = []
    runFailTrend.value.data = []
    runFailTrend.value.labels = []
    init()
  }
);
onBeforeMount(init);
</script>

<template>
  <bk-loading
    class="run-trend mt20"
    :loading="isLoading"
  >
    <section class="run-trend-card overview-card">
      <h3 class="g-card-title">{{ t('Pipeline run times trend') }}</h3>
      <double-y-area-line
        :data="runTimeTrend.data"
        :labels="runTimeTrend.labels"
        :titles="[t('Total runs'), t('Failed runs')]"
        @point-click="handleRunTimePointClick"
        @point-hover="handleRunTimePointHover"
      />
    </section>
    <section class="run-trend-card overview-card">
      <h3 class="g-card-title">{{ t('Pipeline run fails trend') }}</h3>
      <bar
        :data="runFailTrend.data"
        :labels="runFailTrend.labels"
        :title="t('Run times')"
        @point-click="handleRunFailPointClick"
        @point-hover="handleRunFailPointHover"
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
