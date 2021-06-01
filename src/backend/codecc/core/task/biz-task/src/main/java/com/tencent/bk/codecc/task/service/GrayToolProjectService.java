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

package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.task.vo.TriggerGrayToolVO;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.devops.common.api.pojo.Page;
import com.tencent.bk.codecc.task.vo.GrayToolReportVO;

import java.util.List;
import java.util.Set;

/**
 * 灰度工具项目服务代码
 *
 * @version V1.0
 * @date 2020/12/29
 */
public interface GrayToolProjectService {
    /**
     * 根据项目ID查询信息
     *
     * @param projectId
     * @return
     */
    GrayToolProjectVO findGrayInfoByProjectId(String projectId);


    /**
     * 更新项目灰度信息
     * @param userId
     * @param grayToolProjectVO
     * @return
     */
    Boolean updateGrayInfo(String userId, GrayToolProjectVO grayToolProjectVO);


    /**
     * 查询所有信息
     * @param reqVO
     * @return
     */
    Page<GrayToolProjectVO> queryGrayToolProjectList(GrayToolProjectVO reqVO, Integer pageNum, Integer pageSize,
                                    String sortField, String sortType);

    /**
     * 保存项目灰度信息
     * @param userId
     * @param grayToolProjectVO
     * @return
     */
    Boolean save(String userId, GrayToolProjectVO grayToolProjectVO);

    /**
     * 创建灰度任务池
     * @param toolName
     */
    void selectGrayTaskPool(String toolName, String userName);

    /**
     * 通过工具名获取灰度任务清单
     * @param toolName
     * @return
     */
    Set<Long> findTaskListByToolName(String toolName);

    /**
     * 触发灰度任务池
     * @param toolName
     */
    TriggerGrayToolVO triggerGrayToolTasks(String toolName);

    /**
     * 查询灰度报告
     * @param toolName
     * @param codeccBuildId
     * @return
     */
    GrayToolReportVO findGrayToolReportByToolNameAndBuildId(String toolName, String codeccBuildId);

    /**
     * 根据项目id查找灰度项目
     * @param projectIdSet
     * @return
     */
    List<GrayToolProjectVO> findGrayToolProjectByProjectIds(Set<String> projectIdSet);

}
