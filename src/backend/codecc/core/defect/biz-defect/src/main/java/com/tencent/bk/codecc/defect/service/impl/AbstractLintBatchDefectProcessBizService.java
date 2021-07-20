package com.tencent.bk.codecc.defect.service.impl;

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.dao.mongorepository.BuildDefectRepository;
import com.tencent.bk.codecc.defect.dao.mongorepository.LintDefectV2Repository;
import com.tencent.bk.codecc.defect.dao.mongotemplate.LintDefectV2Dao;
import com.tencent.bk.codecc.defect.model.BuildDefectEntity;
import com.tencent.bk.codecc.defect.model.LintDefectV2Entity;
import com.tencent.bk.codecc.defect.service.AbstractBatchDefectProcessBizService;
import com.tencent.bk.codecc.defect.service.CheckerService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.service.newdefectjudge.NewDefectJudgeService;
import com.tencent.bk.codecc.defect.utils.ParamUtils;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.TaskLogVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Lint类工具告警批量处理抽象类
 *
 * @version V1.0
 * @date 2020/3/3
 */
@Slf4j
public abstract class AbstractLintBatchDefectProcessBizService extends AbstractBatchDefectProcessBizService
{
    @Autowired
    private Client client;
    @Autowired
    private LintDefectV2Repository lintDefectV2Repository;
    @Autowired
    private LintDefectV2Dao lintDefectV2Dao;
    @Autowired
    private BuildDefectRepository buildDefectRepository;
    @Autowired
    private TaskLogService taskLogService;
    @Autowired
    private NewDefectJudgeService newDefectJudgeService;
    @Autowired
    private CheckerService multitoolCheckerService;

    /**
     * 根据前端传入的条件查询告警键值
     *
     * @param taskId
     * @param defectQueryReqVO
     * @return
     */
    @Override
    protected List getDefectsByQueryCond(long taskId, DefectQueryReqVO defectQueryReqVO)
    {
        List<String> toolNameSet = ParamUtils.getToolsByDimension(defectQueryReqVO.getToolName(), defectQueryReqVO.getDimension(), taskId);

        if (CollectionUtils.isEmpty(toolNameSet)) {
            return new ArrayList();
        }

        String buildId = defectQueryReqVO.getBuildId();
        Set<String> conditionDefectType = defectQueryReqVO.getDefectType();

        //获取任务信息
        Result<TaskDetailVO> taskInfoResult = client.get(ServiceTaskRestResource.class).getTaskInfoById(taskId);
        TaskDetailVO taskDetailVO = taskInfoResult.getData();

        //1.获取相同包id下的规则集合
        Set<String> pkgChecker = multitoolCheckerService.queryPkgRealCheckers(defectQueryReqVO.getPkgId(), toolNameSet, taskDetailVO);

        Set<String> defectIdSet = new HashSet<>();

        if (StringUtils.isNotEmpty(buildId))
        {
            Map<String, Boolean> toolDefectResultMap = taskLogService.defectCommitSuccess(taskId, toolNameSet, buildId, ComConstants.Step4MutliTool.COMMIT.value());
            List<String> successTools = toolDefectResultMap.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList());

            List<BuildDefectEntity> buildFiles = buildDefectRepository.findByTaskIdAndToolNameInAndBuildId(taskId, successTools, buildId);
            if (CollectionUtils.isNotEmpty(buildFiles))
            {
                for (BuildDefectEntity buildDefectEntity : buildFiles)
                {
                    defectIdSet.addAll(buildDefectEntity.getFileDefectIds());
                }
            }
        }

        long newDefectJudgeTime = 0;
        if (CollectionUtils.isNotEmpty(conditionDefectType)
                && !conditionDefectType.containsAll(Sets.newHashSet(ComConstants.DefectType.NEW.stringValue(), ComConstants.DefectType.HISTORY.stringValue())))
        {
            // 查询新老告警判定时间
            newDefectJudgeTime = newDefectJudgeService.getNewDefectJudgeTime(taskId, taskDetailVO);
        }

        Map<String, Boolean> filedMap = new HashMap<>();
        filedMap.put("_id", true);
        filedMap.put("status", true);
        List<LintDefectV2Entity> defectEntityList = lintDefectV2Dao.findDefectByCondition(taskId, defectQueryReqVO, defectIdSet, pkgChecker, newDefectJudgeTime, filedMap, toolNameSet);

        return defectEntityList;
    }

    /**
     * 根据前端传入的告警key，查询有效的告警
     * 过滤规则是：忽略告警、告警处理人分配、告警标志修改针对的都是待修复告警，而恢复忽略针对的是已忽略告警
     * @param batchDefectProcessReqVO
     */
    @Override
    protected List<LintDefectV2Entity> getEffectiveDefectByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        List<LintDefectV2Entity> defecEntityList = lintDefectV2Repository.findStatusByEntityIdIn(batchDefectProcessReqVO.getDefectKeySet());
        if (CollectionUtils.isEmpty(defecEntityList))
        {
            return new ArrayList<>();
        }

        Iterator<LintDefectV2Entity> it = defecEntityList.iterator();
        while (it.hasNext())
        {
            LintDefectV2Entity defectEntity = it.next();
            int status = defectEntity.getStatus();
            int statusCond = getStatusCondition();
            boolean notMatchNewStatus = statusCond == ComConstants.DefectStatus.NEW.value() && status != ComConstants.DefectStatus.NEW.value();
            boolean notMatchIgnoreStatus = statusCond > ComConstants.DefectStatus.NEW.value() && (status & statusCond) == 0;
            if (notMatchNewStatus || notMatchIgnoreStatus)
            {
                it.remove();
            }
        }
        return defecEntityList;
    }
}
