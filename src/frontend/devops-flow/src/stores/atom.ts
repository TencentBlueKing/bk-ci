import { ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchAtomModal, fetchAtomVersionList, type AtomModal, type AtomVersion } from '@/api/atom'
import { getAtomModalKey } from '@/utils/atom'

/**
 * 插件配置状态管理 Store
 * 管理插件配置（atomModal）和版本列表的缓存和加载状态
 */
export const useAtomStore = defineStore('atom', () => {
  // 插件配置缓存：atomCode-version -> AtomModal
  const atomModalMap = ref<Map<string, AtomModal>>(new Map())

  // 插件配置加载状态：atomCode-version -> loading
  const modalLoadingMap = ref<Map<string, boolean>>(new Map())

  // 版本列表缓存：atomCode -> AtomVersion[]
  const versionListCache = ref<Map<string, AtomVersion[]>>(new Map())

  // 版本列表加载状态：atomCode -> loading
  const versionLoadingMap = ref<Map<string, boolean>>(new Map())

  /**
   * 从缓存获取插件配置
   */
  function getCachedAtomModal(atomCode: string, version: string): AtomModal | null {
    const key = getAtomModalKey(atomCode, version)
    return atomModalMap.value.get(key) || null
  }

  /**
   * 检查插件配置是否正在加载
   */
  function isLoadingAtomModal(atomCode: string, version: string): boolean {
    const key = getAtomModalKey(atomCode, version)
    return modalLoadingMap.value.get(key) || false
  }

  /**
   * 检查版本列表是否正在加载
   */
  function isLoadingVersionList(atomCode: string): boolean {
    return versionLoadingMap.value.get(atomCode) || false
  }

  /**
   * 从缓存获取版本列表
   */
  function getCachedVersionList(atomCode: string): AtomVersion[] | null {
    return versionListCache.value.get(atomCode) || null
  }

  /**
   * 获取或加载插件配置
   */
  async function getAtomModal(
    atomCode: string,
    version: string,
    projectCode: string,
  ): Promise<AtomModal | null> {
    const key = getAtomModalKey(atomCode, version)

    // 先检查缓存
    if (atomModalMap.value.has(key)) {
      return atomModalMap.value.get(key)!
    }

    // 如果正在加载，等待加载完成
    if (modalLoadingMap.value.get(key)) {
      // 简单的轮询等待（实际项目中可以使用更好的方式）
      return new Promise((resolve) => {
        const checkInterval = setInterval(() => {
          if (!modalLoadingMap.value.get(key)) {
            clearInterval(checkInterval)
            resolve(atomModalMap.value.get(key) || null)
          }
        }, 50)
      })
    }

    // 开始加载
    modalLoadingMap.value.set(key, true)

    try {
      const atomModal = await fetchAtomModal({
        projectCode,
        atomCode,
        version,
        queryOfflineFlag: false,
      })

      // 缓存加载的配置
      atomModalMap.value.set(key, atomModal)
      return atomModal
    } catch (error) {
      console.error(`Failed to load atom modal for ${atomCode}@${version}:`, error)
      return null
    } finally {
      modalLoadingMap.value.set(key, false)
    }
  }

  /**
   * 获取或加载版本列表
   */
  async function getVersionList(
    atomCode: string,
    projectCode: string,
  ): Promise<AtomVersion[] | null> {
    // 先检查缓存
    if (versionListCache.value.has(atomCode)) {
      return versionListCache.value.get(atomCode)!
    }

    // 如果正在加载，等待加载完成
    if (versionLoadingMap.value.get(atomCode)) {
      // 简单的轮询等待（实际项目中可以使用更好的方式）
      return new Promise((resolve) => {
        const checkInterval = setInterval(() => {
          if (!versionLoadingMap.value.get(atomCode)) {
            clearInterval(checkInterval)
            resolve(versionListCache.value.get(atomCode) || null)
          }
        }, 50)
      })
    }

    // 开始加载
    versionLoadingMap.value.set(atomCode, true)

    try {
      const versionList = await fetchAtomVersionList({ projectCode, atomCode })
      versionListCache.value.set(atomCode, versionList)
      return versionList
    } catch (error) {
      console.error(`Failed to load version list for ${atomCode}:`, error)
      return null
    } finally {
      versionLoadingMap.value.set(atomCode, false)
    }
  }

  /**
   * 设置版本列表到缓存
   */
  function setVersionList(atomCode: string, versionList: AtomVersion[]) {
    versionListCache.value.set(atomCode, versionList)
  }

  /**
   * 设置插件配置到缓存
   */
  function setAtomModal(atomCode: string, version: string, atomModal: AtomModal) {
    const key = getAtomModalKey(atomCode, version)
    atomModalMap.value.set(key, atomModal)
  }

  /**
   * 清除指定插件配置缓存
   */
  function clearAtomModal(atomCode: string, version: string) {
    const key = getAtomModalKey(atomCode, version)
    atomModalMap.value.delete(key)
    modalLoadingMap.value.delete(key)
  }

  /**
   * 清除指定版本列表缓存
   */
  function clearVersionList(atomCode: string) {
    versionListCache.value.delete(atomCode)
    versionLoadingMap.value.delete(atomCode)
  }

  /**
   * 清除所有缓存
   */
  function clearAllCache() {
    atomModalMap.value.clear()
    modalLoadingMap.value.clear()
    versionListCache.value.clear()
    versionLoadingMap.value.clear()
  }

  return {
    // Getters
    getCachedAtomModal,
    isLoadingAtomModal,
    getCachedVersionList,
    isLoadingVersionList,

    // Actions
    getAtomModal,
    setAtomModal,
    clearAtomModal,
    getVersionList,
    setVersionList,
    clearVersionList,
    clearAllCache,
  }
})
