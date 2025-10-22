/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
export const PLUGIN_URL_PARAM_REG = /\{(.*?)(\?){0,1}\}/g
export const allVersionKeyList = [
    'BK_CI_MAJOR_VERSION',
    'BK_CI_MINOR_VERSION',
    'BK_CI_FIX_VERSION'
]
export const semverVersionKeySet = new Set(allVersionKeyList)

export const NAME_FILTER_TYPE = 'filterByName'
export const CREATOR_FILTER_TYPE = 'filterByCreator'
export const FILTER_BY_LABEL = 'filterByLabel'
export const FILTER_BY_PAC_REPO = 'filterByPacRepo'
export const FILTER_BY_VIEW_ID = 'filterByViewIds'

export const FILTER_BY_DELETER = 'filterByViewIds'

export const PIPELINE_FILTER_PIPELINENAME = 'filterByPipelineName'
export const PIPELINE_FILTER_CREATOR = 'filterByCreator'
export const PIPELINE_FILTER_VIEWIDS = 'filterByViewIds'
export const PIPELINE_FILTER_LABELS = 'filterByLabels'

export const jobConst = {
    LINUX: 'Linux',
    MACOS: 'macOS',
    WINDOWS: 'Windows',
    NONE: 'noEnv'
}

export const buildEnvMap = {
    thirdPartyAgentId: 'THIRD_PARTY_AGENT_ID',
    thirdPartyAgentEnvId: 'THIRD_PARTY_AGENT_ENV',
    dockerBuildVersion: 'DOCKER',
    tstackAgentId: 'TSTACK'
}

export const VIEW_CONDITION = {
    LIKE: 'LIKE',
    INCLUDE: 'INCLUDE'
}

export function getVersionConfig () {
    return {
        BK_CI_MAJOR_VERSION: {
            type: 'STRING',
            desc: (window.pipelineVue && window.pipelineVue.$i18n.t('preview.majorVersion')) || 'majorVersion',
            default: '0',
            placeholder: 'BK_CI_MAJOR_VERSION'
        },
        BK_CI_MINOR_VERSION: {
            type: 'STRING',
            desc: (window.pipelineVue && window.pipelineVue.$i18n.t('preview.minorVersion')) || 'minorVersion',
            default: '0',
            placeholder: 'BK_CI_MINOR_VERSION'
        },
        BK_CI_FIX_VERSION: {
            type: 'STRING',
            desc: (window.pipelineVue && window.pipelineVue.$i18n.t('preview.fixVersion')) || 'fixVersion',
            default: '0',
            placeholder: 'BK_CI_FIX_VERSION'
        }
    }
}

export function pluginUrlParse (originUrl, query) {
    /* eslint-disable */
    return new Function('ctx', `return '${originUrl.replace(PLUGIN_URL_PARAM_REG, '\'\+ (ctx.hasOwnProperty(\'$1\') ? ctx[\'$1\'] : "") \+\'')}'`)(query)
    /* eslint-enable */
}

export const errorTypeMap = [
    {
        title: 'systemError',
        icon: 'error-system'
    },
    {
        title: 'userError',
        icon: 'error-user'
    },
    {
        title: 'thirdPartyError',
        icon: 'third-party'
    },
    {
        title: 'pluginError',
        icon: 'error-plugin'
    },
    {
        title: 'containerError',
        icon: 'error-node'
    }
]

export const repoTypeMap = {
    CUSTOM_DIR: 'details.customRepo',
    PIPELINE: 'details.pipelineRepo',
    IMAGE: 'details.imageRepo'
}
export const repoTypeNameMap = {
    CUSTOM_DIR: 'custom',
    PIPELINE: 'pipeline',
    IMAGE: 'image'
}
export const fileExtIconMap = {
    txt: ['.json', '.txt', '.md'],
    zip: ['.zip', '.tar', '.tar.gz', '.tgz', '.jar', '.gz'],
    apkfile: ['.apk'],
    ipafile: ['.ipa']
}
export function extForFile (name) {
    const defaultIcon = 'file'
    const pos = name.lastIndexOf('.')
    if (pos > -1) {
        const ext = name.substring(pos)
        return Object.keys(fileExtIconMap).find(key => {
            const arr = fileExtIconMap[key]
            return arr.includes(ext)
        }) ?? defaultIcon
    }
    return defaultIcon
}
export const ORDER_ENUM = {
    ascending: 'ASC',
    descending: 'DESC'
}
export const PIPELINE_SORT_FILED = {
    pipelineName: 'NAME',
    createTime: 'CREATE_TIME',
    latestBuildStartDate: 'LAST_EXEC_TIME',
    updateTime: 'UPDATE_TIME'
}

export const UI_MODE = 'MODEL'
export const CODE_MODE = 'YAML'

export const templateTypeEnum = {
    CONSTRAIN: 'CONSTRAIN',
    FREEDOM: 'FREEDOM',
    PUBLIC: 'PUBLIC',
    CUSTOMIZE: 'CUSTOMIZE'
}

export const BUILD_HISTORY_TABLE_DEFAULT_COLUMNS = [
    'buildNum',
    'material',
    'startType',
    'startTime',
    'endTime',
    'totalTime',
    'executeTime',
    'artifactList',
    'artifactQuality',
    'pipelineVersion',
    'remark',
    'errorCode'
]
export const BUILD_HISTORY_TABLE_COLUMNS_MAP = {
    buildNum: {
        index: 0,
        id: 'buildNum',
        label: 'buildNum',
        width: 120
    },
    stageStatus: {
        index: 1,
        id: 'stageStatus',
        label: 'history.stageStatus',
        width: localStorage.getItem('stageStatusWidth') ?? 520
    },
    material: {
        index: 2,
        id: 'material',
        label: 'editPage.material',
        width: localStorage.getItem('materialWidth') ?? 500
    },
    startType: {
        index: 3,
        id: 'startType',
        label: 'history.triggerInfo',
        width: 120
    },
    queueTime: {
        index: 4,
        id: 'queueTime',
        label: 'history.tableMap.queueTime',
        width: 120
    },
    startTime: {
        index: 5,
        id: 'startTime',
        label: 'history.tableMap.startTime',
        width: 120
    },
    endTime: {
        index: 6,
        id: 'endTime',
        label: 'history.tableMap.endTime',
        width: 120
    },
    totalTime: {
        index: 7,
        id: 'totalTime',
        label: 'details.totalCost',
        width: localStorage.getItem('totalTimeWidth') ?? 120
    },
    executeTime: {
        index: 8,
        id: 'executeTime',
        label: 'details.executeCost',
        width: localStorage.getItem('executeTimeWidth') ?? 120
    },
    artifactList: {
        index: 9,
        id: 'artifactList',
        label: 'history.artifactList',
        width: 180
    },
    artifactQuality: {
        index: 10,
        id: 'artifactQuality',
        label: 'artifactQuality',
        width: 280
    },
    appVersions: {
        index: 11,
        id: 'appVersions',
        label: 'history.tableMap.appVersions'
    },
    remark: {
        index: 12,
        id: 'remark',
        label: 'history.remark',
        minWidth: 160,
        width: localStorage.getItem('remarkWidth') ?? 200
    },
    recommendVersion: {
        index: 13,
        id: 'recommendVersion',
        label: 'history.tableMap.recommendVersion'
    },
    pipelineVersion: {
        index: 14,
        id: 'pipelineVersion',
        label: 'history.tableMap.pipelineVersion'
    },
    entry: {
        index: 15,
        id: 'entry',
        label: 'history.tableMap.entry',
        width: 120,
        hiddenInHistory: true,
        entries: [{
            type: '',
            label: 'detail'

        }, {
            type: 'partView',
            label: 'details.partView'

        }, {
            type: 'codeRecords',
            label: 'details.codeRecords'
        }, {
            type: 'output',
            label: 'details.outputReport'
        }]
    },
    errorCode: {
        index: 16,
        width: 280,
        id: 'errorCode',
        label: 'history.errorCode'
    },
    buildMsg: {
        index: 17,
        width: 180,
        id: 'buildMsg',
        label: 'history.buildMsg'
    }
}

export const pipelineTabIdMap = {
    pipeline: 'pipeline',
    trigger: 'trigger',
    notice: 'notice',
    setting: 'setting'
}

export const VERSION_STATUS_ENUM = {
    COMMITTING: 'COMMITTING',
    BRANCH: 'BRANCH',
    RELEASED: 'RELEASED'
}

export const TARGET_ACTION_ENUM = {
    COMMIT_TO_MASTER: 'COMMIT_TO_MASTER', // 提交到默认分支
    COMMIT_TO_SOURCE_BRANCH: 'COMMIT_TO_SOURCE_BRANCH', // 提交到Dev(源分支)分支
    COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE: 'COMMIT_TO_SOURCE_BRANCH_AND_REQUEST_MERGE', // 提交到Dev(源分支)并创建MR
    CHECKOUT_BRANCH_AND_REQUEST_MERGE: 'CHECKOUT_BRANCH_AND_REQUEST_MERGE', // 新增分支并创建 MR 到默认分支
    COMMIT_TO_BRANCH: 'COMMIT_TO_BRANCH' // 提交到指定分支
}
