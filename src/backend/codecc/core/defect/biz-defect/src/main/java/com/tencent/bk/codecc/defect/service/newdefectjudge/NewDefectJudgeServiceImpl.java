package com.tencent.bk.codecc.defect.service.newdefectjudge;

import com.google.common.collect.Maps;
import com.tencent.bk.codecc.defect.dao.mongorepository.FirstAnalysisSuccessTimeRepository;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.client.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

/**
 * 描述
 *
 * @version V1.0
 * @date 2019/12/6
 */
@Slf4j
@Service
public class NewDefectJudgeServiceImpl implements NewDefectJudgeService
{
    @Autowired
    private FirstAnalysisSuccessTimeRepository firstSuccessTimeRepository;

    @Autowired
    private Client client;

    /**
     * 获取新告警判定时间
     *
     * @param taskId
     * @param toolName
     * @return
     */
    @Override
    public long getNewDefectJudgeTime(long taskId, String toolName, TaskDetailVO taskDetailVO)
    {
//        long result = 0;
//        NewDefectJudgeVO newDefectJudge;
//        if(null == taskDetailVO)
//        {
//            // 获取告警转为历史的时间
//            Result<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
//            if (taskInfoResult.isNotOk() || null == taskInfoResult.getData())
//            {
//                log.error("get task info fail! task id is: {}, msg: {}", taskId, taskInfoResult.getMessage());
//                throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
//            }
//            newDefectJudge = taskInfoResult.getData().getNewDefectJudge();
//        }
//        else
//        {
//            newDefectJudge = taskDetailVO.getNewDefectJudge();
//        }
//        if (newDefectJudge != null)
//        {
//            if (newDefectJudge.getFromDateTime() != null && newDefectJudge.getFromDateTime() != 0)
//            {
//                result = newDefectJudge.getFromDateTime();
//            }
//            else if (StringUtils.isNotEmpty(newDefectJudge.getFromDate()))
//            {
//                result = DateTimeUtils.convertStringDateToLongTime(newDefectJudge.getFromDate(), DateTimeUtils.yyyyMMddFormat);
//            }
//        }
//
//        // 获取首次成功分析时间
//        if (result == 0)
//        {
//            FirstAnalysisSuccessEntity firstSuccessTimeEntity = firstSuccessTimeRepository.findFirstByTaskIdAndToolName(taskId, toolName);
//            if (firstSuccessTimeEntity != null)
//            {
//                result = firstSuccessTimeEntity.getFirstAnalysisSuccessTime();
//            }
//        }
//        return result;

        return 0;
    }

    @Override
    public long getNewDefectJudgeTime(long taskId, TaskDetailVO taskDetailVO) {
        return 0;
    }

    @Override
    public Map<Long, Long> batchGetNewDefectJudgeTime(Collection<Long> taskIdList, String toolName)
    {
        Map<Long, Long> taskTimeMap = Maps.newHashMap();
//        if (CollectionUtils.isEmpty(taskIdList) || StringUtils.isEmpty(toolName))
//        {
//            return taskTimeMap;
//        }
//
//        List<FirstAnalysisSuccessEntity> successEntityList =
//                firstSuccessTimeRepository.findByTaskIdInAndToolNameIs(taskIdList, toolName);
//        if (CollectionUtils.isNotEmpty(successEntityList))
//        {
//            taskTimeMap = successEntityList.stream().collect(Collectors
//                    .toMap(FirstAnalysisSuccessEntity::getTaskId,
//                            FirstAnalysisSuccessEntity::getFirstAnalysisSuccessTime));
//
//        }
        return taskTimeMap;
    }
}
