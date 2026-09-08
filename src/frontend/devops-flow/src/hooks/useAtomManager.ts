import {
  fetchAtomClassify,
  fetchAtoms,
  fetchSearchAtoms,
  JobCategory,
  JobType,
  type AtomClassify,
  type AtomItem,
} from '@/api/atom'
import { markAtomDisabled } from '@/utils/atomOs'
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

export const RD_STORE_CODE = 'rdStore'

interface AtomListCache {
  data: AtomItem[]
  hasMore: boolean
  page: number // 云任务单阶段分页
  timestamp: number
  loading: boolean
  // 构建任务双阶段状态
  recommendPage: number
  unCommendPage: number
  commendAtomCount: number
  isCommendAtomPageOver: boolean
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
    os?: string
    jobType?: JobType
    queryProjectAtomFlag?: boolean
  }) => {
    const { classifyId = '', keyword = '', os = '', jobType, queryProjectAtomFlag = true } = params
    return `${category}_${classifyId}_${keyword}_${os}_${jobType}_${queryProjectAtomFlag}`
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

  // 获取插件列表（内部管理分页，构建任务采用「适配优先 + 不适配追加」双阶段分页）
  const fetchAtomList = async (params: {
    classifyId?: string
    os?: string
    jobType?: JobType
    keyword?: string
    queryProjectAtomFlag?: boolean
    reset?: boolean
    forceRefresh?: boolean
  }): Promise<{
    records: AtomItem[]
    hasMore: boolean
  }> => {
    const {
      classifyId = '',
      keyword = '',
      jobType,
      queryProjectAtomFlag = true,
      reset = false,
      forceRefresh = false,
      os,
    } = params

    const cacheKey = generateCacheKey({ classifyId, keyword, os, jobType, queryProjectAtomFlag })

    // 初始化缓存
    if (!atomCacheMap[cacheKey]) {
      atomCacheMap[cacheKey] = {
        data: [],
        hasMore: true,
        page: 1,
        timestamp: 0,
        loading: false,
        recommendPage: 1,
        unCommendPage: 1,
        commendAtomCount: 0,
        isCommendAtomPageOver: false,
      }
    }

    const cache = atomCacheMap[cacheKey]

    // 缓存命中（仅 reset 且未强制刷新且缓存有效且有数据）
    if (reset && !forceRefresh && isCacheValid(cacheKey) && cache.data.length > 0) {
      return {
        records: cache.data,
        hasMore: cache.hasMore,
      }
    }

    // reset 或强制刷新：重置缓存与双阶段状态
    if (reset || forceRefresh) {
      cache.data = []
      cache.hasMore = true
      cache.page = 1
      cache.recommendPage = 1
      cache.unCommendPage = 1
      cache.commendAtomCount = 0
      cache.isCommendAtomPageOver = false
    }

    // 防止重复请求
    if (cache.loading) {
      return {
        records: [],
        hasMore: cache.hasMore,
      }
    }

    cache.loading = true

    try {
      const pageSize = 8
      let records: AtomItem[] = []
      let hasMore = false

      if (os) {
        // 构建任务：双阶段分页（适配插件优先，拉完后追加不适配插件）
        if (!cache.isCommendAtomPageOver) {
          // 阶段一：适配当前 OS 的插件
          const result = await fetchAtoms({
            projectCode: projectCode.value,
            category,
            jobType,
            classifyId,
            os,
            keyword,
            queryProjectAtomFlag,
            page: cache.recommendPage,
            pageSize,
          })
          records = markAtomDisabled(result.records || [], os)
          cache.data = [...cache.data, ...records]
          cache.recommendPage += 1

          const count = result.count
          if (cache.data.length >= count) {
            cache.isCommendAtomPageOver = true
            cache.commendAtomCount = count
            cache.unCommendPage = 1

            // 适配插件拉完后，立即请求一页不适配插件（对齐流水线）
            const unCommendResult = await fetchAtoms({
              projectCode: projectCode.value,
              category,
              jobType: undefined,
              classifyId,
              os,
              keyword,
              queryProjectAtomFlag,
              fitOsFlag: false,
              queryFitAgentBuildLessAtomFlag: false,
              page: cache.unCommendPage,
              pageSize,
            })
            const unCommendRecords = markAtomDisabled(unCommendResult.records || [], os)
            cache.data = [...cache.data, ...unCommendRecords]
            cache.unCommendPage += 1
            records = [...records, ...unCommendRecords]
            hasMore = cache.data.length < cache.commendAtomCount + unCommendResult.count
          } else {
            hasMore = true
          }
        } else {
          // 阶段二：不适配当前 OS 的插件
          const result = await fetchAtoms({
            projectCode: projectCode.value,
            category,
            jobType: undefined,
            classifyId,
            os,
            keyword,
            queryProjectAtomFlag,
            fitOsFlag: false,
            queryFitAgentBuildLessAtomFlag: false,
            page: cache.unCommendPage,
            pageSize,
          })
          records = markAtomDisabled(result.records || [], os)
          cache.data = [...cache.data, ...records]
          cache.unCommendPage += 1

          hasMore = cache.data.length < cache.commendAtomCount + result.count
        }
      } else {
        // 云任务（无构建环境）：单阶段分页
        const result = await fetchAtoms({
          projectCode: projectCode.value,
          category,
          jobType,
          classifyId,
          os,
          keyword,
          queryProjectAtomFlag,
          page: cache.page,
          pageSize,
        })
        records = markAtomDisabled(result.records || [], os)
        cache.data = [...cache.data, ...records]
        cache.page += 1

        hasMore = records.length >= pageSize
      }

      cache.hasMore = hasMore
      cache.timestamp = Date.now()

      return {
        records,
        hasMore,
      }
    } catch (error) {
      console.error('Failed to fetch atoms:', error)
      return {
        records: [],
        hasMore: false,
      }
    } finally {
      cache.loading = false
    }
  }

  // 搜索模式：分页加载已安装/未安装插件列表（不需要缓存）
  const fetchSearchAtomList = async (params: {
    searchKey: string
    installed?: boolean
    os?: string
    page?: number
    pageSize?: number
  }): Promise<{
    records: AtomItem[]
    hasMore: boolean
    page: number
    totalPages: number
    count: number
  }> => {
    const { searchKey, installed = true, os, page = 1, pageSize = 100 } = params
    
    try {
      const result = await fetchSearchAtoms({
        projectCode: projectCode.value,
        category: JobCategory.TASK,
        searchKey,
        installed,
        os,
        page,
        pageSize,
      })
      
      const records = markAtomDisabled(result.records || [], os)
      const hasMore = page < result.totalPages
      
      return {
        records,
        hasMore,
        page,
        totalPages: result.totalPages,
        count: result.count,
      }
    } catch (error) {
      console.error('Failed to fetch search atoms:', error)
      return {
        records: [],
        hasMore: false,
        page,
        totalPages: 0,
        count: 0,
      }
    }
  }

  // 获取缓存的插件列表
  const getCachedAtomList = (
    params: {
      classifyId?: string
      keyword?: string
      os?: string
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
      os?: string
      jobType?: JobType
      queryProjectAtomFlag?: boolean
    } = {},
  ) => {
    const cacheKey = generateCacheKey(params)
    return atomCacheMap[cacheKey]?.loading || false
  }

  // 清除指定缓存
  const clearCache = (params?: {
    classifyId?: string
    keyword?: string
    os?: string
    jobType?: JobType
    queryProjectAtomFlag?: boolean
  }) => {
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
      os?: string
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
    fetchSearchAtomList,
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
