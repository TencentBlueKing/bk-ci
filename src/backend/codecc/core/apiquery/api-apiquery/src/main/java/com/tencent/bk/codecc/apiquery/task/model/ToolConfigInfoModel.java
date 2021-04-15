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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.codecc.apiquery.defect.model.CommonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工具信息实体类
 *
 * @version V1.0
 * @date 2019/4/24
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ToolConfigInfoModel extends CommonModel {
    /**
     * 配置信息对应的项目ID
     */
    @JsonProperty("task_id")
    private long taskId;

    /**
     * 工具的名称
     */
    @JsonProperty("tool_name")
    private String toolName;

    /**
     * 工具当前任务执行步骤
     */
    @JsonProperty("cur_step")
    private int curStep;

    /**
     * 工具当前任务步骤的状态，成功/失败
     */
    @JsonProperty("step_status")
    private int stepStatus;

    @JsonProperty("param_json")
    private String paramJson;

    /**
     * 跟进状态 对照PREFIX_FOLLOW_STATUS
     */
    @JsonProperty("follow_status")
    private int followStatus;

    /**
     * 上次跟进状态
     */
    @JsonProperty("last_follow_status")
    private int lastFollowStatus;

    /**
     * 工具平台的ip,比如coverity、klocwork工具的platform ip
     */
    @JsonProperty("platform_ip")
    private String platformIp;

    /**
     * 规则集
     */
    @JsonProperty("checker_set")
    private ToolCheckerSetModel checkerSet;

    /**
     * 特殊配置(用于工具侧配置文件中添加个性化属性)
     */
    @JsonProperty("spec_config")
    private String specConfig;

    @JsonProperty("current_build_id")
    private String currentBuildId;
}
