/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

import com.tencent.bk.codecc.defect.vo.CheckerPkgRspVO;
import com.tencent.bk.codecc.defect.vo.ConfigCheckersPkgReqVO;
import com.tencent.bk.codecc.defect.vo.IgnoreCheckerVO;

import java.util.List;

/**
 * 规则包配置业务接口
 *
 * @version V1.0
 * @date 2019/6/5
 */
public interface IConfigCheckerPkgBizService
{

    /**
     * 获取配置规则包
     *
     * @param taskId
     * @param toolName
     * @return
     */
    List<CheckerPkgRspVO> getConfigCheckerPkg(Long taskId, String toolName);


    /**
     * 开启/关闭 规则包配置
     *
     * @param checker
     * @param taskId
     * @return
     */
    Boolean configCheckerPkg(ConfigCheckersPkgReqVO checker, Long taskId);

    /**
     * 新建默认忽略
     * @param ignoreCheckerVO
     * @param userName
     */
    Boolean createDefaultIgnoreChecker(IgnoreCheckerVO ignoreCheckerVO, String userName);

    /**
     * 获取忽略规则信息
     * @param taskId
     * @param toolName
     * @return
     */
    IgnoreCheckerVO getIgnoreCheckerInfo(Long taskId, String toolName);


}
