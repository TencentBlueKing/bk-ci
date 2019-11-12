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

import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 多工具规则服务层代码
 *
 * @version V1.0
 * @date 2019/4/26
 */
public interface MultitoolCheckerService
{

    /**
     * 多工具查询单个工具的所有规则信息详情
     *
     * @param toolName
     * @return
     */
    Map<String, CheckerDetailVO> queryAllChecker(String toolName);

    /**
     * 查询打开的规则
     * @param codeLang
     * @param toolConfigInfoVO
     * @return
     */
    List<CheckerDetailVO> queryAllChecker(ToolConfigInfoVO toolConfigInfoVO);

    /**
     * 查询打开的规则
     *
     * @param taskInfoEntity
     * @param toolConfigInfoEntity
     * @return
     */
    Map<String, CheckerDetailVO> queryOpenCheckers(ToolConfigInfoVO toolConfigInfoVO);


    /**
     * 多工具查询指定项目单个工具的所有规则信息
     *
     * @param toolName
     * @param taskLang
     * @param paramJson
     * @return
     */
    List<CheckerDetailVO> queryAllChecker(String toolName, String paramJson);


    /**
     * 查询指定规则包的真实规则名（工具可识别的规则名）
     *
     * @param codeLang
     * @param pkgId
     * @param toolConfigInfoVO
     * @return
     */
    Set<String> queryPkgRealCheckers(String pkgId, ToolConfigInfoVO toolConfigInfoVO);


    /**
     * 根据任务id和工具名获取忽略规则
     * @param taskId
     * @param toolName
     * @return
     */
    Boolean mergeIgnoreChecker(long taskId, String toolName, List<String> ignoreCheckers);

}
