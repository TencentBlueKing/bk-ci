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

package com.tencent.bk.codecc.defect.vo.admin;

import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;

/**
 * 任务分析记录请求对象
 *
 * @version V1.0
 * @date 2020/12/18
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskLogOverviewReqVO extends TaskLogOverviewVO {

    @ApiModelProperty("分析状态 enum ScanStatus")
    private Integer status;

    @ApiModelProperty("任务ID集合")
    private Collection<Long> taskIds;

}
