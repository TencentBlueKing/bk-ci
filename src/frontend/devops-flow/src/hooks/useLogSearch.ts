/**
 * useLogSearch Hook
 * 日志搜索功能封装
 * 提供搜索关键词、匹配结果、导航等功能
 */
import type { LogItem } from '@/api/log'
import { computed, ref, watch, type Ref } from 'vue'

export interface UseLogSearchOptions {
  logs: Ref<LogItem[]>
  containerSelector?: string // 日志容器的 CSS 选择器，用于滚动定位
}

export function useLogSearch(options: UseLogSearchOptions) {
  const { logs, containerSelector } = options

  // State
  const searchKeyword = ref('')
  const searchMatches = ref<number[]>([])
  const currentMatchIndex = ref(-1)

  // 搜索结果计数文本
  const searchResultText = computed(() => {
    if (!searchKeyword.value || searchMatches.value.length === 0) return '0 / 0'
    return `${currentMatchIndex.value + 1}/${searchMatches.value.length}`
  })

  // 搜索日志
  const searchLogs = () => {
    if (!searchKeyword.value.trim()) {
      searchMatches.value = []
      currentMatchIndex.value = -1
      return
    }

    const keyword = searchKeyword.value.toLowerCase()
    const matches: number[] = []

    logs.value.forEach((log, index) => {
      if (log.message.toLowerCase().includes(keyword)) {
        matches.push(index)
      }
    })

    searchMatches.value = matches
    currentMatchIndex.value = matches.length > 0 ? 0 : -1

    // 如果有匹配项，滚动到第一个匹配项
    if (matches.length > 0) {
      scrollToMatch(0)
    }
  }

  // 滚动到指定匹配项
  const scrollToMatch = (index: number) => {
    if (searchMatches.value.length === 0) return

    // 如果没有提供容器选择器，尝试查找默认的日志容器
    const container = containerSelector
      ? document.querySelector(containerSelector)
      : document.querySelector('.logContent, .logContentArea, .logLines')

    if (!container) return

    const matchIndex = searchMatches.value[index]
    const lineElement = container.querySelector(`[data-line-index="${matchIndex}"]`)

    if (lineElement) {
      lineElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }

  // 上一个匹配
  const prevMatch = () => {
    if (searchMatches.value.length === 0) return
    currentMatchIndex.value =
      (currentMatchIndex.value - 1 + searchMatches.value.length) % searchMatches.value.length
    scrollToMatch(currentMatchIndex.value)
  }

  // 下一个匹配
  const nextMatch = () => {
    if (searchMatches.value.length === 0) return
    currentMatchIndex.value = (currentMatchIndex.value + 1) % searchMatches.value.length
    scrollToMatch(currentMatchIndex.value)
  }

  // 清除搜索
  const clearSearch = () => {
    searchKeyword.value = ''
    searchMatches.value = []
    currentMatchIndex.value = -1
  }

  // 高亮搜索关键词
  const highlightKeyword = (text: string): string => {
    if (!searchKeyword.value.trim()) return text

    const keyword = searchKeyword.value
    const regex = new RegExp(`(${keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')

    return text.replace(regex, `<mark class="search-highlight">$1</mark>`)
  }

  // 检查指定索引的日志是否匹配
  const isMatch = (index: number): boolean => {
    return searchMatches.value.includes(index)
  }

  // 检查指定索引的日志是否是当前激活的匹配项
  const isActiveMatch = (index: number): boolean => {
    return (
      searchMatches.value.length > 0 &&
      searchMatches.value[currentMatchIndex.value] === index
    )
  }

  // 监听搜索关键词变化
  watch(searchKeyword, () => {
    searchLogs()
  })

  // 监听日志变化，重新搜索
  watch(
    () => logs.value.length,
    () => {
      if (searchKeyword.value) {
        searchLogs()
      }
    },
  )

  return {
    // State
    searchKeyword,
    searchMatches,
    currentMatchIndex,
    searchResultText,

    // Methods
    searchLogs,
    scrollToMatch,
    prevMatch,
    nextMatch,
    clearSearch,
    highlightKeyword,
    isMatch,
    isActiveMatch,
  }
}

