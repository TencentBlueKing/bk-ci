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

package com.tencent.bk.codecc.defect.vo.enums;

/**
 * 规则推荐类型
 * 
 * @date 2019/12/26
 * @version V1.0
 */
public enum CheckerRecommendType 
{
    /**
     * 系统默认，取默认规则包
     */
    SYSTEM_DEFAULT("默认"),
    /**
     * 系统推荐，取默认规则包之外的
     */
    SYSTEM_RECOMMEND("推荐"),
    /**
     * 用户自定义，取用户自定义的
     */
    USER_DEFINED("自定义");


    private String name;

    CheckerRecommendType(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return this.name;
    }
}
