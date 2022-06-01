/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.model

import com.tencent.bkrepo.auth.pojo.token.Token
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * 用户
 */
@Document("user")
@CompoundIndexes(
    CompoundIndex(name = "userId_idx", def = "{'userId': 1}", unique = true, background = true),
    CompoundIndex(name = "tokens_iid_idx", def = "{'tokens._id': 1}", background = true),
    CompoundIndex(name = "roles_idx", def = "{'roles': 1}", background = true),
    CompoundIndex(name = "group_idx", def = "{'group': 1}", background = true),
    CompoundIndex(name = "asstUsers_idx", def = "{'asstUsers': 1}", background = true)
)
data class TUser(
    val userId: String,
    val name: String,
    val pwd: String,
    val admin: Boolean = false,
    val locked: Boolean = false,
    val tokens: List<Token> = emptyList(),
    val roles: List<String> = emptyList(),
    val asstUsers: List<String> = emptyList(),
    val group: Boolean = false,
    val email: String? = null,
    val phone: String? = null,
    var accounts: List<String>? = emptyList(),
    val createdDate: LocalDateTime? = LocalDateTime.now(),
    val lastModifiedDate: LocalDateTime? = LocalDateTime.now()
)
