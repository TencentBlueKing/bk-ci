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

package com.tencent.bk.codecc.apiquery.task.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 工具信息实体类
 *
 * @version V1.0
 * @date 2019/4/24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "t_tool_config")
@CompoundIndexes({
        @CompoundIndex(name = "task_id_1_tool_name_1", def = "{'task_id': 1, 'tool_name': 1}")
})
public class ToolConfigInfoEntity extends CommonEntity
{
    /**
     * 配置信息对应的项目ID
     */
    @Indexed
    @Field("task_id")
    private long taskId;

    /**
     * 工具的名称
     */
    @Indexed
    @Field("tool_name")
    private String toolName;

    /**
     * 工具当前任务执行步骤
     */
    @Field("cur_step")
    private int curStep;

    /**
     * 工具当前任务步骤的状态，成功/失败
     */
    @Field("step_status")
    private int stepStatus;

    @Field("param_json")
    private String paramJson;

    /**
     * 跟进状态 对照PREFIX_FOLLOW_STATUS
     */
    @Field("follow_status")
    private int followStatus;

    /**
     * 上次跟进状态
     */
    @Field("last_follow_status")
    private int lastFollowStatus;

    /**
     * 工具平台的ip,比如coverity、klocwork工具的platform ip
     */
    @Field("platform_ip")
    private String platformIp;

    /**
     * 规则集
     */
    @Field("checker_set")
    private ToolCheckerSetEntity checkerSet;

    /**
     * 特殊配置(用于工具侧配置文件中添加个性化属性)
     */
    @Field("spec_config")
    private String specConfig;

    @Field("current_build_id")
    private String currentBuildId;
}
