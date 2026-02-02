/**
 * 获取原子模型unique Key
 * @param atomCode 原子标识
 * @param atomVersion 原子版本
 */
export function getAtomModalKey(atomCode: string, atomVersion: string): string {
  return `${atomCode}-${atomVersion}`
}

/**
 * 判断两个数组是否有交集
 */
export function hasIntersection(arr1: any[], arr2: any[]): boolean {
  try {
    return arr2.some((item) => arr1.includes(item))
  } catch (e) {
    return false
  }
}

/**
 * 根据插件字段rely配置决定是否显示
 * @param fieldProps 插件字段配置
 * @param values 插件表单值
 */
export function rely(fieldProps: any, values: any): boolean {
  try {
    if (!fieldProps.rely) {
      return true
    }
    const {
      rely: { expression = [], operation = 'AND' },
    } = fieldProps

    const cb = (item: any) => {
      const { key, value, regex } = item
      const formValue = values[key]

      if (Array.isArray(value)) {
        if (Array.isArray(formValue)) {
          return hasIntersection(value, formValue)
        }
        return typeof formValue !== 'undefined' && value.includes(formValue)
      } else if (regex) {
        const reg = new RegExp(regex, 'i')
        return Array.isArray(formValue)
          ? formValue.some((item) => reg.test(item))
          : reg.test(formValue)
      } else {
        return Array.isArray(formValue)
          ? formValue.some((item) => item === value)
          : formValue === value
      }
    }

    switch (operation) {
      case 'AND':
        return expression.every(cb)
      case 'OR':
        return expression.length > 0 ? expression.some(cb) : true
      case 'NOT':
        return expression.length > 0 ? !expression.some(cb) : true
      default:
        return true
    }
  } catch (e) {
    console.error('Error in rely check:', e)
    return true
  }
}

/**
 * 根据原子模型获取原子默认值
 * @param atomProps 原子模型表单对象
 */
export function getAtomDefaultValue(atomProps: Record<string, any> = {}): Record<string, any> {
  return Object.keys(atomProps).reduce(
    (formProps, key) => {
      const prop = atomProps[key]
      if (prop && typeof prop === 'object' && 'default' in prop) {
        formProps[key] = prop.default
      }
      return formProps
    },
    {} as Record<string, any>,
  )
}

/**
 * 获取原子输出对象
 * @param output 原子输出
 */
export function getAtomOutputObj(output: Record<string, any> = {}): Record<string, any> {
  try {
    const outputObj: Record<string, any> = {}
    for (const key in output) {
      if (Object.prototype.hasOwnProperty.call(output, key)) {
        const outputItem = output[key]
        if (outputItem && typeof outputItem === 'object' && 'type' in outputItem) {
          outputObj[key] = outputItem.type
        } else {
          outputObj[key] = outputItem
        }
      }
    }
    return outputObj
  } catch (e) {
    console.warn('get output error', output, e)
    return {}
  }
}

/**
 * 判断是否是新模板版本
 * @param htmlTemplateVersion HTML模板版本
 */
export function isNewAtomTemplate(htmlTemplateVersion?: string): boolean {
  return htmlTemplateVersion !== '1.0' && htmlTemplateVersion !== undefined
}

/**
 * 对比插件版本差异
 * @param preAtomVal 上一个版本的插件值
 * @param preAtomProps 上一个版本的插件属性
 * @param atomProps 当前版本的插件属性
 * @param isChangeAtom 是否更换了插件
 */
export function diffAtomVersions(
  preAtomVal: Record<string, any> = {},
  preAtomProps: Record<string, any> = {},
  atomProps: Record<string, any> = {},
  isChangeAtom: boolean,
): {
  atomValue: Record<string, any>
  atomVersionChangedKeys: string[]
} {
  let atomValue: Record<string, any> = {}
  const atomVersionChangedKeys: string[] = []

  if (!isChangeAtom) {
    atomValue = Object.keys(atomProps).reduce(
      (formProps, key) => {
        const atomProp = atomProps[key] || {}
        const preAtomProp = preAtomProps[key] || {}

        // 检查组件类型、字段类型和 multiple 是否相同
        const isSameComponent = atomProp.component === preAtomProp.component
        const isSameType = atomProp.type === preAtomProp.type
        const isSameMultiple =
          (atomProp.optionsConf || {}).multiple === (preAtomProp.optionsConf || {}).multiple

        if (isSameComponent && isSameType && isSameMultiple) {
          // 如果属性相同，保留原值
          formProps[key] = preAtomVal[key]
        } else {
          // 如果属性不同，标记为变更
          atomVersionChangedKeys.push(key)
        }
        return formProps
      },
      {} as Record<string, any>,
    )
  }

  return { atomValue, atomVersionChangedKeys }
}
