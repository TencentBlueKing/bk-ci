package com.tencent.bk.codecc.defect.vo;

import com.tencent.bk.codecc.defect.vo.enums.CheckerSetCategory;
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * 描述
 *
 * @version V1.0
 * @date 2020/2/25
 */
@Data
public class OtherCheckerSetListQueryReq
{
    @ApiModelProperty("关键字")
    private String keyWord;

    @ApiModelProperty("语言")
    private Set<String> checkerSetLanguage;

    @ApiModelProperty("规则集类别")
    private Set<CheckerSetCategory> checkerSetCategory;

    @ApiModelProperty("工具名")
    private Set<String> toolName;

    @ApiModelProperty("规则集来源")
    private Set<CheckerSetSource> checkerSetSource;

    @ApiModelProperty("创建者")
    private String creator;

    @ApiModelProperty("快速搜索框")
    private String quickSearch;

    @ApiModelProperty("排序字段")
    private String sortField;

    @ApiModelProperty("排序字段")
    private Sort.Direction sortType;

    @ApiModelProperty("排序字段")
    int pageNum;

    @ApiModelProperty("排序字段")
    int pageSize;

    @ApiModelProperty("项目是否已安装")
    private Boolean projectInstalled;
}
