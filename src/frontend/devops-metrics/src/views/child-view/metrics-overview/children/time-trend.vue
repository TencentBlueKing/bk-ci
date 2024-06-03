<script setup lang="ts">
import AreaLine, { IData } from '@/components/charts/area-line';
import {
  ref,
  watch,
  onBeforeMount,
} from 'vue';
import {
  sharedProps,
} from '../common/props-type';
import http from '@/http/api';
import { useI18n } from "vue-i18n";
const { t } = useI18n();
const props = defineProps(sharedProps);
const isLoading = ref(false);
const timeTrend = ref<{
  data: Array<IData>,
  labels: Array<string>
}>({
  data: [
    {
      list: [],
      label: t('Average time (min)'),
      backgroundColor: 'rgba(43, 124, 255,0.3)',
      borderColor: 'rgba(43, 124, 255,1)',
    },
    {
      list: [],
      label: t('Average time of failure runs (min)'),
      backgroundColor: 'rgba(255, 86, 86,0.3)',
      borderColor: 'rgba(255, 86, 86, 1)',
    },
  ],
  labels: [],
});


const getData = () => {
  isLoading.value = true;
  http
    .getPipelineRunTimeTrend(props.status)
    .then(({ pipelineTrendInfo }) => {
      pipelineTrendInfo.forEach((runTime) => {
        timeTrend.value.data[0].list.push(runTime.totalAvgCostTime);
        timeTrend.value.data[1].list.push(runTime.failAvgCostTime);
        timeTrend.value.labels.push(runTime.statisticsTime);
      });
    })
    .finally(() => {
      isLoading.value = false;
    });
};

watch(
  () => props.status, () => {
    timeTrend.value.data[0].list = []
    timeTrend.value.data[1].list = []
    timeTrend.value.labels = []
    getData()
  }
);
onBeforeMount(getData);
</script>

<template>
  <bk-loading
    class="time-trend overview-card mt20"
    :loading="isLoading"
  >
    <h3 class="g-card-title">{{ t('Average time trend') }}</h3>
    <area-line
      :data="timeTrend.data"
      :labels="timeTrend.labels"
      :title="t('Average time (min)')"
    />
  </bk-loading>
</template>

<style lang="scss" scoped>
.time-trend {
  height: 4rem;
}
</style>
