package com.tencent.bk.codecc.defect.utils;

import com.tencent.bk.codecc.defect.model.CCNStatisticEntity;
import com.tencent.bk.codecc.defect.model.CLOCStatisticEntity;
import com.tencent.bk.codecc.defect.model.CommonStatisticEntity;
import com.tencent.bk.codecc.defect.model.DUPCStatisticEntity;
import com.tencent.bk.codecc.defect.model.DefectEntity;
import com.tencent.bk.codecc.defect.model.LintStatisticEntity;

import java.util.Collection;
import java.util.List;

public interface CommonKafkaClient {

    void pushDefectEntityToKafka(List<DefectEntity> defectEntityList);

    /**
     * 推送编译型工具统计信息到数据平台
     * @param commonStatisticEntity
     */
    void pushCommonStatisticToKafka(CommonStatisticEntity commonStatisticEntity);

    /**
     * 推送lint类工具统计信息到数据平台
     * @param lintStatisticEntity
     */
    void pushLintStatisticToKafka(LintStatisticEntity lintStatisticEntity);


    /**
     * 推送lint类工具统计信息到数据平台
     * @param ccnStatisticEntity
     */
    void pushCCNStatisticToKafka(CCNStatisticEntity ccnStatisticEntity);

    /**
     * 推送lint类工具统计信息到数据平台
     * @param dupcStatisticEntity
     */
    void pushDUPCStatisticToKafka(DUPCStatisticEntity dupcStatisticEntity);


    /**
     * 推送代码行统计信息到数据平台
     * @param clocStatisticEntityList
     */
    void pushCLOCStatisticToKafka(Collection<CLOCStatisticEntity> clocStatisticEntityList);
}
