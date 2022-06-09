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

const props = defineProps(sharedProps);
const isLoading = ref(false);
const data = ref<IData>({
  labels: [],
  list: [],
});

const getData = () => {
  isLoading.value = true;
  http
    .getErrorTypeSummaryData(props.status)
    .then(({ pipelineFailInfoList }) => {
      pipelineFailInfoList?.forEach((failInfo) => {
        data.value.list.push(failInfo.errorCount);
        data.value.labels.push(failInfo.name);
      });
    })
    .finally(() => {
      isLoading.value = false;
    });
};

watch(
  () => props.status, () =>{
    data.value.list = []
    data.value.labels = []
    getData()
  }
);
onMounted(getData);
</script>

<template>
  <bk-loading
    class="error-doughnut overview-card mt20"
    :loading="isLoading"
  >
    <h3 class="g-card-title">Stat by error type</h3>
    <doughnut :data="data"></doughnut>
  </bk-loading>
</template>

<style lang="scss" scoped>
.error-doughnut {
  height: 2.8rem;
}
</style>
