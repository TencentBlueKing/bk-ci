<script setup lang="ts">
import ScrollLoadSelect from '@/components/scroll-load-select';
import http from '@/http/api';
import { sharedProps } from '../common/props-type';
import useFilter from '@/composables/use-filter';
import { useI18n } from "vue-i18n";
const { t } = useI18n();
const emit = defineEmits(['change']);
defineProps(sharedProps);

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
    errorCodes: [],
  });
};
const disableDate = (time) => time && time.getTime() > Date.now()
</script>

<template>
  <section class="main-filter mt20">
    <scroll-load-select
      class="mr8 w240"
      id-key="pipelineId"
      name-key="pipelineName"
      :placeholder="t('Pipelines')"
      :multiple="true"
      :api-method="http.getPipelineList"
      :select-value="status.pipelineIds"
      @change="(pipelineIds) => handleChange({ pipelineIds })"
    />
    <scroll-load-select
      class="mr8 w240"
      id-key="errorType"
      name-key="errorName"
      :placeholder="t('Error type')"
      :multiple="true"
      :api-method="http.getErrorTypeList"
      :select-value="status.errorTypes"
      @change="(errorTypes) => handleChange({ errorTypes })"
    />
    <scroll-load-select
      class="mr8 w240"
      id-key="labelId"
      name-key="labelName"
      :placeholder="t('Pipeline lable')"
      :multiple="true"
      :api-method="http.getPipelineLabels"
      :select-value="status.pipelineLabelIds"
      @change="(pipelineLabelIds) => handleChange({ pipelineLabelIds })"
    />
    <scroll-load-select
      class="mr8 w240"
      id-key="errorCode"
      name-key="errorCode"
      :atom-code="atomCode"
      :placeholder="t('Error code')"
      :multiple="true"
      :api-method="http.getErrorCodeList"
      :select-value="status.errorCodes"
      @change="(errorCodes) => handleChange({ errorCodes })"
    />
    <bk-date-picker
      class="mr16 w240"
      type="daterange"
      :disable-date="disableDate"
      :model-value="[status.startTime, status.endTime]"
      @change="handleTimeChange"
    />
    <bk-button :disabled="resetBtnDisabled" @click="clearStatus">{{ t('Reset') }}</bk-button>
  </section>
</template>

<style lang="scss" scoped>
.main-filter {
  display: flex;
}
</style>
