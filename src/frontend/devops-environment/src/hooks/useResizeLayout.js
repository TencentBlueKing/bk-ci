import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { RESIZE_LAYOUT_CONFIG_STORAGE_KEY, ENV_ACTIVE_NODE_TYPE } from '@/store/constants'
import UseInstance from '@/hooks/useInstance'

/**
 * ResizeLayout Hook
 * 用于管理可调整大小的布局组件的状态和配置
 *
 * @param {Object} options - 配置选项
 * @param {Number} options.defaultWidth - 默认宽度，默认为 240
 * @param {Number|Ref<Number>} options.containerWidth - 容器宽度，默认为 1920，支持响应式引用
 * @param {Boolean} options.autoInit - 是否自动初始化和监听路由变化，默认为 true
 * @returns {Object} 返回布局相关的状态和方法
 */
export function useResizeLayout (options = {}) {
    const { defaultWidth = 240, containerWidth = 1920, autoInit = true } = options
    const { proxy } = UseInstance()
    
    const currentAsideWidth = ref(defaultWidth)
    const isCollapsed = ref(false)
    const resizeLayout = ref(null)
    
    // 将 containerWidth 转换为响应式引用
    const containerWidthRef = computed(() => {
        return typeof containerWidth === 'object' && 'value' in containerWidth
            ? containerWidth.value
            : containerWidth
    })
    
    // 获取当前页面的唯一key: resType_pageName
    const currentPageKey = computed(() => {
        const resType = proxy.$route.params.resType || 'pipeline'
        const pageName = proxy.$route.name || 'envList'
        return `${resType}_${pageName}`
    })
    
    // 从localStorage获取所有页面的配置
    const getStorageConfig = () => {
        try {
            const config = localStorage.getItem(RESIZE_LAYOUT_CONFIG_STORAGE_KEY)
            return config ? JSON.parse(config) : {}
        } catch (e) {
            console.warn('解析ResizeLayout配置失败', e)
            return {}
        }
    }
    
    // 保存配置到localStorage
    const saveStorageConfig = (key, data) => {
        try {
            const config = getStorageConfig()
            config[key] = data
            localStorage.setItem(RESIZE_LAYOUT_CONFIG_STORAGE_KEY, JSON.stringify(config))
        } catch (e) {
            console.warn('保存ResizeLayout配置失败', e)
        }
    }
    
    // 获取当前页面的配置
    const getCurrentPageConfig = () => {
        const config = getStorageConfig()
        return config[currentPageKey.value] || { width: defaultWidth, collapsed: false }
    }
    
    // 初始化宽度
    const initialDivide = computed(() => {
        const pageConfig = getCurrentPageConfig()
        return pageConfig.width || defaultWidth
    })
    
    // 计算主内容区域宽度
    const mainWidth = computed(() => {
        return isCollapsed.value ? containerWidthRef.value : containerWidthRef.value - currentAsideWidth.value
    })
    
    // 处理折叠状态变化
    const handleCollapseChange = (val) => {
        isCollapsed.value = val
        const pageConfig = getCurrentPageConfig()
        saveStorageConfig(currentPageKey.value, {
            ...pageConfig,
            collapsed: val
        })
    }
    
    // 处理拖拽后的宽度变化
    const afterResize = (val) => {
        currentAsideWidth.value = val
        const pageConfig = getCurrentPageConfig()
        saveStorageConfig(currentPageKey.value, {
            ...pageConfig,
            width: val
        })
    }
    
    // 初始化当前页面的状态
    const initPageState = () => {
        const pageConfig = getCurrentPageConfig()
        currentAsideWidth.value = pageConfig.width || defaultWidth
        
        // 恢复折叠状态
        if (pageConfig.collapsed && resizeLayout.value) {
            resizeLayout.value.setCollapse(true)
        }
    }
    
    // 设置折叠状态
    const setCollapse = (val) => {
        if (resizeLayout.value) {
            resizeLayout.value.setCollapse(val)
        }
    }
    
    // 如果启用自动初始化，则监听路由变化并在挂载时初始化
    if (autoInit) {
        // 监听路由变化，切换页面时恢复对应的状态
        watch(currentPageKey, () => {
            initPageState()
        })
        
        // 组件挂载时初始化
        onMounted(() => {
            initPageState()
        })
    }
    onUnmounted(() => {
        localStorage.removeItem(ENV_ACTIVE_NODE_TYPE)
    })
    return {
        // 状态
        currentAsideWidth,
        isCollapsed,
        resizeLayout,
        currentPageKey,
        initialDivide,
        mainWidth,
        
        // 方法
        handleCollapseChange,
        afterResize,
        initPageState,
        setCollapse,
        getStorageConfig,
        getCurrentPageConfig
    }
}
