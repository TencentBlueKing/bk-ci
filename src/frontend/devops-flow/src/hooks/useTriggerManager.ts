import {
  fetchAtomClassify,
  fetchAtoms,
  fetchAtomVersionList,
  JobCategory,
  type AtomClassify,
  type AtomItem,
  type AtomVersion,
} from '@/api/atom'
import {
  fetchTriggerModal,
  fetchTriggerTypes,
  type TriggerBaseItem,
  type TriggerModal,
  type TriggerType,
} from '@/api/trigger'
import { computed, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'

interface TriggerListCache {
  data: TriggerBaseItem[]
  hasMore: boolean
  page: number
  timestamp: number
  loading: boolean
}

interface TriggerCacheMap {
  [key: string]: TriggerListCache
}

interface TriggerModalCache {
  [key: string]: {
    data: TriggerModal | null
    loading: boolean
    timestamp: number
  }
}

interface TriggerVersionCache {
  [atomCode: string]: {
    data: AtomVersion[]
    loading: boolean
    timestamp: number
  }
}

/**
 * 触发器数据管理 Hook
 * 提供触发器分类和触发器列表的缓存管理
 */
export const useTriggerManager = () => {
  const route = useRoute()
  const projectCode = computed(() => (route?.params?.projectId as string) || '')

  // 全局状态
  const typeList = ref<TriggerType[]>([])
  const isLoadingTypes = ref(false)
  const triggerCacheMap = reactive<TriggerCacheMap>({})
  const modalCacheMap = reactive<TriggerModalCache>({})
  const versionCacheMap = reactive<TriggerVersionCache>({})

  // 触发器分类缓存（用于获取 classifyId）
  const triggerClassify = ref<AtomClassify | null>(null)
  const triggerClassifyPromise = ref<Promise<AtomClassify | null> | null>(null)

  // 缓存过期时间（10分钟）
  const CACHE_EXPIRE_TIME = 10 * 60 * 1000

  /**
   * 获取触发器分类（classifyCode === 'trigger'）
   */
  const fetchTriggerClassify = async (): Promise<AtomClassify | null> => {
    if (triggerClassify.value) {
      return triggerClassify.value
    }
    if (triggerClassifyPromise.value) {
      return triggerClassifyPromise.value
    }

    triggerClassifyPromise.value = (async () => {
      try {
        const list = await fetchAtomClassify({
          category: JobCategory.TRIGGER,
          serviceScope: 'PIPELINE'
        })
        const target = (list || []).find((item) => item.classifyCode === 'trigger') || null
        triggerClassify.value = target
        return target
      } catch (error) {
        console.error('Failed to fetch trigger classify:', error)
        return null
      } finally {
        triggerClassifyPromise.value = null
      }
    })()

    return triggerClassifyPromise.value
  }

  // 生成缓存键
  const generateCacheKey = (params: { ownerStoreCode?: string; keyword?: string }) => {
    const { ownerStoreCode = '', keyword = '' } = params
    return `trigger_${ownerStoreCode}_${keyword}`
  }

  // 生成 Modal 缓存键
  const generateModalCacheKey = (sourceCode: string, atomCode: string, version: string) => {
    return `${sourceCode}_${atomCode}_${version}`
  }

  // 检查缓存是否有效
  const isCacheValid = (cacheKey: string): boolean => {
    const cache = triggerCacheMap[cacheKey]
    if (!cache) return false
    return Date.now() - cache.timestamp < CACHE_EXPIRE_TIME
  }

  /**
   * 获取触发器分类列表
   */
  const fetchTypeList = async (forceRefresh = false): Promise<TriggerType[]> => {
    if (typeList.value.length > 0 && !forceRefresh) {
      return typeList.value
    }

    isLoadingTypes.value = true
    try {
      const result = await fetchTriggerTypes()
      typeList.value = result || []
      return typeList.value
    } catch (error) {
      console.error('Failed to fetch trigger types:', error)
      return []
    } finally {
      isLoadingTypes.value = false
    }
  }

  /**
   * 获取触发器列表
   */
  const fetchList = async (
    params: {
      ownerStoreCode?: string
      keyword?: string
      page?: number
      pageSize?: number
      forceRefresh?: boolean
    } = {},
  ): Promise<{
    records: TriggerBaseItem[]
    hasMore: boolean
    page: number
  }> => {
    const {
      ownerStoreCode = '',
      keyword = '',
      page = 1,
      pageSize = 20,
      forceRefresh = false,
    } = params

    const cacheKey = generateCacheKey({ ownerStoreCode, keyword })

    // 初始化缓存
    if (!triggerCacheMap[cacheKey]) {
      triggerCacheMap[cacheKey] = {
        data: [],
        hasMore: true,
        page: 0,
        timestamp: 0,
        loading: false,
      }
    }

    const cache = triggerCacheMap[cacheKey]

    // 如果是第一页且缓存有效且不强制刷新，直接返回缓存数据
    if (page === 1 && !forceRefresh && isCacheValid(cacheKey) && cache.data.length > 0) {
      return {
        records: cache.data,
        hasMore: cache.hasMore,
        page: cache.page,
      }
    }

    // 防止重复请求
    if (cache.loading) {
      return {
        records: cache.data,
        hasMore: cache.hasMore,
        page: cache.page,
      }
    }

    cache.loading = true

    try {
      // 1. 先获取 trigger 分类的 classifyId
      const classify = await fetchTriggerClassify()
      const classifyId = classify?.id || ''

      // 2. 调用插件接口获取触发器列表
      const result = await fetchAtoms({
        projectCode: projectCode.value,
        category: JobCategory.TRIGGER,
        classifyId,
        ownerStoreCode: ownerStoreCode || undefined,
        keyword: keyword || undefined,
        queryProjectAtomFlag: false,
        page,
        pageSize,
      })

      const records: TriggerBaseItem[] = (result.records || []).map((item: AtomItem) => ({
        atomCode: item.atomCode,
        name: item.name,
        summary: item.summary,
        logoUrl: item.logoUrl,
        version: item.version,
        defaultVersion: item.defaultVersion,
        publisher: item.publisher,
        score: item.score,
        recentExecuteNum: item.recentExecuteNum,
        hotFlag: item.hotFlag,
        recommendFlag: item.recommendFlag,
        installed: item.installed,
        defaultFlag: item.defaultFlag,
        ownerStoreCode: (item as any).ownerStoreCode || ownerStoreCode || '',
      }))
      const hasMore = records.length >= pageSize

      if (page === 1 || forceRefresh) {
        // 第一页或强制刷新，重置缓存
        cache.data = records
        cache.page = 1
      } else {
        // 后续页，追加数据
        cache.data = [...cache.data, ...records]
        cache.page = page
      }

      cache.hasMore = hasMore
      cache.timestamp = Date.now()

      return {
        records: cache.data,
        hasMore,
        page: cache.page,
      }
    } catch (error) {
      console.error('Failed to fetch trigger list:', error)
      return {
        records: cache.data,
        hasMore: false,
        page: cache.page,
      }
    } finally {
      cache.loading = false
    }
  }

  /**
   * 获取触发器详情配置
   */
  const fetchModal = async (
    ownerStoreCode: string,
    atomCode: string,
    version: string,
  ): Promise<TriggerModal | null> => {
    const cacheKey = generateModalCacheKey(ownerStoreCode, atomCode, version)

    // 初始化缓存
    if (!modalCacheMap[cacheKey]) {
      modalCacheMap[cacheKey] = {
        data: null,
        loading: false,
        timestamp: 0,
      }
    }

    const cache = modalCacheMap[cacheKey]

    // 检查缓存
    if (cache.data && Date.now() - cache.timestamp < CACHE_EXPIRE_TIME) {
      return cache.data
    }

    // 防止重复请求
    if (cache.loading) {
      return new Promise((resolve) => {
        const checkInterval = setInterval(() => {
          if (!cache.loading) {
            clearInterval(checkInterval)
            resolve(cache.data)
          }
        }, 50)
      })
    }

    cache.loading = true

    try {
      const result = await fetchTriggerModal(ownerStoreCode, projectCode.value, atomCode, version)
      cache.data = result
      cache.timestamp = Date.now()
      return result
    } catch (error) {
      console.error('Failed to fetch trigger modal:', error)
      return null
    } finally {
      cache.loading = false
    }
  }

  /**
   * 获取触发器版本列表（复用插件版本接口）
   */
  const fetchVersionList = async (
    projectCode: string,
    atomCode: string,
  ): Promise<AtomVersion[]> => {
    if (!atomCode || !projectCode) return []

    const cacheKey = `${projectCode}_${atomCode}`
    if (!versionCacheMap[cacheKey]) {
      versionCacheMap[cacheKey] = { data: [], loading: false, timestamp: 0 }
    }

    const cache = versionCacheMap[cacheKey]

    if (cache.data.length > 0 && Date.now() - cache.timestamp < CACHE_EXPIRE_TIME) {
      return cache.data
    }

    if (cache.loading) {
      return new Promise((resolve) => {
        const checkInterval = setInterval(() => {
          if (!cache.loading) {
            clearInterval(checkInterval)
            resolve(cache.data)
          }
        }, 50)
      })
    }

    cache.loading = true

    try {
      const result = await fetchAtomVersionList({ projectCode, atomCode })
      cache.data = result || []
      cache.timestamp = Date.now()
      return cache.data
    } catch (error) {
      console.error(`Failed to fetch version list for ${atomCode}:`, error)
      return []
    } finally {
      cache.loading = false
    }
  }

  /**
   * 检查是否正在加载列表
   */
  const isLoadingList = (
    params: {
      classifyCode?: string
      keyword?: string
    } = {},
  ) => {
    const cacheKey = generateCacheKey(params)
    return triggerCacheMap[cacheKey]?.loading || false
  }

  /**
   * 检查是否正在加载 Modal
   */
  const isLoadingModal = (sourceCode: string, atomCode: string, version: string) => {
    const cacheKey = generateModalCacheKey(sourceCode, atomCode, version)
    return modalCacheMap[cacheKey]?.loading || false
  }

  /**
   * 获取缓存的 Modal
   */
  const getCachedModal = (sourceCode: string, atomCode: string, version: string) => {
    const cacheKey = generateModalCacheKey(sourceCode, atomCode, version)
    return modalCacheMap[cacheKey]?.data || null
  }

  /**
   * 清除指定缓存
   */
  const clearCache = (params?: { classifyCode?: string; keyword?: string }) => {
    if (params) {
      const cacheKey = generateCacheKey(params)
      delete triggerCacheMap[cacheKey]
    } else {
      // 清除所有缓存
      Object.keys(triggerCacheMap).forEach((key) => {
        delete triggerCacheMap[key]
      })
    }
  }

  /**
   * 刷新数据
   */
  const refreshData = async (
    params: {
      classifyCode?: string
      keyword?: string
    } = {},
  ) => {
    return await fetchList({ ...params, forceRefresh: true })
  }

  return {
    // 状态
    typeList: computed(() => typeList.value),
    isLoadingTypes: computed(() => isLoadingTypes.value),

    // 方法
    fetchTypeList,
    fetchTriggerClassify,
    fetchList,
    fetchModal,
    fetchVersionList,
    isLoadingList,
    isLoadingModal,
    getCachedModal,
    clearCache,
    refreshData,

    // 计算属性
    typeMap: computed(() => {
      const map: Record<string, TriggerType> = {}
      typeList.value.forEach((item) => {
        map[item.ownerStoreCode] = item
      })
      return map
    }),
  }
}
