/**
 * ExecDetail Constants
 * 执行详情相关常量定义
 */

// Tab 类型
export const DETAIL_TAB = {
  LOG: 'log',
  SETTING: 'setting',
} as const

// Tab 类型的 TypeScript 类型
export type DetailTabType = (typeof DETAIL_TAB)[keyof typeof DETAIL_TAB]
