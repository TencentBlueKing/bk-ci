package com.tencent.bk.codecc.defect.service;

import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.TaskPersonalStatisticRefreshReq;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.util.JsonUtil;
import com.tencent.devops.common.web.mq.ConstantsKt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * 告警批量分配处理器抽象类
 *
 * @version V1.0
 * @date 2019/10/31
 */
@Slf4j
public abstract class AbstractBatchDefectProcessBizService implements IBizService<BatchDefectProcessReqVO>
{
    @Autowired
    protected BizServiceFactory<IQueryWarningBizService> factory;

    @Autowired
    private TaskPersonalStatisticService taskPersonalStatisticService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    protected Client client;

    @Override
    public Result processBiz(BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        log.info("begin to batch process: {}", batchDefectProcessReqVO);
        String isSelectAll = batchDefectProcessReqVO.getIsSelectAll();

        List defectList;
        if (ComConstants.CommonJudge.COMMON_Y.value().equalsIgnoreCase(isSelectAll))
        {
            String queryDefectCondition = batchDefectProcessReqVO.getQueryDefectCondition();
            DefectQueryReqVO queryCondObj = JsonUtil.INSTANCE.to(queryDefectCondition, DefectQueryReqVO.class);
            if (queryCondObj == null)
            {
                log.error("try to new queryDefectConditionOjg instance by class failed， queryDefectCondition json is {}", queryDefectCondition);
                throw new CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID);
            }

            Set<String> status = new HashSet<>();
            status.add(String.valueOf(getStatusCondition()));
            queryCondObj.setStatus(status);

            defectList = getDefectsByQueryCond(batchDefectProcessReqVO.getTaskId(), queryCondObj);
        }
        else
        {
            defectList = getEffectiveDefectByDefectKeySet(batchDefectProcessReqVO);
        }

        if (CollectionUtils.isNotEmpty(defectList))
        {
            doBiz(defectList, batchDefectProcessReqVO);
        }
        log.info("batch process successful");
        return new Result(CommonMessageCode.SUCCESS, "batch process successful");
    }

    protected void refreshOverviewData(long taskId) {
        taskPersonalStatisticService.refresh(taskId, "from batch defect process: " + this);
    }

    /**
     * 获取批处理类型对应的告警状态条件
     * 忽略告警、告警处理人分配、告警标志修改针对的都是待修复告警，而恢复忽略针对的是已忽略告警
     * @return
     */
    protected int getStatusCondition()
    {
        return ComConstants.DefectStatus.NEW.value();
    }

    protected abstract void doBiz(List defectList, BatchDefectProcessReqVO batchDefectProcessReqVO);

    protected abstract List getDefectsByQueryCond(long taskId, DefectQueryReqVO defectQueryReqVO);

    protected abstract List getEffectiveDefectByDefectKeySet(BatchDefectProcessReqVO batchDefectProcessReqVO);
}
