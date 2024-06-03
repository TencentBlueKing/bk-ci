<script setup lang="ts">
import Doughnut, { IData } from '@/components/charts/doughnut';
import {
  ref,
  onMounted,
  watch,
} from 'vue';
import http from '@/http/api';
import {
  sharedProps,
} from '../common/props-type';
import useFilter from '@/composables/use-filter';
import { useI18n } from "vue-i18n";
const { t } = useI18n();

const emit = defineEmits(['change']);
const props = defineProps(sharedProps);

const {
  handleChange,
} = useFilter(emit);

const isLoading = ref(false);
const data = ref<IData>({
  labels: [],
  list: [],
  errorTypes: [],
});

const getData = () => {
  isLoading.value = true;
  http
    .getErrorTypeSummaryData(props.status)
    .then(({ pipelineFailInfoList }) => {
      pipelineFailInfoList?.forEach((failInfo) => {
        data.value.list.push(failInfo.errorCount);
        data.value.labels.push(failInfo.name);
        data.value.errorTypes.push(failInfo.errorType)
      });
    })
    .finally(() => {
      isLoading.value = false;
    });
};

const handleDoughnutClick = ([{ index }]) => {
  if (data.value.errorTypes.length > 1) {
    handleChange({
      errorTypes: [data.value.errorTypes[index]]
    })
  }
}

watch(
  () => props.status, () =>{
    data.value.list = [];
    data.value.labels = [];
    data.value.errorTypes = [];
    getData();
  }
);
onMounted(getData);
</script>

<template>
  <bk-loading
    class="error-doughnut overview-card mt20"
    :loading="isLoading"
  >
    <h3 class="g-card-title">{{ t('Stat by error type') }}</h3>
    <doughnut :data="data" @doughnut-click="handleDoughnutClick"></doughnut>
  </bk-loading>
</template>

<style lang="scss" scoped>
.error-doughnut {
  height: 2.8rem;
}
</style>
