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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.stream.pojo.enums

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum

/*
    工蜂接口返回状态码枚举
 */

enum class GitCodeApiStatus(
    val status: Int,
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, keyPrefixName = "gitCodeApiStatus", reusePrefixFlag = false)
    val content: String
    ) {
    OK(200, "ok"),//操作成功
    CREATED(201, "created"),//创建成功
    BAD_REQUEST(400, "badRequest"),//参数错误，或是参数格式错误
    UNAUTHORIZED(401, "unauthorized"),//认证失败
    FORBIDDEN(403, "forbidden"),//帐号并没有该操作的权限或者项目设置不允许该操作
    NOT_FOUND(404, "notFound"),//资源不存在，也可能是帐号没有该项目的权限（为防止黑客撞库获取库列表）
    METHOD_NOT_ALLOWED(405, "methodNotAllowed"),//没有该接口
    CONFLICT(409, "conflict"),//与已存在的对象/内容冲突或者操作行为与规则相冲突
    UNPROCESSABLE(422, "unprocessable"),//操作不能进行
    LOCKED(423, "locked"),//帐号被锁定，或api请求频率超限
    TOO_MANY_REQUESTS(429, "tooManyRequests"),//请求被限流
    SERVER_ERROR(500, "serverError");//服务器出错

    companion object {
        fun getStatus(status: Int): GitCodeApiStatus? {
            val codes = values().associateBy { it.status }
            if (status !in codes.keys) {
                return null
            }
            return codes[status]
        }
    }
}
