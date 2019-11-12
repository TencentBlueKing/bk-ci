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

package com.tencent.bk.codecc.task.service.impl;

import com.tencent.bk.codecc.task.constant.TaskConstants;
import com.tencent.bk.codecc.task.constant.TaskMessageCode;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.service.AbstractTaskRegisterService;
import com.tencent.bk.codecc.task.service.PipelineService;
import com.tencent.bk.codecc.task.service.TaskRegisterService;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.TaskIdVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.exception.StreamException;
import com.tencent.devops.common.auth.api.external.BkAuthExRegisterApi;
import com.tencent.devops.common.web.aop.annotation.OperationHistory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.tencent.devops.common.constant.ComConstants.FUNC_SCAN_SCHEDULE;
import static com.tencent.devops.common.constant.ComConstants.MODIFY_INFO;
import static com.tencent.devops.common.constant.CommonMessageCode.UTIL_EXECUTE_FAIL;

/**
 * 蓝盾任务注册服务实现类
 *
 * @version V1.0
 * @date 2019/5/6
 */
@Service("devopsTaskRegisterService")
public class DevopsTaskRegisterServiceImpl extends AbstractTaskRegisterService
{
    private static Logger logger = LoggerFactory.getLogger(DevopsTaskRegisterServiceImpl.class);

    @Autowired
    @Qualifier("taskRegisterService")
    private TaskRegisterService taskRegisterService;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private BkAuthExRegisterApi bkAuthExRegisterApi;

    @Override
    public TaskIdVO registerTask(TaskDetailVO taskDetailVO, String userName)
    {

        TaskInfoEntity taskInfoEntity = saveTaskInfo(taskDetailVO, userName);
        //将任务注册到权限中心
        bkAuthExRegisterApi.registerCodeCCTask(userName, String.valueOf(taskInfoEntity.getTaskId()),
                taskInfoEntity.getNameEn(), taskInfoEntity.getProjectId());
        return new TaskIdVO(taskInfoEntity.getTaskId(), taskInfoEntity.getNameEn());
    }

    @Override
    public TaskInfoEntity saveTaskInfo(TaskDetailVO taskDetailVO, String userName)
    {
        assembleRegisterParam(taskDetailVO, userName);
        TaskInfoEntity taskInfoEntity = taskRegisterService.saveTaskInfo(taskDetailVO, userName);
        if (null == taskInfoEntity || !StringUtils.isNumeric(String.valueOf(taskInfoEntity.getTaskId())))
        {
            logger.error("save new task info fail! project id: {}, project name: {}", taskDetailVO.getProjectId(),
                    taskDetailVO.getProjectName());
            throw new CodeCCException(TaskMessageCode.REGISTER_TASK_FAIL);
        }
        return taskInfoEntity;
    }

    @Override
    @OperationHistory(funcId = FUNC_SCAN_SCHEDULE, operType = MODIFY_INFO)
    public Boolean modifyTimeAnalysisTask(List<String> executeDate, String executeTime, long taskId, String userName)
    {
        TaskInfoEntity taskInfoEntity = taskRepository.findByTaskId(taskId);
        pipelineService.modifyCodeCCTiming(taskInfoEntity, executeDate, executeTime, userName);
        taskRepository.save(taskInfoEntity);
        return true;
    }

    @Override
    public Boolean updateTaskFromPipeline(TaskDetailVO taskDetailVO, String userName)
    {
        return null;
    }


    /**
     * 组装注册参数
     *
     * @param taskDetailVO
     */
    private void assembleRegisterParam(TaskDetailVO taskDetailVO, String userName)
    {

        //流水线id和中文名不为空表示是通过devops平台创建的codecc代码检查任务，则任务中文名使用"devops_XXX"模板，其中xxx为devops流水线名称
        if (StringUtils.isNotEmpty(taskDetailVO.getPipelineId()) &&
                StringUtils.isNotEmpty(taskDetailVO.getPipelineName()))
        {
            //只有平台创建的才自动生成英文名
            taskDetailVO.setNameEn(getTaskStreamName(taskDetailVO.getProjectId(), taskDetailVO.getPipelineName()));
            String newCnName = handleCnName(taskDetailVO.getPipelineName());
            taskDetailVO.setNameCn(newCnName);
            try
            {
                taskDetailVO.setCodeLang(pipelineService.convertDevopsCodeLangToCodeCC(taskDetailVO.getDevopsCodeLang()));
            }
            catch (StreamException e)
            {
                logger.error("deserialize devops code lang fail! code lang info: {}", taskDetailVO.getDevopsCodeLang());
                throw new CodeCCException(UTIL_EXECUTE_FAIL);
            }
        }
        taskDetailVO.setStatus(TaskConstants.TaskStatus.ENABLE.value());

//        handleDevopsDepartmentInfo(taskDetailVO);

    }

}
