package com.tencent.bk.codecc.defect.consumer;

import static java.util.stream.Collectors.toSet;

import com.tencent.bk.codecc.defect.dao.mongorepository.CheckerSetTaskRelationshipRepository;
import com.tencent.bk.codecc.defect.model.checkerset.CheckerSetTaskRelationshipEntity;
import com.tencent.bk.codecc.defect.service.AbstractCodeScoringService;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.devops.common.api.BaseDataVO;
import com.tencent.devops.common.api.OpenSourceCheckerSetVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.ComConstants.CodeLang;
import com.tencent.devops.common.service.BaseDataCacheService;
import com.tencent.devops.common.service.IConsumer;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CodeScoringConsumer implements IConsumer<CommitDefectVO> {

    @Autowired
    private Client client;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BaseDataCacheService baseDataCacheService;

    @Autowired
    private CheckerSetTaskRelationshipRepository checkerSetTaskRelationshipRepository;

    @Override
    public void consumer(CommitDefectVO commitDefectVO) {
        Result<TaskDetailVO> result = client.get(ServiceTaskRestResource.class)
                .getTaskInfoById(commitDefectVO.getTaskId());
        if (result.isNotOk() || result.getData() == null) {
            log.error("scoring fail to get project id {} {}",
                    commitDefectVO.getTaskId(),
                    commitDefectVO.getBuildId());
            throw new CodeCCException("scoring fail to get project id");
        }

        try {
            TaskDetailVO taskDetailVO = result.getData();
            AbstractCodeScoringService codeScoringService =
                    applicationContext.getBean(getScoringServiceName(taskDetailVO),
                            AbstractCodeScoringService.class);
            codeScoringService.scoring(taskDetailVO,
                    commitDefectVO.getBuildId(),
                    commitDefectVO.getToolName(),
                    "Normal");
        } catch (Throwable e) {
            log.info("", e);
        }
    }

    private String getScoringServiceName(TaskDetailVO taskDetailVO) {
        boolean isOpenScan = isOpenScan(taskDetailVO.getTaskId(), taskDetailVO.getCodeLang());
        if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()
                .equalsIgnoreCase(taskDetailVO.getCreateFrom()) || isOpenScan
        ) {
            return "TStandard";
        } else {
            return "Custom";
        }
    }

    /**
     * 判断当前度量计算的环境是否符合开源扫描的场景
     * 规则集是否符合开源扫描规则集要求
     * 从缓存中根据当前项目语言获取相应的全量规则集信息与当前 Task 的规则集比对
     *
     * @param taskId
     * @param codeLang
     */
    private boolean isOpenScan(long taskId, long codeLang) {
        List<CheckerSetTaskRelationshipEntity> checkerSetTaskRelationshipEntityList =
                checkerSetTaskRelationshipRepository.findByTaskId(taskId);
        List<BaseDataVO> baseDataVOList = baseDataCacheService.getLanguageBaseDataFromCache(codeLang);
        // 过滤 OTHERS 的开源规则集
        Set<Object> openSourceCheckerSet = baseDataVOList.stream()
                .filter(baseDataVO ->
                        !(CodeLang.OTHERS.langName().equals(baseDataVO.getLangFullKey())))
                .flatMap(baseDataVO ->
                    baseDataVO.getOpenSourceCheckerListVO().stream()
                            .filter(openSourceCheckerSetVO ->
                                openSourceCheckerSetVO.getCheckerSetType() == null
                                        || "FULL".equals(openSourceCheckerSetVO.getCheckerSetType())
                            ).map(OpenSourceCheckerSetVO::getCheckerSetId)
                ).collect(toSet());
        Set<String> checkerSetIdSet = checkerSetTaskRelationshipEntityList.stream()
                .map(CheckerSetTaskRelationshipEntity::getCheckerSetId)
                .collect(toSet());
        return checkerSetIdSet.containsAll(openSourceCheckerSet);
    }
}
