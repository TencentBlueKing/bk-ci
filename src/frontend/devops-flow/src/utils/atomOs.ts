import { JobCategory, type AtomItem } from '@/api/atom'

/**
 * 判断插件在当前构建环境下是否禁用（OS 不适配）
 * @param os 当前构建环境 OS（undefined 表示云任务/无构建环境）
 * @param atom 插件信息
 */
export function isAtomDisabled(os: string | undefined, atom: AtomItem): boolean {
  if (atom.category === JobCategory.TRIGGER) return false
  const osList = atom.os ?? []
  return (
    (!os && osList.length > 0) ||
    (!!os && osList.length > 0 && !osList.includes(os)) ||
    (!!os && osList.length === 0 && !atom.buildLessRunFlag)
  )
}

/**
 * 批量标记插件禁用状态
 * @param list 插件列表
 * @param os 当前构建环境 OS
 */
export function markAtomDisabled(list: AtomItem[], os?: string): AtomItem[] {
  return list.map((atom) => ({ ...atom, disabled: isAtomDisabled(os, atom) }))
}
