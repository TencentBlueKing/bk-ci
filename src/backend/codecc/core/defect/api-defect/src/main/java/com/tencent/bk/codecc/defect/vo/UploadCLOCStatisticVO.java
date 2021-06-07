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

package com.tencent.bk.codecc.defect.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 上传CLOC统计数据视图
 *
 * @version V1.0
 * @date 2019/10/7
 */
@Data
@ApiModel("上传CLOC统计数据视图")
public class UploadCLOCStatisticVO
{
    @ApiModelProperty("任务id")
    @JsonProperty("task_id")
    private Long taskId;

    @ApiModelProperty("流名称")
    @JsonProperty("stream_name")
    private String streamName;

    @JsonProperty("tool_name")
    private String toolName;

    @ApiModelProperty("语言代码量统计")
    private List<CLOCLanguageVO> languageCodeList;

    @ApiModelProperty("语言统计")
    private List<String> languages;
}
