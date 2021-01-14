/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.tencent.bk.codecc.task.pojo;

import lombok.Data;

/**
 * 工具基本信息模型
 * 
 * @date 2019/11/18
 * @version V1.0
 */
@Data
public class ToolBaseInfoModel 
{

    /**
     * 工具模型,LINT、COMPILE、TSCLUA、CCN、DUPC，决定了工具的接入、告警、报表的处理及展示类型
     */
    private String pattern;

    /**
     * 工具名称，也是唯一KEY
     */
    private String name;

    /**
     * 工具的展示名
     */
    private String displayName;

    /**
     * 工具类型，界面上展示工具归类：
     * 发现缺陷和安全漏洞、规范代码、复杂度、重复代码
     */
    private String type;

    /**
     * 支持语言，通过位运算的值表示
     */
    private Long lang;

    /**
     * 根据项目语言来判断是否推荐该款工具,true表示推荐，false表示不推荐
     */
    private Boolean recommend;

    /**
     * 状态：测试（T）、灰度（保留字段）、发布（P）、下架， 注：测试类工具只有管理员可以在页面上看到，只有管理员可以接入
     */
    private String status;

    /**
     * 工具的个性参数，如pylint的Python版本，这个参数用json保存。
     * 用户在界面上新增参数，填写参数名，参数变量， 类型（单选、复选、下拉框等），枚举值
     */
    private String params;
}
