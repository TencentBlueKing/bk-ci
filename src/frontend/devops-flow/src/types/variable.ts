/**
 * Variable type definitions for devops-flow
 */

import type { Param } from '@/types/flow'

/**
 * Variable types
 */
export enum ParamType {
  STRING = 'STRING',
  TEXTAREA = 'TEXTAREA',
  BOOLEAN = 'BOOLEAN',
  ENUM = 'ENUM',
  MULTIPLE = 'MULTIPLE',
}

/**
 * Variable category
 */
export enum VariableCategory {
  INPUT = 'input', // 入参
  CONSTANT = 'constant', // 常量
  OTHER = 'other', // 其他变量
}

/**
 * Variable panel tab types
 */
export enum VariablePanelTab {
  VARIABLES = 'variables', // 变量
  PLUGIN_OUTPUT = 'plugin_output', // 插件输出变量
  SYSTEM = 'system', // 系统变量
}

/**
 * Param option for ENUM/MULTIPLE types
 */
export interface ParamOption {
  key: string
  value: string
}

/**
 * Plugin output variable
 */
export interface PluginOutputVariable {
  id: string // Variable ID
  name: string // Variable name
  desc?: string // Description
  stepId?: string // Step ID that outputs this variable
}

/**
 * System variable
 */
export interface SystemVariable {
  id: string // Variable ID
  name: string // Variable name
  desc?: string // Description
  remark?: string // Remark
  value?: string // Current value (if available)
}

/**
 * Default variable values by type
 */
export const DEFAULT_VARIABLE_VALUES: Record<ParamType, Param> = {
  [ParamType.STRING]: {
    id: '',
    name: '',
    type: ParamType.STRING,
    category: VariableCategory.OTHER,
    constant: false,
    defaultValue: '',
    required: false,
    readOnly: false,
  },
  [ParamType.TEXTAREA]: {
    id: '',
    name: '',
    type: ParamType.TEXTAREA,
    category: VariableCategory.OTHER,
    constant: false,
    defaultValue: '',
    required: false,
    readOnly: false,
  },
  [ParamType.BOOLEAN]: {
    id: '',
    name: '',
    type: ParamType.BOOLEAN,
    category: VariableCategory.OTHER,
    constant: false,
    defaultValue: false,
    required: false,
    readOnly: false,
  },
  [ParamType.ENUM]: {
    id: '',
    name: '',
    type: ParamType.ENUM,
    category: VariableCategory.OTHER,
    constant: false,
    defaultValue: '',
    required: false,
    readOnly: false,
    options: [],
  },
  [ParamType.MULTIPLE]: {
    id: '',
    name: '',
    type: ParamType.MULTIPLE,
    category: VariableCategory.OTHER,
    constant: false,
    defaultValue: [],
    required: false,
    readOnly: false,
    options: [],
  },
}

/**
 * Variable type list for selector
 * nameKey is used for i18n translation
 */
export const VARIABLE_TYPE_LIST = [
  { id: ParamType.STRING, nameKey: 'flow.variable.types.string' },
  { id: ParamType.TEXTAREA, nameKey: 'flow.variable.types.textarea' },
  { id: ParamType.BOOLEAN, nameKey: 'flow.variable.types.boolean' },
  { id: ParamType.ENUM, nameKey: 'flow.variable.types.enum' },
  { id: ParamType.MULTIPLE, nameKey: 'flow.variable.types.multiple' },
]

/**
 * Options source type for ENUM/MULTIPLE
 */
export enum OptionsSourceType {
  LIST = 'options', // 静态选项列表
  API = 'remote', // 从接口获取
}

/**
 * Options API configuration
 */
export interface OptionsApiConfig {
  type: OptionsSourceType
  url?: string // API 地址
  dataPath?: string // 数据路径，如 data.list
  paramId?: string // 选项 ID 字段名，默认 id
  paramName?: string // 选项名称字段名，默认 name
  [key: string]: unknown
}
/**
 * Validate variable ID
 * - Must start with letter or underscore
 * - Can contain letters, numbers, underscores
 * - For constants: must be all uppercase
 */
export function validateVariableId(id: string, isConstant: boolean): boolean {
  if (!id) return false

  // Basic pattern: start with letter or underscore, followed by letters, numbers, underscores
  const basicPattern = /^[a-zA-Z_][a-zA-Z0-9_]*$/
  if (!basicPattern.test(id)) return false

  // For constants: must be all uppercase
  if (isConstant) {
    return /^[A-Z_][A-Z0-9_]*$/.test(id)
  }

  return true
}
/**
 * System variable group
 */
export interface ReadOnlyVariableGroup {
  hasStepId?: boolean // Whether any plugin output variable is missing stepId
  name: string // Group name
  params: SystemVariable[] // Variables in this group
}
