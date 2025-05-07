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

export const STRING = 'STRING'
export const TEXTAREA = 'TEXTAREA'
export const BOOLEAN = 'BOOLEAN'
export const ENUM = 'ENUM'
export const MULTIPLE = 'MULTIPLE'
export const CHECKBOX = 'CHECKBOX'
export const SVN_TAG = 'SVN_TAG'
export const GIT_REF = 'GIT_REF'
export const CODE_LIB = 'CODE_LIB'
export const CONTAINER_TYPE = 'CONTAINER_TYPE'
export const ARTIFACTORY = 'ARTIFACTORY'
export const SUB_PIPELINE = 'SUB_PIPELINE'
export const CUSTOM_FILE = 'CUSTOM_FILE'
export const REPO_REF = 'REPO_REF'

function paramType (typeConst) {
    return type => type === typeConst
}

export const DEFAULT_PARAM = {
    [STRING]: {
        id: '',
        name: '',
        defaultValue: '',
        defalutValueLabel: 'defaultValue',
        defaultValueLabelTips: 'defaultValueDesc',
        desc: '',
        type: STRING,
        typeDesc: 'string',
        required: true,
        readOnly: false,
        category: ''
    },
    [TEXTAREA]: {
        id: 'textarea',
        name: 'textarea',
        defaultValue: '',
        desc: '',
        type: TEXTAREA,
        typeDesc: 'textarea',
        required: true,
        readOnly: false,
        category: ''
    },
    [BOOLEAN]: {
        id: 'bool',
        name: 'bool',
        defaultValue: true,
        defalutValueLabel: 'defaultValue',
        defaultValueLabelTips: 'defaultValueDesc',
        desc: '',
        type: BOOLEAN,
        typeDesc: 'bool',
        required: true,
        readOnly: false,
        category: ''
    },
    [ENUM]: {
        id: 'select',
        name: 'select',
        defaultValue: '',
        defalutValueLabel: 'defaultValue',
        defaultValueLabelTips: 'defaultValueDesc',
        desc: '',
        type: ENUM,
        typeDesc: 'enum',
        options: [],
        required: true,
        readOnly: false,
        category: ''
    },
    [MULTIPLE]: {
        id: 'multiple',
        name: 'multiple',
        defaultValue: '',
        defalutValueLabel: 'defaultValue',
        defaultValueLabelTips: 'defaultValueDesc',
        desc: '',
        options: [],
        type: MULTIPLE,
        typeDesc: 'multiple',
        required: true,
        readOnly: false,
        category: ''
    },
    [CHECKBOX]: {
        id: 'checkbox',
        name: 'checkbox',
        defaultValue: false,
        defalutValueLabel: 'defaultValue',
        desc: '',
        type: CHECKBOX,
        typeDesc: 'checkbox',
        required: true,
        readOnly: false,
        category: ''
    },
    [SVN_TAG]: {
        id: 'svntag',
        name: 'svntag',
        defaultValue: '',
        defalutValueLabel: 'defaultValue',
        defaultValueLabelTips: 'defaultValueDesc',
        repoHashId: '',
        relativePath: '',
        desc: '',
        options: [],
        type: SVN_TAG,
        typeDesc: 'svntag',
        required: true,
        readOnly: false,
        category: ''
    },
    [GIT_REF]: {
        id: 'gitref',
        name: 'gitref',
        defaultValue: '',
        defalutValueLabel: 'defaultValue',
        defaultValueLabelTips: 'defaultValueDesc',
        repoHashId: '',
        desc: '',
        options: [],
        type: GIT_REF,
        typeDesc: 'gitref',
        required: true,
        readOnly: false,
        category: ''
    },
    [REPO_REF]: {
        id: 'reporef',
        name: 'reporef',
        defaultValue: {
            'repo-name': '',
            branch: ''
        },
        defalutValueLabel: 'defaultValue',
        defaultValueLabelTips: 'defaultValueDesc',
        type: REPO_REF,
        typeDesc: 'reporef',
        required: true,
        readOnly: false,
        category: ''
    },
    [CODE_LIB]: {
        id: 'codelib',
        name: 'codelib',
        defaultValue: '',
        defalutValueLabel: 'defaultValue',
        defaultValueLabelTips: 'defaultValueDesc',
        scmType: 'CODE_GIT',
        desc: '',
        options: [],
        type: CODE_LIB,
        typeDesc: 'codelib',
        required: true,
        readOnly: false,
        category: ''
    },
    [SUB_PIPELINE]: {
        id: 'subPipeline',
        name: 'subPipeline',
        defaultValue: '',
        defalutValueLabel: 'defaultValue',
        defaultValueLabelTips: 'defaultValueDesc',
        desc: '',
        options: [],
        type: SUB_PIPELINE,
        typeDesc: 'subPipeline',
        required: true,
        readOnly: false,
        category: ''
    },
    [CUSTOM_FILE]: {
        id: 'file',
        name: 'file',
        defaultValue: '',
        defalutValueLabel: 'fileDefaultValueLabel',
        defaultValueLabelTips: 'customFileLabelTips',
        enableVersionControl: false,
        desc: '',
        type: CUSTOM_FILE,
        typeDesc: 'custom_file',
        required: true,
        readOnly: false,
        category: ''
    }
}

export const CHECK_DEFAULT_PARAM = {
    [STRING]: {
        key: 'string',
        value: 'value',
        desc: '',
        valueType: STRING,
        required: true
    },
    [TEXTAREA]: {
        key: 'textarea',
        value: '',
        desc: '',
        valueType: TEXTAREA,
        required: true
    },
    [BOOLEAN]: {
        key: 'bool',
        value: true,
        desc: '',
        valueType: BOOLEAN,
        required: true
    },
    [ENUM]: {
        key: 'enum',
        value: '',
        desc: '',
        valueType: ENUM,
        options: [],
        required: true
    },
    [MULTIPLE]: {
        key: 'multiple',
        value: '',
        desc: '',
        options: [],
        valueType: MULTIPLE,
        required: true
    },
    [CHECKBOX]: {
        key: 'checkbox',
        value: false,
        desc: '',
        valueType: CHECKBOX,
        required: true
    }
}
export const CHECK_PARAM_LIST = Object.keys(CHECK_DEFAULT_PARAM).map(key => ({
    id: key,
    name: DEFAULT_PARAM[key].typeDesc
}))

// 常量时展示的变量列表
export const CONST_TYPE_LIST = [STRING, TEXTAREA, ENUM, MULTIPLE]

export const PARAM_LIST = Object.keys(DEFAULT_PARAM).map(key => ({
    id: key,
    name: DEFAULT_PARAM[key].typeDesc
}))

export function getParamsDefaultValueLabel (type) {
    return DEFAULT_PARAM[type] && DEFAULT_PARAM[type].defalutValueLabel ? DEFAULT_PARAM[type].defalutValueLabel : 'defaultValue'
}

export function getParamsDefaultValueLabelTips (type) {
    return DEFAULT_PARAM[type] && DEFAULT_PARAM[type].defaultValueLabelTips ? DEFAULT_PARAM[type].defaultValueLabelTips : 'defaultValueDesc'
}

export const ParamComponentMap = {
    [STRING]: 'VuexInput',
    [TEXTAREA]: 'VuexTextarea',
    [BOOLEAN]: 'EnumInput',
    [ENUM]: 'Selector',
    [MULTIPLE]: 'Selector',
    [SVN_TAG]: 'Selector',
    [GIT_REF]: 'Selector',
    [CODE_LIB]: 'Selector',
    [CONTAINER_TYPE]: 'Selector',
    [ARTIFACTORY]: 'Selector',
    [SUB_PIPELINE]: 'Selector',
    [CUSTOM_FILE]: 'FileParamInput',
    [REPO_REF]: 'CascadeRequestSelector'
}

export const BOOLEAN_LIST = [
    {
        value: true,
        label: true
    },
    {
        value: false,
        label: false
    }
]

export function getRepoOption (type = 'CODE_SVN', paramId = 'repositoryHashId') {
    return {
        url: `/repository/api/user/repositories/{projectId}/hasPermissionList?permission=USE&repositoryType=${type}&page=1&pageSize=1000`,
        paramId,
        paramName: 'aliasName',
        searchable: true,
        hasAddItem: true
    }
}

export function getBranchOption (name) {
    if (!name) return {}
    return {
        url: `/process/api/user/buildParam/{projectId}/repository/refs?repositoryType=NAME&repositoryId=${name}`,
        paramId: 'key',
        paramName: 'value',
        searchable: true,
        settingKey: 'key',
        displayKey: 'value'
    }
}

export const CODE_LIB_OPTION = {
    paramId: 'aliasName',
    paramName: 'aliasName',
    searchable: true,
    hasAddItem: true
}

export const SUB_PIPELINE_OPTION = {
    url: '/process/api/user/pipelines/{projectId}/hasPermissionList?permission=EXECUTE&excludePipelineId={pipelineId}&limit=-1',
    paramId: 'pipelineName',
    paramName: 'pipelineName',
    searchable: true
}

export const CODE_LIB_TYPE = [
    {
        id: 'CODE_GIT',
        name: 'GIT'
    },
    {
        id: 'GITHUB',
        name: 'GITHUB'
    },
    {
        id: 'CODE_GITLAB',
        name: 'GITLAB'
    }
]

export function isRemoteType (param) {
    return param?.payload?.type === 'remote'
}

export const isStringParam = paramType(STRING)
export const isTextareaParam = paramType(TEXTAREA)
export const isBooleanParam = paramType(BOOLEAN)
export const isEnumParam = paramType(ENUM)
export const isMultipleParam = paramType(MULTIPLE)
export const isCheakboxParam = paramType(CHECKBOX)
export const isSvnParam = paramType(SVN_TAG)
export const isGitParam = paramType(GIT_REF)
export const isRepoParam = paramType(REPO_REF)
export const isCodelibParam = paramType(CODE_LIB)
export const isBuildResourceParam = paramType(CONTAINER_TYPE)
export const isArtifactoryParam = paramType(ARTIFACTORY)
export const isSubPipelineParam = paramType(SUB_PIPELINE)
export const isFileParam = paramType(CUSTOM_FILE)
