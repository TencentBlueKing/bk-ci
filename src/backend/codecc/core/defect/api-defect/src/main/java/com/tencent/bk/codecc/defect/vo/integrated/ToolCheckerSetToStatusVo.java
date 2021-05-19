package com.tencent.bk.codecc.defect.vo.integrated;

import lombok.Data;

import java.util.Set;

@Data
public class ToolCheckerSetToStatusVo {
    private Set<String> checkerSetIds;
    private Set<String> checkerIds;
}
