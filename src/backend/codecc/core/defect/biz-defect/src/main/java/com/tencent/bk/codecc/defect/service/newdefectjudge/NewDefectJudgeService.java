package com.tencent.bk.codecc.defect.service.newdefectjudge;

import com.tencent.bk.codecc.task.vo.TaskDetailVO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 新告警判定服务
 *
 * @version V1.0
 * @date 2019/12/6
 */
public interface NewDefectJudgeService
{
    /**
     * 获取新告警判定时间
     *
     * @param taskId
     * @param toolName
     * @return
     */
    long getNewDefectJudgeTime(long taskId, String toolName, TaskDetailVO taskDetailVO);

    /**
     * 获取新告警判定时间
     *
     * @param taskId
     * @return
     */
    long getNewDefectJudgeTime(long taskId, TaskDetailVO taskDetailVO);


    /**
     * 批量获取任务新告警判定时间
     *
     * @param taskIdList 任务ID集合
     * @param toolName   工具名
     * @return list
     */
    Map<Long, Long> batchGetNewDefectJudgeTime(Collection<Long> taskIdList, String toolName);
}
