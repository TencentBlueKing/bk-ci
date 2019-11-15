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

package com.tencent.bk.codecc.task.service;

import com.tencent.bk.codecc.defect.api.ServiceCheckerRestResource;
import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.bk.codecc.defect.vo.IgnoreCheckerVO;
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository;
import com.tencent.bk.codecc.task.dao.mongorepository.ToolRepository;
import com.tencent.bk.codecc.task.dao.mongotemplate.ToolDao;
import com.tencent.bk.codecc.task.model.TaskInfoEntity;
import com.tencent.bk.codecc.task.model.ToolConfigInfoEntity;
import com.tencent.bk.codecc.task.vo.ToolConfigBaseVO;
import com.tencent.bk.codecc.task.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.tencent.devops.common.constant.CommonMessageCode.RECORD_EXIST;

/**
 * 工具管理抽象类
 *
 * @version V1.0
 * @date 2019/4/26
 */
public abstract class AbstractToolService implements ToolService
{
    private static Logger logger = LoggerFactory.getLogger(AbstractTaskRegisterService.class);

    @Autowired
    protected ToolRepository toolRepository;

    @Autowired
    protected TaskRepository taskRepository;

    @Autowired
    protected Client client;

    @Value("${time.analysis.maxhour:#{null}}")
    private String maxHour;

    @Autowired
    private ToolDao toolDao;

    protected void addNewTool2Proj(ToolConfigInfoEntity toolConfigInfoEntity, TaskInfoEntity taskInfoEntity, String user)
    {
        long taskId = toolConfigInfoEntity.getTaskId();
        String toolName = toolConfigInfoEntity.getToolName();

        //校验申请过程中的项目
        String toolNames = taskInfoEntity.getToolNames();
        if (StringUtils.isNotEmpty(toolNames))
        {
            if (toolNames.contains(toolName))
            {
                ToolConfigInfoEntity previousTool = toolRepository.findByTaskIdAndToolName(taskId, toolName);
                if (null != previousTool && previousTool.getCurStep() != -1)
                {
                    logger.error("task [{}] has registered tool before! tool name: {}", taskId, toolName);
                    throw new CodeCCException(RECORD_EXIST, new String[]{toolName}, null);
                }
            }
            else
            {
                toolNames = String.format("%s%s%s", toolNames, ComConstants.TOOL_NAMES_SEPARATOR, toolName);
                List<ToolConfigInfoEntity> toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
                if (null == toolConfigInfoEntityList)
                {
                    toolConfigInfoEntityList = new ArrayList<ToolConfigInfoEntity>()
                    {{
                        add(toolConfigInfoEntity);
                    }};
                    taskInfoEntity.setToolConfigInfoList(toolConfigInfoEntityList);
                }
                else
                {
                    toolConfigInfoEntityList.add(toolConfigInfoEntity);
                }
                taskInfoEntity.setToolNames(toolNames);
            }
        }
        else
        {
            toolNames = toolName;
            taskInfoEntity.setToolNames(toolNames);
            taskInfoEntity.setToolConfigInfoList(new ArrayList<ToolConfigInfoEntity>()
            {{
                add(toolConfigInfoEntity);
            }});
        }
        taskInfoEntity.setUpdatedBy(user);
        taskInfoEntity.setUpdatedDate(System.currentTimeMillis());
    }

    protected void configTaskToolInfo(ToolConfigInfoEntity toolConfigInfoEntity)
    {
        toolConfigInfoEntity.setCurStep(ComConstants.Step4MutliTool.READY.value());
        toolConfigInfoEntity.setStepStatus(ComConstants.StepStatus.SUCC.value());

        logger.info("config task tool info finish! task id: {}, tool name : {}", toolConfigInfoEntity.getTaskId(),
                toolConfigInfoEntity.getToolName());
    }

    /**
     * 更新工具分析步骤及状态
     *
     * @param toolConfigBaseVO
     */
    @Override
    public void updateToolStepStatus(ToolConfigBaseVO toolConfigBaseVO)
    {
        toolDao.updateToolStepStatusByTaskIdAndToolName(toolConfigBaseVO);
    }


    protected String getTaskDefaultTime()
    {
        float time = new Random().nextInt(Integer.valueOf(maxHour) * 2) / 2f;
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, (int) (time * 60));
        String defaultTime = timeFormat.format(cal.getTime());
        logger.info("task default time {}", defaultTime);
        return defaultTime;
    }


    protected List<String> getTaskDefaultReportDate()
    {
        List<String> reportDate = new ArrayList<>();
        reportDate.add(String.valueOf(Calendar.MONDAY));
        reportDate.add(String.valueOf(Calendar.TUESDAY));
        reportDate.add(String.valueOf(Calendar.WEDNESDAY));
        reportDate.add(String.valueOf(Calendar.THURSDAY));
        reportDate.add(String.valueOf(Calendar.FRIDAY));
        reportDate.add(String.valueOf(Calendar.SATURDAY));
        reportDate.add(String.valueOf(Calendar.SUNDAY));
        return reportDate;
    }

    @Override
    public List<String> getEffectiveToolList(long taskId)
    {
        TaskInfoEntity taskInfoEntity = taskRepository.findByTaskId(taskId);
        //获取工具配置实体类清单
        List<ToolConfigInfoEntity> toolConfigInfoEntityList = taskInfoEntity.getToolConfigInfoList();
        if (CollectionUtils.isEmpty(toolConfigInfoEntityList))
        {
            return new ArrayList<>();
        }

        return toolConfigInfoEntityList.stream()
                .filter(toolConfigInfoEntity -> ComConstants.FOLLOW_STATUS.WITHDRAW.value() != toolConfigInfoEntity.getFollowStatus())
                .map(ToolConfigInfoEntity::getToolName)
                .collect(Collectors.toList());
    }


    protected void setDefaultClosedCheckers(ToolConfigInfoEntity toolConfigInfoEntity, String userName)
    {
        ToolConfigInfoVO toolConfigInfoVO = new ToolConfigInfoVO();
        BeanUtils.copyProperties(toolConfigInfoEntity, toolConfigInfoVO, "ignoreCheckers", "checkerProps");

        Result<List<CheckerDetailVO>> allCheckerResult = client.get(ServiceCheckerRestResource.class).queryAllChecker(toolConfigInfoVO);
        if (allCheckerResult.isNotOk() || null == allCheckerResult.getData())
        {
            logger.error("get open checker fail! message: {}", allCheckerResult.getMessage());
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        List<CheckerDetailVO> checkerDetailList = allCheckerResult.getData();
        List<String> ignoreCheckerList = checkerDetailList.stream()
                .filter(checkerDetailEntity -> !ComConstants.CheckerPkgKind.DEFAULT.value().equals(checkerDetailEntity.getPkgKind()))
                .map(CheckerDetailVO::getCheckerKey)
                .collect(Collectors.toList());

        //更新默认屏蔽规则
        IgnoreCheckerVO ignoreCheckerVO = new IgnoreCheckerVO();
        ignoreCheckerVO.setTaskId(toolConfigInfoEntity.getTaskId());
        ignoreCheckerVO.setToolName(toolConfigInfoEntity.getToolName());
        ignoreCheckerVO.setIgnoreList(ignoreCheckerList);
        client.get(ServiceCheckerRestResource.class).createDefaultIgnoreChecker(ignoreCheckerVO, userName);



    }

}
