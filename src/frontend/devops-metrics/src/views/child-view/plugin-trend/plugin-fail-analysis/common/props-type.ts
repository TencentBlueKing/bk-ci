import { PropType } from 'vue';

interface IStatus {
  pipelineIds: number[],
  pipelineLabelIds: number[],
  startTime: string,
  endTime: string,
  errorTypes: number[],
  errorCodes: string[]
}

export const sharedProps = {
  status: Object as PropType<IStatus>,
  resetBtnDisabled: Boolean,
  atomCode: String,
};
