import com.tencent.devops.common.api.enums.PlatformEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-新增外部链接公开体验")
data class ExperiencePublicExternalAdd(
    @ApiModelProperty("体验名称", required = true)
    val experienceName: String,
    @ApiModelProperty("产品类别", required = true)
    val category: Int,
    @ApiModelProperty("操作系统", required = true)
    val platform: PlatformEnum,
    @ApiModelProperty("LOGO地址", required = true)
    val logoUrl: String,
    @ApiModelProperty("外部链接", required = true)
    val externalLink: String
)
