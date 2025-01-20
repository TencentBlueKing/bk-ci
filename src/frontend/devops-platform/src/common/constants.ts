// import { ProgressStatus, UpgradeStatus } from '@/types';

export const BASE_PREFIX = import.meta.env.DEV ? '/ms' : '';
export const STORE_TYPE = 'DEVX';

export const BORDER = ['outer', 'row']
export const TIME_FILTERS = {
  30: '1个月',
  90: '3个月',
  180: '6个月',
  360: '12个月',
  'custom': '自定义'
};