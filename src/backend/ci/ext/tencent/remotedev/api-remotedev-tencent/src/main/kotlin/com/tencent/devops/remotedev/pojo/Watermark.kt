package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "watermark")
data class Watermark(
    @Schema(description = "type", value = "水印类型，可选值为 mixed、explicit、blind，默认为 mixed")
    val type: String?,
    @JsonProperty("username")
    @Schema(description = "username", value = "用户名称，默认为 None")
    val userName: String?,
    @Schema(description = "version", value = "版本，默认为 None")
    var version: String?,
    @Schema(description = "tag", value = "标签，默认为 None")
    var tag: String?,
    @JsonProperty("output_format")
    @Schema(description = "output_format", value = "输出格式，可选值为 image、json，默认为 json")
    var outputFormat: String?,
    @JsonProperty("canvas_width")
    @Schema(description = "canvas_width", value = "画布宽度，默认为 1024")
    var canvasWidth: String?,
    @JsonProperty("canvas_height")
    @Schema(description = "canvas_height", value = "画布高度，默认为 768")
    var canvasHeight: String?,
    @JsonProperty("explicit_background_color")
    @Schema(description = "explicit_background_color", value = "明水印背景色，格式为 \"R,G,B,A\"，默认为 None")
    var explicitBackgroundColor: String?,
    @JsonProperty("explicit_font_size")
    @Schema(description = "explicit_font_size", value = "明水印字体大小，默认为 None")
    var explicitFontSize: String?,
    @JsonProperty("explicit_font_color")
    @Schema(description = "explicit_font_color", value = "明水印字体颜色，格式为 \"R,G,B,A\"，默认为 None")
    var explicitFontColor: String?,
    @JsonProperty("explicit_padding")
    @Schema(description = "explicit_padding", value = "明水印间隔，默认为 None")
    var explicitPadding: String?,
    @JsonProperty("blind_background_color")
    @Schema(description = "blind_background_color", value = "暗水印背景色，格式为 \"R,G,B,A\"，默认为 None")
    var blindBackgroundColor: String?,
    @JsonProperty("blind_background_image")
    @Schema(description = "blind_background_image", value = "暗水印图片 URL，默认为 None")
    var blindBackgroundImage: String?
)
