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

package com.tencent.devops.process.utils

/**
 * 大变量"引用协议"工具。
 *
 * 主表 T_PIPELINE_BUILD_VAR.VALUE 长度受限于 varchar(4000)。
 * 当真实值超过该长度时：
 *  - 真实值进入 T_PIPELINE_BUILD_VAR_OVERFLOW（mediumtext，~16M 容量）；
 *  - 主表只保留一条**纯引用**：`__BK_OVF__:<originalLength>`；
 *  - 主表不再保留任何"截断摘要"内容，避免给现网带来无意义的字节占用与
 *    敏感数据泄露风险。
 *
 * 引用串只承载两种信息：
 *  - 是否大变量（是否带哨兵前缀）；
 *  - 原始字符长度（便于 UI/日志/告警显示，无需再二次查询溢出表）。
 *
 * 大变量真实值仅在 ${{ xxx }} 表达式按需求值时通过
 * [com.tencent.devops.process.engine.dao.PipelineBuildVarOverflowDao] 拉取，
 * `$xxx` / `${xxx}` 旧风格固定看到引用串本身。
 */
object BuildVarOverflowUtils {

    /** 主表 VALUE 列字符上限，与 [PIPELINE_VARIABLES_STRING_LENGTH_MAX] 对齐。 */
    const val MAIN_TABLE_MAX_LENGTH: Int = PIPELINE_VARIABLES_STRING_LENGTH_MAX

    /** 哨兵前缀，用于在主表 VALUE 列识别"是否大变量引用"。 */
    const val OVERFLOW_PREFIX: String = PIPELINE_VARIABLES_OVERFLOW_PREFIX

    /** 大变量值硬上限（字符数），超过则拒绝写入。 */
    const val HARD_MAX_LENGTH: Int = PIPELINE_VARIABLES_STRING_LENGTH_HARD_MAX

    /**
     * 判断主表 VALUE 是否为"大变量引用"。
     *
     * 用户主动构造该前缀的概率极低；即使发生，按需加载时溢出表查询为 null，
     * `$xxx`/`${xxx}` 风格仍直接看到这串字符，行为退化为"与改造前一致"。
     */
    fun isOverflowReference(value: String?): Boolean {
        return value != null && value.startsWith(OVERFLOW_PREFIX)
    }

    /**
     * 计算主表 VALUE 列实际写入的字符串。
     *  - rawValue 长度 ≤ [MAIN_TABLE_MAX_LENGTH]：直接返回原值；
     *  - 否则：返回纯引用串 `__BK_OVF__:<rawValue.length>`，
     *    其最大长度 = OVERFLOW_PREFIX.length + 10（Int 最大 10 位）≈ 25，
     *    主表 varchar(4000) 列恒能容纳。
     */
    fun toMainTableValue(rawValue: String): String {
        return if (rawValue.length <= MAIN_TABLE_MAX_LENGTH) rawValue else "$OVERFLOW_PREFIX${rawValue.length}"
    }

    /** 是否需要将真实值写入溢出表。 */
    fun shouldOverflow(rawValue: String): Boolean = rawValue.length > MAIN_TABLE_MAX_LENGTH
}
