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

package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.CheckerCommonCountVO;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.CheckerListQueryReq;
import com.tencent.bk.codecc.defect.vo.enums.CheckerListSortType;
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 多工具规则服务层代码
 *
 * @version V1.0
 * @date 2019/4/26
 */
public interface CheckerService
{

    /**
     * 多工具查询单个工具的所有规则信息详情
     *
     * @param toolName
     * @return
     */
    Map<String, CheckerDetailVO> queryAllChecker(String toolName);

    /**
     * 多工具查询单个工具的所有规则信息详情
     *
     * @param toolNameSet
     * @return
     */
    Map<String, CheckerDetailVO> queryAllChecker(List<String> toolNameSet, String checkerSet);

    /**
     * 查询打开的规则
     *
     * @param taskId
     * @param toolName
     * @param paramJson
     * @param codeLang
     * @return
     */
    List<CheckerDetailVO> queryAllChecker(long taskId, String toolName, String paramJson, long codeLang);

    /**
     * 根据工具查询对应的规则列表
     *
     * @param toolName
     * @return
     */
    List<CheckerDetailVO> queryCheckerByTool(String toolName);

    /**
     * 查询打开的规则
     *
     * @param taskId
     * @param toolName
     * @return
     */
    Map<String, CheckerDetailVO> queryOpenCheckers(long taskId, String toolName, String paramJson, long codeLang);


    /**
     * 查询指定规则包的真实规则名（工具可识别的规则名）
     *
     * @param pkgId
     * @param taskDetailVO
     * @return
     */
    Set<String> queryPkgRealCheckers(String pkgId, String toolName, TaskDetailVO taskDetailVO);

    /**
     * 查询指定规则包的真实规则名（工具可识别的规则名）
     *
     * @param pkgId
     * @param taskDetailVO
     * @return
     */
    Set<String> queryPkgRealCheckers(String pkgId, List<String> toolNameSet, TaskDetailVO taskDetailVO);

    /**
     * 更新规则配置参数
     *
     * @param toolName
     * @param checkerName
     * @param paramValue
     * @param user
     * @return
     */
    boolean updateCheckerConfigParam(Long taskId, String toolName, String checkerName, String paramValue, String user);

    /**
     * 查询规则详情
     *
     * @param toolName
     * @param checkerKey
     * @return
     */
    CheckerDetailVO queryCheckerDetail(String toolName, String checkerKey);


    /**
     * 根据条件查询规则详情
     *
     * @param checkerListQueryReq
     * @return
     */
    List<CheckerDetailVO> queryCheckerDetailList(CheckerListQueryReq checkerListQueryReq, String projectId, Integer pageNum,
                                                 Integer pageSize, Sort.Direction sortType, CheckerListSortType sortField);

    /**
     * 查询规则相应数量集
     *
     * @param checkerListQueryReq
     * @return
     */
    List<CheckerCommonCountVO> queryCheckerCountList(CheckerListQueryReq checkerListQueryReq);

    /**
     * 查询规则相应数量集（新）
     *
     * @param checkerListQueryReq
     * @return
     */
    List<CheckerCommonCountVO> queryCheckerCountListNew(CheckerListQueryReq checkerListQueryReq, String projectId);

    /**
     * 查询任务的规则配置
     *
     * @param analyzeConfigInfoVO
     * @return
     */
    AnalyzeConfigInfoVO getTaskCheckerConfig(AnalyzeConfigInfoVO analyzeConfigInfoVO);

    /**
     * 查询任务配置的圈复杂度阀值
     *
     * @param toolConfigInfoVO 工具配置信息
     */
    int getCcnThreshold(ToolConfigInfoVO toolConfigInfoVO);

    /**
     * 根据checkerKey和ToolName更新规则详情
     *
     * @param checkerDetailVO
     * @return
     */
    boolean updateCheckerByCheckerKey(CheckerDetailVO checkerDetailVO);
}
