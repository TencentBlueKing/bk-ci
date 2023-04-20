/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

export const NAME_FILTER_TYPE = 'filterByName'
export const CREATOR_FILTER_TYPE = 'filterByCreator'
export const FILTER_BY_LABEL = 'filterByLabel'
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

export const BUILD_HISTORY_TABLE_DEFAULT_COLUMNS = [
    'buildNum',
    'material',
    'startType',
    'startTime',
    'endTime',
    'totalTime',
    'artifactList',
    'pipelineVersion',
    'remark',
    'errorCode'
]

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
    }
]

export const repoTypeMap = {
    CUSTOM_DIR: 'details.customRepo',
    PIPELINE: 'details.pipelineRepo',
    IMAGE: 'details.imageRepo'
}
export const repoTypeNameMap = {
    CUSTOM_DIR: 'custom',
    PIPELINE: 'pipeline'
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
