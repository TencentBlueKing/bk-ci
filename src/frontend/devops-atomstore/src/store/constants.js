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

export const UPDATE_CURRENT_ATOM = 'UPDATE_CURRENT_ATOM'
export const UPDATE_CURRENT_TEMPLATE = 'UPDATE_CURRENT_TEMPLATE'
export const UPDATE_CURRENT_IMAGE = 'UPDATE_CURRENT_IMAGE'
export const UPDATE_CURRENT_SERVICE = 'UPDATE_CURRENT_SERVICE'
export const UPDATE_CURRENT_LIST = 'UPDATE_CURRENT_LIST'
export const UPDATE_MARKET_QUERY = 'UPDATE_MARKET_QUERY'
export const UPDATE_MARKET_DETAIL = 'UPDATE_MARKET_DETAIL'
export const CLEAR_MARKET_DETAIL = 'CLEAR_MARKET_DETAIL'
export const UPDATE_USER_INFO = 'UPDATE_USER_INFO'

export const atomStatusMap = {
    INIT: 'store.初始化',
    COMMITTING: 'store.提交中',
    BUILDING: 'store.构建中',
    BUILD_FAIL: 'store.构建失败',
    TESTING: 'store.测试中',
    AUDITING: 'store.审核中',
    AUDIT_REJECT: 'store.审核驳回',
    RELEASED: 'store.已发布',
    GROUNDING_SUSPENSION: 'store.上架中止',
    UNDERCARRIAGING: 'store.下架中',
    UNDERCARRIAGED: 'store.已下架',
    CODECCING: 'store.代码检查中',
    CODECC_FAIL: 'store.代码检查失败'
}
export const templateStatusList = {
    INIT: 'store.初始化',
    AUDITING: 'store.审核中',
    AUDIT_REJECT: 'store.审核驳回',
    RELEASED: 'store.已发布',
    GROUNDING_SUSPENSION: 'store.上架中止',
    UNDERCARRIAGED: 'store.已下架'
}

export const imageStatusList = {
    INIT: 'store.初始化',
    COMMITTING: 'store.提交中',
    CHECKING: 'store.验证中',
    CHECK_FAIL: 'store.验证失败',
    TESTING: 'store.测试中',
    AUDITING: 'store.审核中',
    AUDIT_REJECT: 'store.审核驳回',
    RELEASED: 'store.已发布',
    GROUNDING_SUSPENSION: 'store.上架中止',
    UNDERCARRIAGING: 'store.下架中',
    UNDERCARRIAGED: 'store.已下架'
}

export const serviceStatusMap = {
    INIT: 'store.初始化',
    COMMITTING: 'store.提交中',
    BUILDING: 'store.构建中',
    BUILD_FAIL: 'store.构建失败',
    TESTING: 'store.测试中',
    EDIT: 'store.填写相关信息中',
    AUDITING: 'store.审核中',
    AUDIT_REJECT: 'store.审核驳回',
    RELEASE_DEPLOYING: 'store.正式发布部署中',
    RELEASE_DEPLOY_FAIL: 'store.正式发布部署失败',
    RELEASED: 'store.已发布',
    GROUNDING_SUSPENSION: 'store.上架中止',
    UNDERCARRIAGING: 'store.下架中',
    UNDERCARRIAGED: 'store.已下架'
}
