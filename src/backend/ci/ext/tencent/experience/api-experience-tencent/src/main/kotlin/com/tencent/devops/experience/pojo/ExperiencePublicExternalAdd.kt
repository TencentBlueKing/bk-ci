import com.tencent.devops.common.api.enums.PlatformEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "版本体验-新增外部链接公开体验")
data class ExperiencePublicExternalAdd(
    @Schema(description = "体验名称", required = true)
    val experienceName: String,
    @Schema(description = "产品类别", required = true)
    val category: Int,
    @Schema(description = "操作系统", required = true)
    val platform: PlatformEnum,
    @Schema(description = "LOGO地址", required = true)
    val logoUrl: String,
    @Schema(description = "外部链接", required = true)
    val externalLink: String
)
