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
export const BOOLEAN = 'BOOLEAN'
export const ENUM = 'ENUM'
export const MULTIPLE = 'MULTIPLE'
export const SVN_TAG = 'SVN_TAG'
export const CODE_LIB = 'CODE_LIB'
export const CONTAINER_TYPE = 'CONTAINER_TYPE'

function paramType (typeConst) {
    return type => type === typeConst
}

export const DEFAULT_PARAM = {
    [STRING]: {
        id: 'string',
        defaultValue: 'value',
        desc: '',
        type: STRING,
        typeDesc: '字符串',
        required: true
    },
    [BOOLEAN]: {
        id: 'bool',
        defaultValue: true,
        desc: '',
        type: BOOLEAN,
        typeDesc: '布尔值',
        required: true
    },
    [ENUM]: {
        id: 'enum',
        defaultValue: '',
        desc: '',
        type: ENUM,
        typeDesc: '单选框',
        options: [],
        required: true
    },
    [MULTIPLE]: {
        id: 'multiple',
        defaultValue: [],
        desc: '',
        options: [],
        type: MULTIPLE,
        typeDesc: '复选框',
        required: true
    },
    [SVN_TAG]: {
        id: 'svntag',
        defaultValue: '',
        repoHashId: '',
        relativePath: '',
        desc: '',
        options: [],
        type: SVN_TAG,
        typeDesc: 'SVN分支或TAG',
        required: true
    },
    [CODE_LIB]: {
        id: 'codelib',
        defaultValue: '',
        scmType: 'CODE_GIT',
        desc: '',
        options: [],
        type: CODE_LIB,
        typeDesc: '代码库',
        required: true
    },
    [CONTAINER_TYPE]: {
        id: 'buildResource',
        defaultValue: '',
        containerType: {
            os: 'LINUX',
            buildType: 'DOCKER'
        },
        desc: '',
        options: [],
        type: CONTAINER_TYPE,
        typeDesc: '构建资源',
        required: true
    }
}

export const PARAM_LIST = Object.keys(DEFAULT_PARAM).map(key => ({
    id: key,
    name: DEFAULT_PARAM[key].typeDesc
}))

export const ParamComponentMap = {
    [STRING]: 'VuexInput',
    [BOOLEAN]: 'EnumInput',
    [ENUM]: 'Selector',
    [MULTIPLE]: 'Selector',
    [SVN_TAG]: 'Selector',
    [CODE_LIB]: 'Selector',
    [CONTAINER_TYPE]: 'Selector'
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

export const SVN_PATH_OPTION = {
    url: '/repository/api/user/repositories/{projectId}/hasPermissionList?permission=USE&repositoryType=CODE_SVN&page=1&pageSize=100',
    paramId: 'repositoryHashId',
    paramName: 'aliasName',
    searchable: true,
    hasAddItem: true
}

export const CODE_LIB_OPTION = {
    paramId: 'aliasName',
    paramName: 'aliasName',
    searchable: true,
    hasAddItem: true
}

export const CODE_LIB_TYPE = [
    {
        id: 'CODE_GIT',
        name: 'GIT'
    },
    {
        id: 'CODE_SVN',
        name: 'SVN'
    },
    {
        id: 'GITHUB',
        name: 'GITHUB'
    }
]

export const isStringParam = paramType(STRING)
export const isBooleanParam = paramType(BOOLEAN)
export const isEnumParam = paramType(ENUM)
export const isMultipleParam = paramType(MULTIPLE)
export const isSvnParam = paramType(SVN_TAG)
export const isCodelibParam = paramType(CODE_LIB)
export const isBuildResourceParam = paramType(CONTAINER_TYPE)
