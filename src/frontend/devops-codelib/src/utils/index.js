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

import {
    regionList,
    isTGit,
    isSvn,
    isGitLab,
    isGithub,
    isScmGit,
    isScmSvn
} from '../config'

export function parsePathAlias (type, path, authType, svnType) {
    let reg = ''
    let msg = ''
    let aliasIndex = 3
    const codelibLocaleObj = window.devops.$i18n.t('codelib')

    switch (true) {
        case isGithub(type):
            reg = /^https\:\/\/(github)(\.com)\/([\w\W\.\-\_\/\+]+)\.git$/i
            msg = `${codelibLocaleObj.githubRule}`
            break
        case isSvn(type) && svnType === 'ssh':
            reg = /^svn\+ssh\:\/\/([\@\-\.a-z0-9A-Z]+)(:[0-9]{2,5})?\/([\w\W\.\-\_\/\+]+)$/i
            msg = `${codelibLocaleObj.svnSshRule}${type}${codelibLocaleObj.address}`
            aliasIndex = 3
            break
        case isSvn(type) && svnType === 'http':
            reg = /^(http|https|svn)\:\/\/([\-\.a-z0-9A-Z]+)(:[0-9]{2,5})?\/([\w\W\.\-\_\/\+]+)$/i
            msg = `${codelibLocaleObj.httpRule}${type}${codelibLocaleObj.address}`
            aliasIndex = 4
            break
        case isGitLab(type) && authType === 'HTTP':
            reg = /^https?\:\/\/([\-\.a-z0-9A-Z]+)(:[0-9]{2,5})?\/([\w\W\.\-\_\/\+]+)\.git$/i
            msg = `${codelibLocaleObj.httpOrHttpsRule}`
            break
        case isGitLab(type) && authType === 'SSH':
            reg = /^(git@)([\-\.a-z0-9A-Z]+)\:(.*).git$/i
            msg = `${codelibLocaleObj.gitlabSshRule}`
            break
        case isTGit(type):
            reg = /^https\:\/\/([\-\.a-z0-9A-Z]+)[\:|\/](.*)\.git$/
            msg = `${codelibLocaleObj.tgitHttpRule}`
            aliasIndex = 2
            break
        case isScmSvn(type) && svnType === 'ssh':
            reg = /^svn\+ssh\:\/\/([\@\-\.a-z0-9A-Z]+)(:[0-9]{2,5})?\/([\w\W\.\-\_\/\+]+)$/i
            msg = `${codelibLocaleObj.svnSshRule}${type}${codelibLocaleObj.address}`
            aliasIndex = 3
            break
        case isScmSvn(type) && (svnType === 'http' || svnType === 'OAUTH'):
            reg = /^(http|https|svn)\:\/\/([\-\.a-z0-9A-Z]+)(:[0-9]{2,5})?\/([\w\W\.\-\_\/\+]+)$/i
            msg = `${codelibLocaleObj.httpRule}${type}${codelibLocaleObj.address}`
            aliasIndex = 4
            break
        case isScmGit(type) && (svnType === 'HTTPS' || svnType === 'OAUTH'):
            reg = /^https?\:\/\/([\-\.a-z0-9A-Z]+)(:[0-9]{2,5})?\/([\w\W\.\-\_\/\+]+)\.git$/i
            msg = `${codelibLocaleObj.httpOrHttpsRule}`
            aliasIndex = 3
            break
        case isScmGit(type) && svnType === 'SSH':
            reg = /^(git@)([\-\.a-z0-9A-Z]+)\:(.*).git$/i
            msg = `${codelibLocaleObj.gitlabSshRule}`
            break
    }

    const matchResult = path.match(reg)

    return matchResult
        ? {
            alias: matchResult[aliasIndex]
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

export function getOffset (target) {
    let totalLeft = null
    let totalTop = null
    let par = target.offsetParent
    totalLeft += target.offsetLeft
    totalTop += target.offsetTop
    while (par) {
        if (navigator.userAgent.indexOf('MSIE 8.0') === -1) {
            // 不是IE8我们才进行累加父级参照物的边框
            totalTop += par.clientTop
            totalLeft += par.clientLeft
        }
        totalTop += par.offsetTop
        totalLeft += par.offsetLeft
        par = par.offsetParent
    }
    return { left: totalLeft, top: totalTop }
}

export const prettyDateTimeFormat = (target) => {
    if (!target) {
        return ''
    }
    const formatStr = (str) => {
        if (String(str).length === 1) {
            return `0${str}`
        }
        return str
    }
    const d = new Date(target)
    const year = d.getFullYear()
    const month = formatStr(d.getMonth() + 1)
    const date = formatStr(d.getDate())
    const hours = formatStr(d.getHours())
    const minutes = formatStr(d.getMinutes())
    const seconds = formatStr(d.getSeconds())
    return `${year}-${month}-${date} ${hours}:${minutes}:${seconds}`
}
