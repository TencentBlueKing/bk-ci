package com.tencent.bk.codecc.defect.vo.customtool;

import com.tencent.bk.codecc.defect.vo.CCNStatisticVO;
import com.tencent.bk.codecc.defect.vo.CCNUploadStatisticVO;
import lombok.Data;

import java.util.List;

@Data
public class CCNFunctionsAndStatisticVO
{
    /**
     * 超标圈复杂度函数列表压缩字符串
     */
    private String defectsCompress;

    /**
     * 文件圈复杂度统计
     */
    private List<FileCCNVO> filesTotalCCN;
}
