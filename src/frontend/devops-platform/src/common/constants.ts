// import { ProgressStatus, UpgradeStatus } from '@/types';

export const BASE_PREFIX = import.meta.env.DEV ? '/ms' : '';
export const STORE_TYPE = 'DEVX';

export const OWNER_PERMISSION_LIST = ['开发', '版本发布', '私有配置', '可见范围', '审批', '成员管理'];
export const DEVELOPER_PERMISSION_LIST = ['开发', '版本发布', '私有配置'];

export const STEP_CODES = {
  COMMIT: 'commit',
  BUILD: 'build',
  TEST: 'test',
  EDIT: 'edit',
  APPROVE: 'approve',
};