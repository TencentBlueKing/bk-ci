package com.tencent.bk.codecc.defect.consumer;

import com.tencent.bk.codecc.defect.service.impl.ClusterDefectServiceImpl;
import com.tencent.bk.codecc.defect.vo.CommitDefectVO;
import com.tencent.devops.common.service.IConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClusterDefectConsumer implements IConsumer<CommitDefectVO> {

    @Autowired
    private ClusterDefectServiceImpl clusterDefectService;

    @Override
    public void consumer(CommitDefectVO commitDefectVO) {
        try {
            clusterDefectService.cluster(commitDefectVO.getTaskId(),
                    commitDefectVO.getBuildId(),
                    commitDefectVO.getToolName());
        } catch (Throwable e) {
            log.info("", e);
        }
    }
}
