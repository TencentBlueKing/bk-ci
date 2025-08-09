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

package com.tencent.devops.process.constant

object ProcessTemplateMessageCode {
    const val ERROR_TEMPLATE_INSTANCE_NOT_EXISTS = "2101281"
    const val ERROR_PIPELINE_TRIGGER_CONFIG_STEP_ID_NOT_FOUND = "2101282"
    const val ERROR_TEMPLATE_PATH_REF_PIPELINE_NEED_PAC = "2101283" // 路径引用流水线必须开启PAC
    const val ERROR_TEMPLATE_INSTANCE_NEED_PIPELINE_TYPE = "2101284" // 实例化仅支持流水线类型
    const val ERROR_TEMPLATE_PATH_REF_TEMPLATE_NEED_PAC = "2101285" // 模版没有开启PAC不能使用路径引用
    const val ERROR_TEMPLATE_PATH_REF_TEMPLATE_REF_NOT_EMPTY = "2101286" // 路径引用,模板分支不能为空
    const val ERROR_TEMPLATE_TYPE_MODEL_TYPE_NOT_MATCH = "2101287" // 模版类型和编排类型不匹配

    const val ERROR_PIPELINE_RELEASE_MUST_DRAFT_VERSION = "2101290" // 发布流水线时必须使用草稿版本
    const val ERROR_PIPELINE_BASE_VERSION_NOT_FOUND = "2101292" // 未找到基线版本
    const val ERROR_TEMPLATE_YAML_REPOSITORY_NOT_FOUND = "2101293" // 模板YAML文件所属的代码仓库不存在
    const val ERROR_YAML_FOR_TEMPLATE_NOT_FOUND = "2101294" // YAML文件$0对应的模板不存在
    const val ERROR_TEMPLATE_YAML_VERSION_NOT_FOUND = "2101295" // 分支$0下的文件$1对应的模版版本不存在
    const val ERROR_TEMPLATE_VERSION_BY_ID_NOT_FOUND = "2101296" // 模版$0对应的版本$1不存在
    const val ERROR_TEMPLATE_VERSION_BY_PATH_NOT_FOUND = "2101297" // 模版$0在分支$1对应的版本不存在
    const val ERROR_TEMPLATE_REF_TYPE = "2101298" // 模版引用templateId和templatePath不能同时为空
    const val ERROR_TEMPLATE_VERSION_NAME_NOT_EMPTY = "2101299" // 模版版本名称不能为空
    const val ERROR_TEMPLATE_VERSION_NOT_FOUND = "21012300" // 模版$0对应的版本$1不存在
    const val ERROR_TEMPLATE_NOT_ENABLE_PAC = "2101301" // 模版没有开启PAC,不能通过路径引用查询

    const val ERROR_STATUS_NOT_MATCHED = "2101302" // 状态不匹配,预期状态为$0,实际状态为$1

    const val ERROR_PIPELINE_NOT_RELATED_TEMPLATE = "2101303" // 流水线没有关联模板

    const val ERROR_TEMPLATE_LATEST_VERSION_NOT_PUBLISHED = "2101304" // 模板{0}的最新状态处于非发布状态
    const val ERROR_TEMPLATE_TYPE_INVALID = "2101305" // 无效的模板类型
    const val ERROR_RECENTLY_INSTALL_TEMPLATE_NOT_EXIST = "2101306" // 模板{0}的最近安装记录不存在
    const val ERROR_LATEST_PUBLISHED_TEMPLATE_NOT_EXIST = "2101307" // 模板{0}的最新发布版本记录不存在
    const val ERROR_TEMPLATE_TRANSFORM_TO_CUSTOM = "2101308" // 模板处于非约束状态，不允许转化自定义
    const val ERROR_TEMPLATE_SETTING_NOT_EXISTS = "2101309" // 模板设置不存在
}
