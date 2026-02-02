import { useFlowModelStore } from '@/stores/flowModel'
import { get } from '@/utils/http'
import { Message } from 'bkui-vue'
import { storeToRefs } from 'pinia'
import { computed, onMounted, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

// URL parameter regex: {paramName} or {paramName?}
const PLUGIN_URL_PARAM_REG = /\{(.*?)(\?){0,1}\}/g

export interface ListItem {
  value: string | number
  label: string
  disabled?: boolean
  [key: string]: unknown
}

export interface SelectDataConf {
  url?: string
  dataPath?: string
  options?: Array<{ id: string | number; name: string; disabled?: boolean }>
  paramId?: string
  paramName?: string
  displayKey?: string
  settingKey?: string
  initRequest?: boolean
  multiple?: boolean
  searchable?: boolean
  clearable?: boolean
  [key: string]: unknown
}

/**
 * Get value from nested object by path
 * Supports multiple path formats:
 * - 'key' - simple key access
 * - 'key.subKey' - nested key access
 * - 'key[0]' - array index access
 * - 'key.subKey[0].prop' - combined access
 * 
 * @param obj - Source object to extract value from
 * @param path - Dot-separated path string
 * @param defaultVal - Default value if path not found
 * @returns The value at the path or defaultVal
 */
export const getValueByPath = <T = unknown>(
  obj: unknown,
  path: string,
  defaultVal?: T,
): T | undefined => {
  if (!obj || !path) return defaultVal

  const parts = path.split('.')
  let result: unknown = obj

  for (const part of parts) {
    if (result === null || result === undefined || typeof result !== 'object') {
      return defaultVal
    }

    // Handle array index: key[0] or just [0]
    const arrayMatch = part.match(/^(\w*)\[(\d+)\]$/)
    if (arrayMatch) {
      const [, key, index] = arrayMatch
      // If key exists, access it first; otherwise use result directly
      const target = key ? (result as Record<string, unknown>)[key] : result
      if (!Array.isArray(target)) return defaultVal
      result = target[parseInt(index!, 10)]
    } else {
      result = (result as Record<string, unknown>)[part]
    }
  }

  return (result as T) ?? defaultVal
}

/**
 * Extract response data by path
 * Default path: 'records'
 * 
 * @param response - API response object
 * @param dataPath - Path to extract data from (e.g., 'data.records', 'records')
 * @returns Array of data or empty array
 */
export const getResponseData = (
  response: unknown,
  dataPath?: string,
): unknown[] => {
  // Direct array response
  if (Array.isArray(response)) return response

  // Use getValueByPath with default path 'records'
  const result = getValueByPath<unknown[]>(response, dataPath || 'records', [])
  
  return Array.isArray(result) ? result : []
}

/**
 * Parse URL with context variables
 * Replaces {varName} patterns with actual values from multiple sources
 * Source priority: atomValue > container > route.params > route.query > flowModel > flowSetting
 */
export function useUrlParser() {
  const route = useRoute()
  const flowModelStore = useFlowModelStore()
  const { flowModel, flowSetting } = storeToRefs(flowModelStore)

  /**
   * Resolve URL parameter value from multiple sources
   * Priority: atomValue > container > route.params > route.query > flowModel > flowSetting
   */
  const resolveUrlParam = (
    paramName: string,
    context: {
      atomValue?: Record<string, unknown>
      container?: Record<string, unknown>
    } = {},
  ): string => {
    // 1. atomValue (current component props / element)
    if (context.atomValue?.[paramName] !== undefined) {
      return String(context.atomValue[paramName])
    }

    // 2. container (e.g., dispatchType.buildType)
    if (context.container) {
      // Special handling for bkPoolType from container
      if (paramName === 'bkPoolType') {
        const buildType = getValueByPath(context.container, 'dispatchType.buildType')
        if (buildType !== undefined) {
          return String(buildType)
        }
      }
      if (context.container[paramName] !== undefined) {
        return String(context.container[paramName])
      }
    }

    // 3. route params
    if (route.params[paramName] !== undefined) {
      return String(route.params[paramName])
    }

    // 4. route query
    if (route.query[paramName] !== undefined) {
      return String(route.query[paramName])
    }

    // 5. flowModel root level
    const flowModelVal = getValueByPath(flowModel.value, paramName)
    if (flowModelVal !== undefined) {
      return String(flowModelVal)
    }

    // 6. flowSetting root level
    const flowSettingVal = getValueByPath(flowSetting.value, paramName)
    if (flowSettingVal !== undefined) {
      return String(flowSettingVal)
    }

    return ''
  }

  /**
   * Parse URL by replacing {varName} patterns with actual values
   */
  const parseUrl = (
    originUrl: string,
    context: {
      atomValue?: Record<string, unknown>
      container?: Record<string, unknown>
    } = {},
  ): string => {
    return originUrl.replace(PLUGIN_URL_PARAM_REG, (_, key) => {
      return resolveUrlParam(key, context)
    })
  }

  return {
    parseUrl,
    resolveUrlParam,
  }
}

/**
 * Hook for data fetching from API or static options
 * Supports URL parameter replacement from multiple sources
 */
export function useDataSource(options: SelectDataConf) {
  const { t } = useI18n()
  const { parseUrl } = useUrlParser()

  const isLoading = ref(false)
  const list: Ref<ListItem[]> = ref([])
  const error = ref<string | null>(null)

  // Default options
  const paramId = options.paramId || 'id'
  const paramName = options.paramName || 'name'
  const initRequest = options.initRequest ?? true

  /**
   * Check if using API or static options
   */
  const isApiMode = (): boolean => {
    return Boolean(options.url)
  }

  const selectConf = computed(() => ({
    multiple: options.multiple ?? false,
    searchable: options.searchable ?? true,
    clearable: options.clearable ?? true,
  }))

  /**
   * Normalize list items with value/label mapping
   */
  const normalizeListItems = (rawData: unknown[]): ListItem[] => {
    return rawData
      .filter((item): item is Record<string, unknown> => 
        item !== null && typeof item === 'object'
      )
      .map((item) => ({
        ...item,
        value: item[paramId] as string | number,
        label: item[paramName] as string,
      }))
  }

  /**
   * Fetch data from API
   */
  const fetchFromApi = async () => {
    if (!options.url) return

    try {
      isLoading.value = true
      error.value = null

      const parsedUrl = parseUrl(options.url, {
        atomValue: options.atomValue as Record<string, unknown>,
        container: options.container as Record<string, unknown>,
      })

      const response = await get(parsedUrl)
      const resData = getResponseData(response, options.dataPath)

      list.value = normalizeListItems(resData)
    } catch (e: any) {
      error.value = e.message || t('common.loadFailed', 'Failed to load data')
      Message({
        message: error.value!,
        theme: 'error',
      })
    } finally {
      isLoading.value = false
    }
  }

  /**
   * Use static options
   */
  const useStaticOptions = () => {
    const staticOptions = options.options || []
    list.value = normalizeListItems(staticOptions)
  }

  /**
   * Refresh list based on data source type
   */
  const refreshList = () => {
    if (isApiMode()) {
      fetchFromApi()
    } else {
      useStaticOptions()
    }
  }

  // Watch options changes for static data source
  watch(
    () => options.options,
    () => {
      if (!isApiMode()) {
        useStaticOptions()
      }
    },
    { deep: true },
  )

  onMounted(() => {
    if (isApiMode()) {
      if (initRequest) {
        fetchFromApi()
      }
    } else {
      useStaticOptions()
    }
  })

  return {
    list,
    isLoading,
    error,
    refreshList,
    isApiMode,
    selectConf,
  }
}

export default useDataSource
