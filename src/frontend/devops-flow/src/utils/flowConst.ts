export const ORDER_ENUM = {
  ascending: 'asc',
  descending: 'desc',
}
export const FLOW_SORT_FILED = {
  flowName: 'NAME',
  createDate: 'CREATE_TIME',
  latestBuildStartDate: 'LAST_EXEC_TIME',
  updateTime: 'UPDATE_TIME',
}
export const UI_MODE = 'MODEL'
export const CODE_MODE = 'YAML'
export const modeList = [CODE_MODE, UI_MODE]

export const VERSION_STATUS_ENUM = {
  COMMITTING: 'COMMITTING',
  BRANCH: 'BRANCH',
  RELEASED: 'RELEASED',
}

export const ARTIFACTORY_TYPE = {
  CUSTOM_DIR: 'CUSTOM_DIR',
  IMAGE: 'IMAGE',
  REPORT: 'REPORT',
  CREATIVE: 'CREATIVE',
} as const

export const ARTIFACT_TYPES = [
  ARTIFACTORY_TYPE.CUSTOM_DIR,
  ARTIFACTORY_TYPE.IMAGE,
  ARTIFACTORY_TYPE.CREATIVE,
] as const

export const repoTypeMap: Record<string, string> = {
  [ARTIFACTORY_TYPE.CUSTOM_DIR]: 'flow.execute.customRepo',
  [ARTIFACTORY_TYPE.IMAGE]: 'flow.execute.imageRepo',
  [ARTIFACTORY_TYPE.REPORT]: 'flow.execute.reportRepo',
  [ARTIFACTORY_TYPE.CREATIVE]: 'flow.execute.creativeRepo',
}

export const repoTypeNameMap: Record<string, string> = {
  [ARTIFACTORY_TYPE.CUSTOM_DIR]: 'custom',
  [ARTIFACTORY_TYPE.IMAGE]: 'image',
  [ARTIFACTORY_TYPE.REPORT]: 'report',
  [ARTIFACTORY_TYPE.CREATIVE]: 'creative',
}

export const fileExtIconMap: Record<string, string[]> = {
  txt: ['.json', '.txt', '.md'],
  zip: ['.zip', '.tar', '.tar.gz', '.tgz', '.jar', '.gz'],
  apkfile: ['.apk'],
  ipafile: ['.ipa'],
}

export function extForFile(name: string) {
  const defaultIcon = 'file'
  const pos = name.lastIndexOf('.')
  if (pos > -1) {
    const ext = name.substring(pos)
    return (
      Object.keys(fileExtIconMap).find((key) => {
        const arr = fileExtIconMap[key]
        return arr?.includes(ext)
      }) ?? defaultIcon
    )
  }
  return defaultIcon
}

export const allVersionKeyList = ['BK_CI_MAJOR_VERSION', 'BK_CI_MINOR_VERSION', 'BK_CI_FIX_VERSION']

export const templateTypeEnum = {
  CONSTRAIN: 'CONSTRAIN',
  FREEDOM: 'FREEDOM',
  PUBLIC: 'PUBLIC',
  CUSTOMIZE: 'CUSTOMIZE',
}

export const errorTypeMap = [
  { title: 'flow.content.systemError', icon: 'info-circle' },
  { title: 'flow.content.userError', icon: 'user' },
  { title: 'flow.content.thirdPartyError', icon: 'link' },
  { title: 'flow.content.pluginError', icon: 'is-store' },
  { title: 'flow.content.containerError', icon: 'pipeline' },
] as const
