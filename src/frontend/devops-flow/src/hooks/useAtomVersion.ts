import { useAtomStore } from '@/stores/atom'
import type { AtomVersion, AtomModal } from '@/api/atom'

export const DEFAULT_VERSION = '1.*'
/**
 * 插件版本管理 Hook
 * 负责版本列表获取、默认版本选择和配置加载
 */

export interface UseAtomVersionOptions {
  projectCode: string
}

export interface UseAtomVersionReturn {
  /** 加载版本列表 */
  loadVersionList: (atomCode: string) => Promise<AtomVersion[]>
  /** 获取默认版本 */
  getDefaultVersion: (versionList: AtomVersion[]) => string
  /** 加载插件配置 */
  loadAtomModal: (atomCode: string, version: string) => Promise<AtomModal>
  /** 加载状态 */
  isLoadingVersion: (atomCode: string) => boolean
  isLoadingModal: (atomCode: string, version: string) => boolean
}

/**
 * 版本选择策略：
 * 1. 优先选择 recommendFlag = true 的版本
 * 2. 其次选择 defaultFlag = true 的版本
 * 3. 再选择 latestFlag = true 的版本
 * 4. 最后选择列表第一个版本
 */
function selectDefaultVersion(versionList: AtomVersion[]): string {
  if (!versionList || versionList.length === 0) {
    return DEFAULT_VERSION
  }

  // 优先推荐版本
  const recommendVersion = versionList.find((v) => v.recommendFlag)
  if (recommendVersion) {
    return recommendVersion.version
  }

  // 其次默认版本
  const defaultVersion = versionList.find((v) => v.defaultFlag)
  if (defaultVersion) {
    return defaultVersion.version
  }

  // 再选最新版本
  const latestVersion = versionList.find((v) => v.latestFlag)
  if (latestVersion) {
    return latestVersion.version
  }

  // 最后返回第一个
  return versionList[0]?.version || DEFAULT_VERSION
}

export function useAtomVersion(options: UseAtomVersionOptions): UseAtomVersionReturn {
  const { projectCode } = options
  const atomStore = useAtomStore()

  /**
   * 加载版本列表
   */
  const loadVersionList = async (atomCode: string): Promise<AtomVersion[]> => {
    const versionList = await atomStore.getVersionList(atomCode, projectCode)
    if (!versionList) {
      throw new Error(`Failed to load version list for ${atomCode}`)
    }
    return versionList
  }

  /**
   * 获取默认版本
   */
  const getDefaultVersion = (versionList: AtomVersion[]): string => {
    return selectDefaultVersion(versionList)
  }

  /**
   * 加载插件配置
   */
  const loadAtomModal = async (atomCode: string, version: string): Promise<AtomModal> => {
    const atomModal = await atomStore.getAtomModal(atomCode, version, projectCode)
    if (!atomModal) {
      throw new Error(`Failed to load atom modal for ${atomCode}@${version}`)
    }
    return atomModal
  }

  /**
   * 检查版本列表是否正在加载
   */
  const isLoadingVersion = (atomCode: string): boolean => {
    return atomStore.isLoadingVersionList(atomCode)
  }

  /**
   * 检查插件配置是否正在加载
   */
  const isLoadingModal = (atomCode: string, version: string): boolean => {
    return atomStore.isLoadingAtomModal(atomCode, version)
  }

  return {
    loadVersionList,
    getDefaultVersion,
    loadAtomModal,
    isLoadingVersion,
    isLoadingModal,
  }
}
