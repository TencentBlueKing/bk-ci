/**
 * Permission Component Constants
 * Constant values for permission management components
 */

import type { TimeFilterOptions } from './types'

/**
 * Time duration constants in seconds
 */
export const TIME_DURATIONS = {
  ONE_MONTH: 2592000, // 30 days
  THREE_MONTHS: 7776000, // 90 days
  SIX_MONTHS: 15552000, // 180 days
  TWELVE_MONTHS: 31104000, // 360 days
} as const

/**
 * Time duration to days mapping
 */
export const TIME_TO_DAYS: Record<number, number> = {
  [TIME_DURATIONS.ONE_MONTH]: 30,
  [TIME_DURATIONS.THREE_MONTHS]: 90,
  [TIME_DURATIONS.SIX_MONTHS]: 180,
  [TIME_DURATIONS.TWELVE_MONTHS]: 360,
}

/**
 * Default page size for pagination
 */
export const DEFAULT_PAGE_SIZE = 20

/**
 * Custom time range limits
 */
export const CUSTOM_TIME_RANGE = {
  MIN: 1,
  MAX: 365,
} as const

/**
 * API error codes that indicate special states
 */
export const ERROR_CODES = {
  NOT_FOUND: 404,
  FORBIDDEN: 403,
  SPECIAL_ERROR: 2119042,
} as const

/**
 * IAM system configuration
 */
export const IAM_CONFIG = {
  SYSTEM_ID: 'bk_ci_rbac',
  SOURCE: 'externalApp',
} as const

/**
 * IAM message type identifier
 */
export const IAM_MESSAGE_TYPE = 'IAM'

/**
 * Member status values
 */
export const MEMBER_STATUS = {
  NOT_JOINED: 'NOT_JOINED',
  NORMAL: 'NORMAL',
  EXPIRED: 'EXPIRED',
} as const

/**
 * Resource type values
 */
export const RESOURCE_TYPES = {
  PROJECT: 'project',
  CREATIVE_STREAM: 'creative_stream',
  CREATIVE_STREAM_GROUP: 'creative_stream_group',
  CREATIVE_STREAM_TEMPLATE: 'creative_stream_template',
  ENVIRONMENT: 'environment',
} as const

/**
 * Apply dialog types
 */
export const APPLY_DIALOG_TYPES = {
  APPLY: 'apply',
  RENEWAL: 'renewal',
} as const

/**
 * Create time filter options with i18n support
 * @param t - i18n translation function
 * @returns Time filter options object
 */
export const createTimeFilterOptions = (t: (key: string) => string): TimeFilterOptions => ({
  [TIME_DURATIONS.ONE_MONTH]: t('1个月'),
  [TIME_DURATIONS.THREE_MONTHS]: t('3个月'),
  [TIME_DURATIONS.SIX_MONTHS]: t('6个月'),
  [TIME_DURATIONS.TWELVE_MONTHS]: t('12个月'),
})

/**
 * Get resource type display name
 * @param resourceType - Resource type
 * @param t - i18n translation function
 * @returns Display name
 */
export const getResourceTypeName = (resourceType: string, t: (key: string) => string): string => {
  const nameMap: Record<string, string> = {
    pipeline: t('流水线'),
    pipeline_group: t('流水线组'),
    pipeline_template: t('流水线模板'),
    environment: t('环境'),
  }
  return nameMap[resourceType] || resourceType
}

/**
 * Get permission title based on resource type
 * @param resourceType - Resource type
 * @param t - i18n translation function
 * @returns Permission title
 */
export const getPermissionTitle = (resourceType: string, t: (key: string) => string): string => {
  const titleMap: Record<string, string> = {
    pipeline: t('流水线管理'),
    pipeline_template: t('流水线模板管理'),
    pipeline_group: t('流水线组管理'),
  }
  return titleMap[resourceType] || ''
}

/**
 * Get no enable permission title based on resource type
 * @param resourceType - Resource type
 * @param t - i18n translation function
 * @returns Title text
 */
export const getNoEnablePermissionTitle = (
  resourceType: string,
  t: (key: string) => string,
): string => {
  const titleMap: Record<string, string> = {
    pipeline: t('pipelineNotEnabled'),
    pipeline_group: t('pipelineGroupNotEnabled'),
  }
  return titleMap[resourceType] || t('permissionNotEnabled')
}

/**
 * Get close manage tips based on resource type
 * @param resourceType - Resource type
 * @param t - i18n translation function
 * @returns Tips text
 */
export const getCloseManageTips = (resourceType: string, t: (key: string) => string): string => {
  const tipsMap: Record<string, string> = {
    pipeline: t('将编辑者、执行者、查看者中的用户移除'),
    pipeline_group: t('将编辑者、执行者、查看者中的用户移除'),
    pipeline_template: t('将编辑者中的用户移除'),
    environment: t('将拥有者、使用者组中的用户移除'),
  }
  return tipsMap[resourceType] || ''
}

/**
 * Get status display text
 * @param status - Member status
 * @param t - i18n translation function
 * @returns Status display text
 */
export const getStatusText = (status: string, t: (key: string) => string): string => {
  const statusMap: Record<string, string> = {
    NOT_JOINED: t('未加入'),
    NORMAL: t('正常'),
    EXPIRED: t('已过期'),
  }
  return statusMap[status] || status
}

/**
 * Get status icon class
 * @param status - Member status
 * @returns CSS class name
 */
export const getStatusIconClass = (status: string): string => {
  const classMap: Record<string, string> = {
    NOT_JOINED: 'default',
    NORMAL: 'success',
    EXPIRED: 'failed',
  }
  return classMap[status] || 'default'
}

/**
 * Group count fields based on resource type
 * @param resourceType - Resource type
 * @returns Array of field names to display
 */
export const getGroupCountFields = (resourceType: string): string[] => {
  if (resourceType === RESOURCE_TYPES.CREATIVE_STREAM) {
    return ['userCount', 'templateCount', 'departmentCount']
  }
  return ['userCount', 'departmentCount']
}
