package com.tencent.devops.common.pipeline.pojo.element.atom

/**
 * @ Author     ：Royal Huang
 * @ Date       ：Created in 11:09 2019-08-05
 * 老的代码库原子是通过子流水线的ID来关联流水线的，
 * 新的可通过流水线名称来关联
 */

enum class SubPipelineType {
    ID,
    NAME
}