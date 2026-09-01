import { computed, getCurrentInstance } from 'vue'

/**
 * Vue 2.7 和 Vue 3 兼容的事件监听器处理Hook
 * Vue 2.7: $listeners 在模板中直接可用
 * Vue 3: 所有监听器都在 $attrs 中
 *
 * @returns {import('vue').ComputedRef<Object>} 事件监听器对象
 */
export function useListeners() {
    const instance = getCurrentInstance()

    return computed(() => {
        // Vue 2.7: 使用 $listeners
        if (instance?.proxy?.$listeners) {
            return instance.proxy.$listeners
        }

        // Vue 3: 从 $attrs 中提取事件监听器
        const attrs = instance?.attrs || {}
        const eventListeners = {}

        Object.keys(attrs).forEach(key => {
            if (key.startsWith('on') && typeof attrs[key] === 'function') {
                const eventName = key.slice(2).toLowerCase()
                eventListeners[eventName] = attrs[key]
            }
        })

        return eventListeners
    })
}
