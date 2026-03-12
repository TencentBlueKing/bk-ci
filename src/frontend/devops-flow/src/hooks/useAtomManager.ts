import {
  fetchAtomClassify,
  fetchAtoms,
  JobCategory,
  JobType,
  type AtomClassify,
  type AtomItem,
} from '@/api/atom'
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

export const RD_STORE_CODE = 'rdStore'

interface AtomListCache {
  data: AtomItem[]
  hasMore: boolean
  page: number
  timestamp: number
  loading: boolean
}

interface AtomCacheMap {
  [key: string]: AtomListCache
}

interface UseAtomManagerOptions {
  category?: JobCategory
}

/**
 * 统一的插件数据管理 Hook
 * 提供插件分类和插件列表的缓存管理
 */
export const useAtomManager = (options: UseAtomManagerOptions) => {
  const { t } = useI18n()
  const route = useRoute()
  const { category = JobCategory.TASK } = options
  const projectCode = computed(() => {
    return route.params.projectId as string
  })

  // 全局状态
  const classifyList = ref<AtomClassify[]>([])
  const isLoadingClassify = ref(false)
  const atomCacheMap = reactive<AtomCacheMap>({})

  // 缓存过期时间（10分钟）
  const CACHE_EXPIRE_TIME = 10 * 60 * 1000

  // 生成缓存键
  const generateCacheKey = (params: {
    classifyId?: string
    keyword?: string
    jobType?: JobType
    queryProjectAtomFlag?: boolean
  }) => {
    const { classifyId = '', keyword = '', jobType, queryProjectAtomFlag = true } = params
    return `${category}_${classifyId}_${keyword}_${jobType}_${queryProjectAtomFlag}`
  }

  // 检查缓存是否有效
  const isCacheValid = (cacheKey: string): boolean => {
    const cache = atomCacheMap[cacheKey]
    if (!cache) return false
    return Date.now() - cache.timestamp < CACHE_EXPIRE_TIME
  }

  // 获取插件分类列表
  const fetchClassifyList = async (forceRefresh = false): Promise<AtomClassify[]> => {
    if (classifyList.value.length > 0 && !forceRefresh) {
      return classifyList.value
    }

    isLoadingClassify.value = true
    try {
      const result = await fetchAtomClassify({ category })
      classifyList.value = result
      return result
    } catch (error) {
      console.error('Failed to fetch atom classify:', error)
      return []
    } finally {
      isLoadingClassify.value = false
    }
  }

  // 获取插件列表
  const fetchAtomList = async (params: {
    classifyId?: string
    jobType?: JobType
    keyword?: string
    queryProjectAtomFlag?: boolean
    page?: number
    pageSize?: number
    forceRefresh?: boolean
  }): Promise<{
    records: AtomItem[]
    hasMore: boolean
    page: number
  }> => {
    const {
      classifyId = '',
      keyword = '',
      jobType,
      queryProjectAtomFlag = true,
      page = 1,
      pageSize = 20,
      forceRefresh = false,
    } = params

    const cacheKey = generateCacheKey({ classifyId, keyword, jobType, queryProjectAtomFlag })

    // 初始化缓存
    if (!atomCacheMap[cacheKey]) {
      atomCacheMap[cacheKey] = {
        data: [],
        hasMore: true,
        page: 0,
        timestamp: 0,
        loading: false,
      }
    }

    const cache = atomCacheMap[cacheKey]

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
      const result = await fetchAtoms({
        projectCode: projectCode.value,
        category,
        jobType,
        classifyId,
        os: 'WINDOWS',
        keyword,
        queryProjectAtomFlag,
        page,
        pageSize,
      })

      const records = result.records || []
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
      console.error('Failed to fetch atoms:', error)
      return {
        records: cache.data,
        hasMore: false,
        page: cache.page,
      }
    } finally {
      cache.loading = false
    }
  }

  // 获取缓存的插件列表
  const getCachedAtomList = (
    params: {
      classifyId?: string
      keyword?: string
      jobType?: JobType
      queryProjectAtomFlag?: boolean
    } = {},
  ): AtomItem[] => {
    const cacheKey = generateCacheKey(params)
    return atomCacheMap[cacheKey]?.data || []
  }

  // 检查是否正在加载
  const isLoadingAtoms = (
    params: {
      classifyId?: string
      keyword?: string
      jobType?: JobType
      queryProjectAtomFlag?: boolean
    } = {},
  ) => {
    const cacheKey = generateCacheKey(params)
    return atomCacheMap[cacheKey]?.loading || false
  }

  // 清除指定缓存
  const clearCache = (params?: { classifyId?: string; keyword?: string; jobType?: JobType; queryProjectAtomFlag?: boolean }) => {
    if (params) {
      const cacheKey = generateCacheKey(params)
      delete atomCacheMap[cacheKey]
    } else {
      // 清除所有缓存
      Object.keys(atomCacheMap).forEach((key) => {
        delete atomCacheMap[key]
      })
    }
  }

  // 清除所有缓存
  const clearAllCache = () => {
    Object.keys(atomCacheMap).forEach((key) => {
      delete atomCacheMap[key]
    })
    classifyList.value = []
  }

  // 刷新数据
  const refreshData = async (
    params: {
      classifyId?: string
      keyword?: string
      jobType?: JobType
      queryProjectAtomFlag?: boolean
    } = {},
  ) => {
    return await fetchAtomList({ ...params, forceRefresh: true })
  }

  // 预加载分类数据
  const preloadClassifyList = async () => {
    if (classifyList.value.length === 0) {
      await fetchClassifyList()
    }
  }

  return {
    // 状态
    classifyList: computed(() => classifyList.value),
    isLoadingClassify: computed(() => isLoadingClassify.value),

    // 方法
    fetchClassifyList,
    fetchAtomList,
    getCachedAtomList,
    isLoadingAtoms,
    clearCache,
    clearAllCache,
    refreshData,
    preloadClassifyList,

    // 计算属性
    classifyMap: computed(() => {
      const map: Record<string, AtomClassify> = {}
      classifyList.value.forEach((item) => {
        map[item.classifyCode] = item
      })
      return map
    }),

    // 获取分类选项（包含"全部"和"研发商店"选项）
    classifyOptions: computed(() => {
      const options = [...classifyList.value]
      if (category !== JobCategory.TRIGGER) {
        options.unshift({
          id: '',
          classifyCode: 'all',
          classifyName: t('flow.orchestration.allPlugins'),
          weight: 0,
        } as AtomClassify)
        options.push({
          id: '',
          classifyCode: RD_STORE_CODE,
          classifyName: t('flow.orchestration.rdStore'),
          weight: 0,
        } as AtomClassify)
      }
      return options
    }),
  }
}
