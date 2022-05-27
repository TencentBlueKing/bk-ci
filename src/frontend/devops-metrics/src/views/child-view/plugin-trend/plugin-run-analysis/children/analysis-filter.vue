<script setup lang="ts">
import ScrollLoadSelect from '@/components/scroll-load-select';
import http from '@/http/api';
import { sharedProps } from '../common/props-type';
import useFilter from '@/composables/use-filter';

defineProps(sharedProps);
const emit = defineEmits(['change']);

const {
  handleChange,
  handleTimeChange,
} = useFilter(emit);

const clearStatus = () => {
  handleChange({
    pipelineIds: [],
    pipelineLabelIds: [],
    startTime: '',
    endTime: '',
    errorTypes: [],
    atomCodes: [],
  });
};
</script>

<template>
  <section class="main-filter mt20">
    <scroll-load-select
      class="mr8 w240"
      id-key="pipelineId"
      name-key="pipelineName"
      placeholder="Pipelines"
      :multiple="true"
      :api-method="http.getPipelineList"
      :select-value="status.pipelineIds"
      @change="(pipelineIds) => handleChange({ pipelineIds })"
    />
    <scroll-load-select
      class="mr8 w240"
      id-key="errorCode"
      name-key="errorMsg"
      placeholder="Error type"
      :multiple="true"
      :api-method="http.getErrorCodeList"
      :select-value="status.pipelineLabelIds"
      @change="(errorTypes) => handleChange({ errorTypes })"
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
    />
    <scroll-load-select
      class="mr8 w240"
      id-key="atomCode"
      name-key="atomName"
      placeholder="Plugin"
      :multiple="true"
      :api-method="http.getProjectPluginList"
      :select-value="status.pipelineLabelIds"
      @change="(atomCodes) => handleChange({ atomCodes })"
    />
    <bk-date-picker
      class="mr16 w240"
      @change="handleTimeChange"
    />
    <bk-button @click="clearStatus">Reset</bk-button>
  </section>
</template>

<style lang="scss" scoped>
.main-filter {
  display: flex;
}
</style>
