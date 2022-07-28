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
import useFilter from '@/composables/use-filter';
import { useI18n } from "vue-i18n";
const { t } = useI18n();

interface IStage {
  name: string,
  data: Array<IData>,
  labels: Array<string>
}

const props = defineProps(sharedProps);
const emit = defineEmits(['change']);
const {
  handleChange
} = useFilter(emit);
const isLoading = ref(false);
const stageList = ref<Array<IStage>>([]);
const barColorList = [
  '167,58,255', '218,165,32', '0,206,209', '255,215,0', '210,105,30', '165,42,42', '255,222,173',
  '255, 86, 86', '255,151,0', '43,124,255', '12,227,144', '255,209,93', '205,92,92', '128,128,0',
  '255,0,255', '3,168,158', '218,112,214', '199,97,20', '85,102,0', '255,128,0'
];

const getDataFromApi = () => {
  isLoading.value = true;
  http
    .getPipelineStageTrend(props.status)
    .then((stageDatas) => {
      stageDatas.forEach((stageData) => {
        const stage = {
          name: stageData?.stageTagName,
          data: [],
          labels: [],
        };
        stageData?.pipelineStageAvgCostTimeInfos?.forEach((timeInfo, index) => {
          stage.data.push({
            list: timeInfo.stageAvgCostTimeInfos.map(pipelineTimeInfo => pipelineTimeInfo.avgCostTime),
            label: timeInfo.pipelineName,
            backgroundColor: `rgba(${barColorList[index]}, 0.3)` || 'rgba(255, 86, 86,0.3)',
            borderColor: `rgba(${barColorList[index]}, 1)` ||  'rgba(255, 86, 86, 1)',
          });
          stage.labels = timeInfo.stageAvgCostTimeInfos.map(pipelineTimeInfo => pipelineTimeInfo.statisticsTime);
        });
        stageList.value.push(stage);
      });
    })
    .finally(() => {
      isLoading.value = false;
      handleChange(false)
    });
};

watch(
  () => props.status, () => {
    stageList.value = []
    getDataFromApi()
  }
);
onBeforeMount(getDataFromApi);
</script>

<template>
  <bk-loading
    class="mt20 stage-average-time"
    :loading="isLoading"
  >
    <h3 class="stage-time-title">{{ t('Stage average time trend') }}</h3>
    <section
      class="stage-card overview-card mt20"
      v-for="stage in stageList"
      :key="stage.name"
    >
      <h3 class="g-card-title">{{ stage.name }}（{{ t('Top 10') }}）</h3>
      <area-line
        :data="stage.data"
        :labels="stage.labels"
        :title="t('Average time (min)')"
      />
    </section>
  </bk-loading>
</template>

<style lang="scss" scoped>
.stage-average-time::after {
  content: '';
  display: table;
  clear: both;
}

.stage-time-title {
  line-height: 30px;
  background: #eaebf0;
  border-radius: 2px;
  padding: 0 16px;
  margin-bottom: -12px;
}

.stage-card {
  width: calc(50% - 10px);
  height: 2.8rem;
  float: left;

  &:nth-child(even) {
    margin-right: 10px;
  }

  &:nth-child(odd) {
    margin-left: 10px;
  }
}
</style>
