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

package com.tencent.devops.process.pojo

/**
 * 构建变量快照。
 *
 * 数据语义：
 *  1. [smallVars]：来源于 T_PIPELINE_BUILD_VAR 的全部变量；
 *     - 对小变量（≤ 4K），其值为真实值；
 *     - 对大变量（> 4K），其值是**纯引用串** `__BK_OVF__:<originalLength>`，
 *       不包含任何真实内容。
 *
 *     保持向后兼容：`$xxx` / `${xxx}` 旧替换逻辑直接看到的是引用串本身，
 *     既不会"显示一半数据"，也不会发生敏感信息泄漏。需要真实值的脚本必须显式
 *     改用 `${{ xxx }}` 表达式语法。
 *
 *  2. [largeKeys]：本次构建中存在溢出的变量名集合（即上面引用串所对应的 keys）。
 *
 *  3. [largeValueLoader]：当 `${{ xxx }}` 表达式访问溢出变量时按需调用。
 *     由 [com.tencent.devops.process.service.BuildVarOverflowLoader] 实例提供，
 *     内置 Caffeine 字符加权 LRU 缓存与"会话级"总加载字节硬上限（可配置）。
 *     历史 build 的变量最大 4K，全部走 [smallVars] 路径，**不会**触发懒加载器。
 *
 * 生命周期：
 *  - 实例**仅适合在一次"表达式求值会话"内重用**（如一次接口调用、一个任务的参数解析）；
 *  - 不要长期持有该对象——它捕获了懒加载器的内部 Caffeine 缓存，长期持有会
 *    阻止大值被 GC 回收。
 */
data class BuildVariableSnapshot(
    val smallVars: Map<String, String>,
    val largeKeys: Set<String>,
    val largeValueLoader: (String) -> String?
) {
    /**
     * 获取变量值。
     *  - 小变量：直接读 [smallVars]；
     *  - 大变量（在 [largeKeys] 中）：触发按需加载。
     *
     * 注意：该方法只供"少量、单点"调用使用，**不要**在循环中遍历全部 [largeKeys] ——
     * 这会等价于一次性把所有大值加载进内存，违背"按需加载"的初衷，
     * 并可能触发 [com.tencent.devops.process.service.BuildVarOverflowBudgetExceededException]。
     */
    fun resolve(key: String): String? {
        if (key in largeKeys) {
            return largeValueLoader.invoke(key)
        }
        return smallVars[key]
    }
}
