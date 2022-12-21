import { PropType } from 'vue';

interface IStatus {
  pipelineIds: number[],
  pipelineLabelIds: number[],
  startTime: string,
  endTime: string,
  errorTypes: number[]
}

export const sharedProps = {
  status: Object as PropType<IStatus>,
  resetBtnDisabled: Boolean,
};
