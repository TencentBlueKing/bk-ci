package com.tencent.bk.codecc.defect.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 告警通用统计结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonDefectStatisticModel {
    /**
     * 新增提示级告警数
     */
    private int newPromptCount;

    /**
     * 新增一般级告警数
     */
    private int newNormalCount;

    /**
     * 新增严重级告警数
     */
    private int newSeriousCount;

    //#########################################

    /**
     * 遗留提示级告警数
     */
    private int existPromptCount;

    /**
     * 遗留一般级告警数
     */
    private int existNormalCount;

    /**
     * 遗留严重级告警数
     */
    private int existSeriousCount;

    //#########################################

    /**
     * “新增”的处理人，涵盖三种严重级别
     */
    private Set<String> newAuthors;

    /**
     * 新增提示级告警处理人
     */
    private Set<String> newPromptAuthors;

    /**
     * 新增一般级告警处理人
     */
    private Set<String> newNormalAuthors;

    /**
     * 新增严重级告警处理人
     */
    private Set<String> newSeriousAuthors;

    //#########################################

    /**
     * "存量"的处理人，涵盖三种严重级别
     */
    private Set<String> existAuthors;

    /**
     * 存量提示级告警处理人
     */
    private Set<String> existPromptAuthors;

    /**
     * 存量一般级告警处理人
     */
    private Set<String> existNormalAuthors;

    /**
     * 存量严重级告警处理人
     */
    private Set<String> existSeriousAuthors;
}
