import { ref, onMounted, onUnmounted, type Ref } from 'vue'

/**
 * 表格高度计算hook
 * 用于计算表格容器的最大高度，支持响应式调整
 * @param containerRef 表格容器引用
 * @returns 表格最大高度
 */
export function useTableHeight(containerRef: Ref<HTMLElement | undefined>) {
  const maxHeight = ref<number>()

  function updateTableHeight() {
    if (containerRef.value) {
      maxHeight.value = containerRef.value.offsetHeight
    }
  }

  onMounted(() => {
    updateTableHeight()
    window.addEventListener('resize', updateTableHeight)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', updateTableHeight)
  })

  return {
    maxHeight,
    updateTableHeight,
  }
}
