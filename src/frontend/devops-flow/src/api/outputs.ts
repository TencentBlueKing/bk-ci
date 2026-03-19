/**
 * Outputs 相关 API 接口定义
 */

import { post, get } from '@/utils/http'
import { ARTIFACTORY_API_URL_PREFIX } from '@/utils/apiUrlPrefix'

/**
 * 制品/报告类型
 */
export type ArtifactoryType = 'CUSTOM_DIR' | 'IMAGE' | 'REPORT' | 'CREATIVE'

/**
 * 制品输出项
 */
export interface Output {
  artifactoryType: ArtifactoryType // 制品仓库类型
  name: string // 制品名称
  fullName?: string // 完整名称（带路径）
  path?: string // 路径
  fullPath: string // 完整路径
  size?: number // 文件大小（字节）
  folder?: boolean // 是否为文件夹
  properties?: Array<{ key: string; value: any }> // 属性列表
  appVersion?: string // 应用版本
  shortUrl?: string // 短链接
  md5?: string // MD5值
  createTime: number // 创建时间戳（秒）

  // 前端补充字段
  id?: string // 制品ID（前端生成）
  icon?: string // 图标名称（前端计算）
  type?: string // 类型（前端判断）
  reportType?: string // 报告类型
  downloadable?: boolean // 是否可下载
  isReportOutput?: boolean // 是否为报告输出
  isImageOutput?: boolean // 是否为镜像输出
  isApp?: boolean // 是否为应用
  indexFileUrl?: string // 索引文件URL
  [key: string]: any // 其他动态字段
}

/**
 * 文件元数据项(与 MetadataLabel 结构相同)
 */
export interface FileMetadataItem {
  labelKey: string // 标签键
  labelColorMap: Record<string, string> // 标签颜色映射
  enumType: boolean // 是否为枚举类型
  display: boolean // 是否显示
  category: string // 分类
  system: boolean // 是否为系统标签
  enableColorConfig: boolean // 是否启用颜色配置
  description?: string // 描述
  createdBy: string // 创建者
  createdDate: string // 创建时间
  lastModifiedBy: string // 最后修改者
  lastModifiedDate: string // 最后修改时间
}

export interface NodeMetadata {
  key: string
  value: string
  system?: boolean
}

/**
 * 文件详情信息
 */
export interface FileInfo {
  name: string // 文件名
  fullName?: string // 完整路径
  size: number // 文件大小（字节）
  folder?: boolean // 是否为文件夹
  createdTime: number // 创建时间（秒）
  modifiedTime: number // 修改时间（秒）
  nodeMetadata?: NodeMetadata[] // 元数据列表
  checksums?: {
    // 校验和
    sha256?: string
    sha1?: string
    md5?: string
  }
  properties?: Array<{ key: string; value: any }> // 属性列表
  [key: string]: any
}

/**
 * 获取制品输出列表请求参数
 */
export interface GetOutputsParams {
  projectId: string // 项目ID
  pipelineId: string // 流水线ID
  buildId: string // 构建ID
  page?: number // 页码
  pageSize?: number // 每页数量
  props?: Array<{ key: string; value: string }> // 筛选条件
  qualityMetadata: {
    key: string
    value: string
  }[]
}

/**
 * 获取制品输出列表响应数据
 */
export interface GetOutputsResponse {
  hasDownloadPermission?: boolean // 是否有下载权限
  records: Output[] // 制品列表
  count: number // 总数
  page: number // 当前页码
  pageSize: number // 每页数量
}

/**
 * 获取文件详情请求参数
 */
export interface GetFileInfoParams {
  projectId: string // 项目ID
  artifactoryType: ArtifactoryType // 制品仓库类型
  path: string // 文件路径
}

/**
 * 元数据标签项
 */
export interface MetadataLabel {
  labelKey: string // 标签键
  labelColorMap: Record<string, string> // 标签颜色映射
  enumType: boolean // 是否为枚举类型
  display: boolean // 是否显示
  category: string // 分类
  system: boolean // 是否为系统标签
  enableColorConfig: boolean // 是否启用颜色配置
  description?: string // 描述
  createdBy: string // 创建者
  createdDate: string // 创建时间
  lastModifiedBy: string // 最后修改者
  lastModifiedDate: string // 最后修改时间
}

/**
 * 获取元数据标签列表请求参数
 */
export interface GetMetadataLabelsParams {
  projectId: string // 项目ID
  pipelineId: string // 流水线ID
  debug?: boolean // 是否为调试模式
}

/**
 * 获取产出制品下载 URL 请求参数
 */
export interface GetDownloadUrlParams {
  projectId: string // 项目ID
  artifactoryType: ArtifactoryType // 制品仓库类型
  path: string // 文件路径
}

/**
 * 获取产出制品下载 URL 响应数据
 */
export interface GetDownloadUrlResponse {
  url: string // 下载链接
  url2: string // 备用下载链接
}

/**
 * 自定义文件夹树节点
 */
export interface CustomDirTreeNode {
  name: string // 文件夹名称
  fullPath: string // 完整路径
  children: CustomDirTreeNode[] // 子节点列表
  isOpen?: boolean // 是否展开
  loading?: boolean // 是否加载中
  leaf?: boolean // 是否为叶子节点，默认 false，接口返回空 children 后置为 true
}

/**
 * 获取自定义文件夹树请求参数
 */
export interface GetCustomDirTreeParams {
  projectId: string // 项目ID
  [key: string]: any // 其他查询参数
}

/**
 * 复制文件请求参数
 */
export interface CopyFileParams {
  projectId: string // 项目ID
  srcArtifactoryType: string // 源制品仓库类型
  srcFileFullPaths: string[] // 源文件路径列表
  dstArtifactoryType: ArtifactoryType // 目标制品仓库类型
  dstDirFullPath: string // 目标目录路径
}

/**
 * 获取制品输出列表
 * @param params 请求参数
 * @returns 制品列表响应数据
 */
export async function requestOutputs(params: GetOutputsParams): Promise<GetOutputsResponse> {
  try {
    const { projectId, pipelineId, buildId, ...otherParams } = params
    const hasBuildId = !!buildId
    const data = await post<Output[]>(
      `${ARTIFACTORY_API_URL_PREFIX}/user/pipeline/output/${projectId}/${pipelineId}/${hasBuildId ? `${buildId}/` : ''}search`,
      otherParams,
    )
    return {
      page: 1,
      pageSize: data.length,
      count: data.length,
      records: data,
    }
  } catch (error) {
    throw error
  }
}

/**
 * 获取文件详情(返回元数据数组)
 * @param params 请求参数
 * @returns 文件元数据数组
 */
export async function requestFileInfo(params: GetFileInfoParams): Promise<FileInfo> {
  const { projectId, artifactoryType, path } = params
  try {
    const res = await get<FileInfo>(
      `${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/${projectId}/${artifactoryType}/show?path=${encodeURIComponent(path)}`,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 获取元数据标签列表
 * @param params 请求参数
 * @returns 元数据标签列表
 */
export async function requestMetadataLabels(
  params: GetMetadataLabelsParams,
): Promise<MetadataLabel[]> {
  const { projectId, pipelineId, debug = false } = params
  try {
    const res = await get<MetadataLabel[]>(
      `${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/quality/metadata/${projectId}/pipeline/${pipelineId}?debug=${debug}`,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 获取产出制品下载 URL
 * @param params 请求参数
 * @returns 下载 URL
 */
export async function requestDownloadUrl(
  params: GetDownloadUrlParams,
): Promise<GetDownloadUrlResponse> {
  const { projectId, artifactoryType, path } = params
  try {
    const res = await post<GetDownloadUrlResponse>(
      `${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/${projectId}/${artifactoryType}/downloadUrl?path=${encodeURIComponent(path)}`,
    )
    return res
  } catch (error) {
    throw error
  }
}

/**
 * 获取自定义文件夹树
 * @param params 请求参数
 * @returns 文件夹树
 */
export function requestCustomDirTree(params: GetCustomDirTreeParams): Promise<CustomDirTreeNode> {
  const { projectId, ...restParams } = params
  try {
    return get<CustomDirTreeNode>(
      `${ARTIFACTORY_API_URL_PREFIX}/user/custom-repo/${projectId}/dir/tree`,
      {
        params: restParams,
      },
    )
  } catch (error) {
    throw error
  }
}

/**
 * 复制文件
 * @param params 请求参数
 * @returns 是否成功
 */
export function requestCopyFile(params: CopyFileParams): Promise<boolean> {
  try {
    return post<boolean>(`${ARTIFACTORY_API_URL_PREFIX}/user/artifactories/file/copy`, params)
  } catch (error) {
    throw error
  }
}
