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

import {
    regionList,
    isTGit,
    isSvn,
    isGitLab,
    isGithub
} from '../config'

export function parsePathAlias (type, path, authType, svnType) {
    let reg = ''
    let msg = ''
    const codelibLocaleObj = window.devops.$i18n.t('codelib')

    switch (true) {
        case isGithub(type):
            reg = /^https\:\/\/(github)(\.com)\/([\w\W\.\-\_\/\+]+)\.git$/i
            msg = `${codelibLocaleObj.githubRule}${type}${codelibLocaleObj.address}`
            break
        case isSvn(type) && svnType === 'ssh':
            reg = /^svn\+ssh\:\/\/([\@\-\.a-z0-9A-Z]+)(:[0-9]{2,5})?\/([\w\W\.\-\_\/\+]+)$/i
            msg = `${codelibLocaleObj.svnSshRule}${type}${codelibLocaleObj.address}`
            break
        case isSvn(type) && svnType === 'http':
            reg = /^http\:\/\/([\-\.a-z0-9A-Z]+)(:[0-9]{2,5})?\/([\w\W\.\-\_\/\+]+)$/i
            msg = `${codelibLocaleObj.httpRule}${type}${codelibLocaleObj.address}`
            break
        case isGitLab(type):
            reg = /^https?\:\/\/([\-\.a-z0-9A-Z]+)(:[0-9]{2,5})?\/([\w\W\.\-\_\/\+]+)\.git$/i
            msg = `${codelibLocaleObj.httpsRule}${type}${codelibLocaleObj.address}`
            break
        case (authType === 'T_GIT_OAUTH') || (isTGit(type) && authType === 'HTTPS'):
            reg = /^https\:\/\/git(\.code)?(\.tencent)\.com[\:|\/](.*)\.git$/
            msg = `${codelibLocaleObj.tgitHttpRule}${type}${codelibLocaleObj.address}`
            break
        case isTGit(type):
            reg = /^git@git(\.tencent)(\.com)[\:|\/](.*)\.git$/
            msg = `${codelibLocaleObj.tgitRule}${type}${codelibLocaleObj.address}`
            break
    }

    const matchResult = path.match(reg)

    return matchResult
        ? {
            alias: matchResult[3]
        }
        : {
            msg
        }
}

export function parsePathRegion (path) {
    const regRegion = /\/\/(.*)(tc-svn|tc-scm|sh-svn([0-9]*)|bj-svn|bj-scm|scm-gy|gz-svn|svn-cd1|group-svn1\.group)\.tencent\.com\//i
    const regionResult = path.match(regRegion)
    let region = ''
    if (regionResult) {
        region = regionResult[2].replace(/(\w+)-(.*)/, '$1').toUpperCase()
        if (regionResult[2] === 'scm-gy') {
            region = 'GY'
        } else if (region === 'SVN') {
            region = 'CD'
        }
    }

    return regionList.includes(region) ? region : ''
}

export function firstUpperCase (str) {
    const codelibLocaleObj = window.devops.$i18n.t('codelib')
    if (typeof str === 'string') {
        return str.slice(0, 1).toUpperCase() + str.slice(1).toLowerCase()
    } else {
        console.warn(`camelCase, ${codelibLocaleObj.paramBeString}`)
    }
}

export function queryStringify (query) {
    return Object.keys(query).map(key => query[key] ? `${key}=${query[key]}` : key).join('&')
}
