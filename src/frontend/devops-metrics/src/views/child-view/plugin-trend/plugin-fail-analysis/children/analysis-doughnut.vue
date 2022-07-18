<script setup lang="ts">
import doughnut, { IData } from '@/components/charts/doughnut';
import {
  ref,
  onMounted,
  watch,
} from 'vue';
import {
  sharedProps,
} from '../common/props-type';
import http from '@/http/api';
import { useI18n } from "vue-i18n";
const { t } = useI18n();
const props = defineProps(sharedProps);
const isLoading = ref(false);
const errorData = ref<IData>({
  labels: [],
  list: [],
});

const getData = () => {
  isLoading.value = true;
  http
    .getErrorCodeStatisticsInfo(props.status)
    .then((errorList) => {
      errorList?.forEach((errorItem) => {
        errorData.value.labels.push(errorItem.errorCodeInfo?.errorTypeName);
        errorData.value.list.push(errorItem.errorCount);
      });
    })
    .finally(() => {
      isLoading.value = false;
    });
};

watch(
  () => props.status, () => {
    errorData.value.list = []
    errorData.value.labels = []
    getData()
  }
);
onMounted(getData);
</script>

<template>
  <bk-loading
    class="analysis-doughnut overview-card mt20"
    :loading="isLoading"
  >
    <h3 class="g-card-title">{{ t('State by error code') }}</h3>
    <doughnut :data="errorData"></doughnut>
  </bk-loading>
</template>

<style lang="scss" scoped>
.analysis-doughnut {
  height: 2.8rem;
}
</style>
