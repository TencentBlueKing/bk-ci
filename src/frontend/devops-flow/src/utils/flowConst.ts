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

export const repoTypeMap = {
  CUSTOM_DIR: 'flow.execute.customRepo',
  PIPELINE: 'flow.execute.flowRepo',
  IMAGE: 'flow.execute.imageRepo',
  REPORT: 'flow.execute.reportRepo',
}

export const repoTypeNameMap = {
  CUSTOM_DIR: 'custom',
  PIPELINE: 'pipeline',
  IMAGE: 'image',
  REPORT: 'report',
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
    CUSTOMIZE: 'CUSTOMIZE'
}
