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

export const regionList = ['TC', 'SH', 'BJ', 'GZ', 'CD', 'GROUP']

export const codelibConfig = {
    svn: {
        credentialTypes: 'SSH_PRIVATEKEY',
        label: 'SVN',
        typeName: 'codeSvn'
    },
    svn_http: {
        credentialTypes: 'USERNAME_PASSWORD',
        label: 'SVN',
        typeName: 'codeSvn'
    },
    git: {
        credentialTypes: 'TOKEN_SSH_PRIVATEKEY',
        label: 'Git',
        typeName: 'codeGit'
    },
    github: {
        label: 'Github',
        typeName: 'github'
    },
    git_http: {
        credentialTypes: 'TOKEN_USERNAME_PASSWORD',
        label: 'Git',
        typeName: 'codeGit'
    },
    gitlab: {
        credentialTypes: 'ACCESSTOKEN',
        label: 'Gitlab',
        typeName: 'codeGitLab'
    },
    tgit: {
        credentialTypes: 'TOKEN_SSH_PRIVATEKEY',
        label: 'TGit',
        typeName: 'codeTGit'
    },
    tgit_https: {
        credentialTypes: 'USERNAME_PASSWORD',
        label: 'TGit',
        typeName: 'codeTGit'
    }
}

export const codelibTypes = [
    'SVN',
    'Github',
    'Gitlab'
]

/**
 * 根据仓库类型获取仓库配置
 * method getCodelibConfig
 * @param {string} typeName
 */
export function getCodelibConfig (typeName, svnType = 'ssh', authType = 'ssh') {
    let type = typeName.toLowerCase().replace(/^\S*?([gitlab|git|svn|github|tgit])/i, '$1')
    console.log(type)
    if (type === 'svn' && svnType === 'http') {
        type = 'svn_http'
    }
    if (type === 'git' && authType === 'HTTP') {
        type = 'git_http'
    }
    if (type === 'tgit' && authType === 'HTTPS') {
        type = 'tgit_https'
    }
    console.log(typeName, svnType, authType, type)
    return codelibConfig[type]
}

/**
 * 判断是代码库是否为svn
 * method isSvn
 * @param {string} typeName
 */
export function judgementCodelibType (codeType = 'codeSvn') {
    return typeName => {
        return typeName === codeType
    }
}

/**
 * 判断是代码库是否为svn
 * method isSvn
 * @param {string} typeName
 */
export const isSvn = judgementCodelibType('codeSvn')
/**
 * 判断是代码库是否为Git
 * method isGit
 * @param {string} typeName
 */
export const isGit = judgementCodelibType('codeGit')

/**
 * 判断是代码库是否为GitLab
 * method isGitlab
 * @param {string} typeName
 */
export const isGitLab = judgementCodelibType('codeGitLab')

/**
 * 判断是代码库是否为Github
 * method isGitlab
 * @param {string} typeName
 */
export const isGithub = judgementCodelibType('github')
