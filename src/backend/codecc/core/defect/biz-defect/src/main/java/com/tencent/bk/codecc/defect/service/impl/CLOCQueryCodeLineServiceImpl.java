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
 
package com.tencent.bk.codecc.defect.service.impl;

import com.tencent.bk.codecc.defect.dao.mongotemplate.CLOCDefectDao;
import com.tencent.bk.codecc.defect.dto.CodeLineModel;
import com.tencent.bk.codecc.defect.service.ICLOCQueryCodeLineService;
import com.tencent.bk.codecc.defect.vo.ToolClocRspVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * cloc查询代码行数服务
 * 
 * @date 2020/3/31
 * @version V1.0
 */
@Service
public class CLOCQueryCodeLineServiceImpl implements ICLOCQueryCodeLineService
{
    @Autowired
    private CLOCDefectDao clocDefectDao;

    @Override
    public ToolClocRspVO getCodeLineInfo(Long taskId)
    {
        List<CodeLineModel> codeLineModelList = clocDefectDao.getCodeLineInfo(taskId);
        ToolClocRspVO toolClocRspVO = new ToolClocRspVO();
        toolClocRspVO.setTaskId(taskId);
        toolClocRspVO.setCodeLineList(codeLineModelList);
        return toolClocRspVO;
    }
}
