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

package com.tencent.bk.codecc.apiquery.service;


import com.tencent.bk.codecc.apiquery.vo.ToolConfigPlatformVO;
import com.tencent.devops.common.api.pojo.Page;


/**
 * 工具管理服务接口
 *
 * @version V1.0
 * @date 2020/4/24
 */
public interface ToolService
{

    /**
     * 获取工具platform配置信息列表
     *
     * @param taskId     任务ID
     * @param toolName   工具名
     * @param platformIp IP
     * @return list
     */
    Page<ToolConfigPlatformVO> getPlatformInfoList(Long taskId, String toolName, String platformIp, Integer pageNum,
            Integer pageSize, String sortType);

    /**
     * 获取任务Platform详情信息
     *
     * @param taskId     任务ID
     * @param toolName   工具名
     * @return vo
     */
    ToolConfigPlatformVO getTaskPlatformDetail(Long taskId, String toolName);
}
