package com.tencent.bk.codecc.task.vo.scanconfiguration;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * 扫描触发配置视图
 *
 * @version V4.0
 * @date 2019/11/8
 */
@Data
@ApiModel("扫描触发配置视图")
public class ScanConfigurationVO extends CommonVO
{
    /**
     * 定时扫描配置
     */
    private TimeAnalysisConfigVO timeAnalysisConfig;

    /**
     * 1：增量；0：全量; 2: diff模式
     */
    private Integer scanType;

    /**
     * 新告警转为历史告警的配置
     */
    private NewDefectJudgeVO newDefectJudge;

    /**
     * 告警作者转换配置
     */
    private List<TransferAuthorPair> transferAuthorList;

    /*
     * 是否回写工蜂
     */
    private Boolean mrCommentEnable;

    @Data
    public static class TransferAuthorPair
    {
        private String sourceAuthor;
        private String targetAuthor;
    }
}
