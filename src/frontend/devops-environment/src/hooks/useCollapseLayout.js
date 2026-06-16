import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { getOffset } from '@/utils/util'

const STORAGE_KEY_PREFIX = 'collapse_layout_'

// 全局状态存储，按 storageKey 缓存
const globalStates = new Map()

/**
 * 折叠布局 Hook
 * @param {string} storageKey - 本地存储的唯一标识，用于记忆折叠状态
 * @param {boolean} defaultFlod - 默认是否折叠，默认为 false
 */
export default function useCollapseLayout (storageKey, defaultFlod = false) {
    const layoutRef = ref(null)
    const layoutWidth = ref('auto')
    const layoutOffsetTop = ref(0)
    
    // 从本地存储获取折叠状态
    const getStoredFlod = () => {
        if (!storageKey) return defaultFlod
        try {
            const stored = localStorage.getItem(`${STORAGE_KEY_PREFIX}${storageKey}`)
            return stored !== null ? JSON.parse(stored) : defaultFlod
        } catch (e) {
            console.error('Failed to get stored flod state:', e)
            return defaultFlod
        }
    }
    
    // 保存折叠状态到本地存储
    const saveFlodState = (value) => {
        if (!storageKey) return
        try {
            localStorage.setItem(`${STORAGE_KEY_PREFIX}${storageKey}`, JSON.stringify(value))
        } catch (e) {
            console.error('Failed to save flod state:', e)
        }
    }
    
    // 获取或创建全局状态
    if (!globalStates.has(storageKey)) {
        const flod = ref(getStoredFlod())
        
        // 监听折叠状态变化，保存到本地存储
        watch(flod, (newFlod) => {
            saveFlodState(newFlod)
        }, {
            immediate: true
        })
        
        globalStates.set(storageKey, {
            flod
        })
    }
    
    // 从全局状态获取
    const { flod } = globalStates.get(storageKey)
    
    // 监听折叠状态变化，重新计算布局宽度
    watch(flod, () => {
        // 使用 nextTick 确保 DOM 更新后再计算
        setTimeout(() => {
            initLayout()
        }, 0)
    }, {
        immediate: true
    })

    // 左侧样式
    const leftStyles = computed(() => {
        if (flod.value) {
            return {
                width: '260px'
            }
        }
        return {
            width: layoutWidth.value
        }
    })
    
    // 右侧样式
    const rightStyles = computed(() => {
        const paddingBottom = 18
        return {
            height: `calc(100vh - ${layoutOffsetTop.value + paddingBottom}px)`
        }
    })
    
    // 切换折叠状态
    const toggleFlod = () => {
        flod.value = !flod.value
    }
    
    // 设置折叠状态
    const setFlod = (value) => {
        flod.value = value
    }
    
    // 展开
    const expand = () => {
        flod.value = true
    }
    
    // 收起
    const collapse = () => {
        flod.value = false
    }
    
    // 初始化布局尺寸
    const initLayout = () => {
        if (!layoutRef.value) return
        
        const width = layoutRef.value.getBoundingClientRect().width
        layoutWidth.value = `${width}px`
        const offsetTop = getOffset(layoutRef.value).top
        layoutOffsetTop.value = offsetTop
    }
    
    // 监听 ResizeLayout 变化事件
    const handleResizeLayoutChange = () => {
        setTimeout(() => {
            initLayout()
        }, 0)
    }
    
    onMounted(() => {
        initLayout()
        window.addEventListener('resize', initLayout)
        // 监听 ResizeLayout 的折叠/展开事件
        window.addEventListener('resize-layout-change', handleResizeLayoutChange)
    })
    
    onBeforeUnmount(() => {
        window.removeEventListener('resize', initLayout)
        window.removeEventListener('resize-layout-change', handleResizeLayoutChange)
    })
    
    return {
        // refs
        layoutRef,
        
        // 状态
        flod,
        
        // 样式
        leftStyles,
        rightStyles,
        
        // 方法
        toggleFlod,
        setFlod,
        expand,
        collapse,
        initLayout
    }
}
