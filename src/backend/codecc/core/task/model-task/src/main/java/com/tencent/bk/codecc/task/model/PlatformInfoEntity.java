/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

/**
 * Coverity Platform的信息
 *
 * @version V2.0
 * @date 2019/9/30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_platform_info")
@CompoundIndexes(
        @CompoundIndex(name = "tool_name_1_ip_1", def = "{'tool_name': 1, 'ip': 1}")
)
public class PlatformInfoEntity extends CommonEntity
{
    @Indexed
    @Field("tool_name")
    private String toolName;

    @Indexed
    @Field("ip")
    private String ip;

    @Field("port")
    private String port;

    @Field("user_name")
    private String userName;

    @Field("passwd")
    private String passwd;

    @Field("token")
    private String token;

    /**
     * 0-启用，1-停用
     */
    @Field("status")
    private Integer status;

    @Field("owner")
    private String owner;

    /**
     * 支持的任务类型，当前主要用来区分是否支持开源扫描的
     */
    @Field("support_task_types")
    private Set<String> supportTaskTypes;
}
