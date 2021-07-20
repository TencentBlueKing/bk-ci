package com.tencent.bk.codecc.schedule.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;

/**
 * 文件分片合并
 *
 * @author zuihou
 * @date 2018/08/28
 */
@Data
@ToString
@ApiModel(value = "FileChunksMerge", description = "文件合并实体")
public class FileChunksMergeVO
{
    @NotNull(message = "文件名不能为空")
    @ApiModelProperty(value = "文件名", required = true)
    private String fileName;

    @ApiModelProperty(value = "分片总数")
    private Integer chunks;

    @NotNull(message = "上传类型不能为空")
    @ApiModelProperty(value = "上传类型")
    private String uploadType;

    @NotNull(message = "构建ID不能为空")
    @ApiModelProperty(value = "构建ID")
    private String buildId;
}
