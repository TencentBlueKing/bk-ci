/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved. BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
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

package com.tencent.devops.common.db.utils

import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL

/**
 * 租户列的动态 jOOQ 字段。
 * 开发库尚未跑增量 SQL 时，codegen 不会生成 TENANT_ID，DAO 仍需能编译。
 */
object TenantTableFields {
    /** T_PROJECT 使用小写列名 */
    val PROJECT_TENANT_ID: Field<String> = DSL.field(DSL.name("tenant_id"), String::class.java)
    val PROJECT_TENANT_ENGLISH_NAME: Field<String> =
        DSL.field(DSL.name("tenant_english_name"), String::class.java)

    /** T_PROJECT_APPROVAL / T_ATOM / T_TEMPLATE / T_IMAGE 使用大写列名 */
    val TENANT_ID: Field<String> = DSL.field(DSL.name("TENANT_ID"), String::class.java)
    val TENANT_ENGLISH_NAME: Field<String> = DSL.field(DSL.name("TENANT_ENGLISH_NAME"), String::class.java)
}

fun Record.optionalTenantId(): String? {
    val existing = field("TENANT_ID") ?: field("tenant_id") ?: return null
    return get(existing) as? String
}
