package com.tencent.bk.codecc.defect.utils;

import com.tencent.bk.codecc.defect.model.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.model.CommonStatisticEntity;
import com.tencent.bk.codecc.defect.model.DUPCStatisticEntity;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.model.LintStatisticEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class CommonKafkaClientImpl implements CommonKafkaClient {

    @Override
    public void pushDefectEntityToKafka(List<DefectEntity> defectEntityList) {
    }

    /**
     * 推送编译型工具统计信息到数据平台
     * @param commonStatisticEntity
     */
    @Override
    public void pushCommonStatisticToKafka(CommonStatisticEntity commonStatisticEntity) {
    }

    /**
     * 推送lint类工具统计信息到数据平台
     * @param lintStatisticEntity
     */
    @Override
    public void pushLintStatisticToKafka(LintStatisticEntity lintStatisticEntity) {
    }


    /**
     * 推送lint类工具统计信息到数据平台
     * @param ccnStatisticEntity
     */
    @Override
    public void pushCCNStatisticToKafka(CCNStatisticEntity ccnStatisticEntity) {
    }

    /**
     * 推送lint类工具统计信息到数据平台
     * @param dupcStatisticEntity
     */
    @Override
    public void pushDUPCStatisticToKafka(DUPCStatisticEntity dupcStatisticEntity) {
    }


    /**
     * 推送代码行统计信息到数据平台
     * @param clocStatisticEntityList
     */
    @Override
    public void pushCLOCStatisticToKafka(Collection<CLOCStatisticEntity> clocStatisticEntityList) {
    }
}
