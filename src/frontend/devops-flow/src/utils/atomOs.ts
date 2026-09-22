/*
 * Tencent is pleased to support the open source community by making
 * 蓝鲸智云PaaS平台 (BlueKing PaaS) available.
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 * 蓝鲸智云PaaS平台 (BlueKing PaaS) is licensed under the MIT License.
 * License for 蓝鲸智云PaaS平台 (BlueKing PaaS):
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
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
