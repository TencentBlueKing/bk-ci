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

package com.tencent.devops.common.api;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 刷新工具镜像版本
 *
 * @version V1.0
 * @date 2020/9/7
 */
@Data
public class RefreshDockerImageHashReqVO {
    @NotNull(message = "工具名不能为空")
    @ApiModelProperty(value = "工具名", required = true)
    private String toolName;

    @ApiModelProperty(value = "工具版本")
    private String toolVersion;

    @ApiModelProperty(value = "工具版本类型，T-测试版本，G-灰度版本，P-正式发布版本")
    private String versionType;
}
