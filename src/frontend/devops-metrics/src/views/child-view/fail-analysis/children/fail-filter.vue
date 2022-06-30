<script setup lang="ts">
import ScrollLoadSelect from '@/components/scroll-load-select';
import http from '@/http/api';
import { sharedProps } from '../common/props-type';
import useFilter from '@/composables/use-filter';
import {
  ref,
  watch
} from 'vue';

const emit = defineEmits(['change']);
defineProps(sharedProps);

const {
  handleChange,
  handleTimeChange,
} = useFilter(emit);
const setKey = ref(0)

const clearStatus = () => {
  setKey.value += 1
  handleChange({
    pipelineIds: [],
    pipelineLabelIds: [],
    startTime: '',
    endTime: '',
    errorTypes: [],
  });
};
</script>

<template>
  <section class="main-filter">
    <scroll-load-select
      class="mr8 w240"
      id-key="pipelineId"
      name-key="pipelineName"
      placeholder="Pipelines"
      :multiple="true"
      :api-method="http.getPipelineList"
      :select-value="status.pipelineIds"
      @change="(pipelineIds) => handleChange({ pipelineIds })"
      :key="setKey"
    />
    <scroll-load-select
      class="mr8 w240"
      id-key="errorType"
      name-key="errorName"
      placeholder="Error type"
      :multiple="true"
      :api-method="http.getErrorTypeList"
      :select-value="status.errorTypes"
      @change="(errorTypes) => handleChange({ errorTypes })"
      :key="setKey"
    />
    <scroll-load-select
      class="mr8 w240"
      id-key="labelId"
      name-key="labelName"
      placeholder="Pipeline lable"
      :multiple="true"
      :api-method="http.getPipelineLabels"
      :select-value="status.pipelineLabelIds"
      @change="(pipelineLabelIds) => handleChange({ pipelineLabelIds })"
      :key="setKey"
    />
    <bk-date-picker
      class="mr16 w240"
      type="daterange"
      :model-value="[status.startTime, status.endTime]"
      @change="handleTimeChange"
      :key="setKey"
    />
    <bk-button :disabled="resetBtnDisabled" @click="clearStatus">Reset</bk-button>
  </section>
</template>

<style lang="scss" scoped>
.main-filter {
  display: flex;
}
</style>
