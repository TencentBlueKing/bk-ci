/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2026 THL A29 Limited, a Tencent company.  All rights reserved.
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

/**
 * 解析权限中心默认选中的项目
 * 优先级：URL 指定项目 > 用户当前访问项目（Cookie X-DEVOPS-PROJECT-ID）> 用户上次选择（localStorage 缓存）> 兜底值
 * @param {Object} options
 * @param {string} [options.routeProjectCode] - 路由（params/query）中显式指定的项目
 * @param {string} [options.cookieProjectCode] - Cookie X-DEVOPS-PROJECT-ID，用户当前访问的项目
 * @param {string} [options.cachedProjectCode] - localStorage 缓存的项目，用户上次选择
 * @param {string[]} [options.projectCodes] - 用户可访问的项目列表；提供时对 cookie/缓存候选做成员校验（URL 指定值不校验）
 * @param {string} [options.fallbackProjectCode] - 全部候选无效时的兜底值（如项目列表第一项）
 * @returns {string} 默认选中的项目 code
 */
export function resolveDefaultProjectCode({
    routeProjectCode = '',
    cookieProjectCode = '',
    cachedProjectCode = '',
    projectCodes,
    fallbackProjectCode = '',
} = {}) {
    if (routeProjectCode) return routeProjectCode;

    const candidates = [cookieProjectCode, cachedProjectCode];
    for (const code of candidates) {
        if (code && (!projectCodes || projectCodes.includes(code))) {
            return code;
        }
    }
    return fallbackProjectCode;
}

export default resolveDefaultProjectCode;
