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

package com.tencent.devops.environment.pojo.enums

/**
 * 节点的操作人合规状态。
 * 当节点导入人不为节点的主备份负责人时，节点的操作人状态置为 `OPERATOR_CHANGED`。
 *   （通常由 CMDB / CC 同步任务在每个 cron 周期内重新计算后回写到 `T_NODE.OPERATOR_STATUS`）。
 *
 * 持久化为 `T_NODE.OPERATOR_STATUS tinyint` 列：[code] 与列值一一对应，
 * 因此新增枚举值时请勿改动已有 `code`。
 */
@Suppress("UNUSED")
enum class NodeOperatorStatus(val code: Byte) {
    /** 正常情况，操作人是节点的主备份负责人。 */
    NORMAL(0),

    /** 负责人已变更，即节点导入人不是节点的主备份负责人。 */
    OPERATOR_CHANGED(1);

    companion object {
        /**
         * 按 DB 列值反查枚举：
         * - `code == null`（DB 列暂无值 / 同步任务未回写）→ 返回 `null`，由调用方决定是否透传给前端。
         * - 未识别的取值 → 抛 [IllegalArgumentException]：列值是同步任务自己写入的，
         *   出现未识别值意味着写入逻辑或 DB 数据被脏污染，应及时暴露而非用 [NORMAL] 掩盖。
         */
        fun valOf(code: Byte?): NodeOperatorStatus? {
            if (code == null) return null
            values().forEach {
                if (it.code == code) return it
            }
            throw IllegalArgumentException("No NodeOperatorStatus enum for code=$code")
        }
    }
}
