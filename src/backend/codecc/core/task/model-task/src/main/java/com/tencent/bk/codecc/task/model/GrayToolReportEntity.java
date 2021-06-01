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
 
package com.tencent.bk.codecc.task.model;

import com.tencent.codecc.common.db.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * 灰度报告表
 * 
 * @date 2021/1/7
 * @version V1.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "t_gray_tool_report")
@CompoundIndexes({
        @CompoundIndex(name = "project_id_1_codecc_build_id_1", def = "{'project_id': 1, 'codecc_build_id': 1}",
                background = true)
})
public class GrayToolReportEntity extends CommonEntity
{
    //蓝盾项目id
    @Field("project_id")
    private String projectId;
    //工具名
    @Field("tool_name")
    private String toolName;
    //统一下发生成的唯一id
    @Field("codecc_build_id")
    @Indexed(background = true)
    private String codeccBuildId;
    //上一次报告信息
    @Field("last_report_info")
    private GrayToolReportSubEntity lastReportInfo;
    //本次报告信息
    @Field("current_report_info")
    private GrayToolReportSubEntity currentReportInfo;
    //本次扫描出告警的任务清单
    @Field("defect_task_list")
    private List<GrayDefectTaskSubEntity> defectTaskList;
}
