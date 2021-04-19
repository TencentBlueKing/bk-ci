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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to
 * use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.service.GrayToolProjectService;
import com.tencent.bk.codecc.task.vo.GrayToolProjectVO;
import com.tencent.bk.codecc.task.vo.GrayToolReportVO;
import com.tencent.bk.codecc.task.vo.TriggerGrayToolVO;
import com.tencent.devops.common.api.pojo.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 灰度工具项目服务代码
 *
 * @version V1.0
 * @date 2020/12/29
 */
@Slf4j
@Service
public class GrayToolProjectServiceImpl implements GrayToolProjectService {

    @Override
    public GrayToolProjectVO findGrayInfoByProjectId(String projectId) {
        // TODO("not implemented")
        GrayToolProjectVO mockResult = new GrayToolProjectVO();
        mockResult.setProjectId(projectId);
        mockResult.setOpenSourceProject(false);
        return mockResult;
    }

    @Override
    public Boolean updateGrayInfo(String userId, GrayToolProjectVO grayToolProjectVO) {
        // TODO("not implemented")
        return null;
    }

    @Override
    public Page<GrayToolProjectVO> queryGrayToolProjectList(GrayToolProjectVO reqVO, Integer pageNum,
                                                            Integer pageSize, String sortField, String sortType) {
        // TODO("not implemented")
        return null;
    }

    @Override
    public Boolean save(String userId, GrayToolProjectVO grayToolProjectVO) {
        // TODO("not implemented")
        return null;
    }

    @Override
    public void selectGrayTaskPool(String toolName, String userName) {
        // TODO("not implemented")
    }

    @Override
    public Set<Long> findTaskListByToolName(String toolName) {
        // TODO("not implemented")
        return null;
    }

    @Override
    public TriggerGrayToolVO triggerGrayToolTasks(String toolName) {
        // TODO("not implemented")
        return null;
    }

    @Override
    public GrayToolReportVO findGrayToolReportByToolNameAndBuildId(String toolName, String codeccBuildId) {
        // TODO("not implemented")
        return null;
    }

    @Override
    public List<GrayToolProjectVO> findGrayToolProjectByProjectIds(Set<String> projectIdSet) {
        // TODO("not implemented")
        return null;
    }
}
