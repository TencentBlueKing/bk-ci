package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "watermark")
data class Watermark(
    @get:Schema(title = "type", description = "水印类型，可选值为 mixed、explicit、blind，默认为 mixed")
    val type: String?,
    @JsonProperty("username")
    @get:Schema(title = "username", description = "用户名称，默认为 None")
    val userName: String?,
    @get:Schema(title = "version", description = "版本，默认为 None")
    var version: String?,
    @get:Schema(title = "tag", description = "标签，默认为 None")
    var tag: String?,
    @JsonProperty("output_format")
    @get:Schema(title = "output_format", description = "输出格式，可选值为 image、json，默认为 json")
    var outputFormat: String?,
    @JsonProperty("canvas_width")
    @get:Schema(title = "canvas_width", description = "画布宽度，默认为 1024")
    var canvasWidth: String?,
    @JsonProperty("canvas_height")
    @get:Schema(title = "canvas_height", description = "画布高度，默认为 768")
    var canvasHeight: String?,
    @JsonProperty("explicit_background_color")
    @get:Schema(title = "explicit_background_color", description = "明水印背景色，格式为 \"R,G,B,A\"，默认为 None")
    var explicitBackgroundColor: String?,
    @JsonProperty("explicit_font_size")
    @get:Schema(title = "explicit_font_size", description = "明水印字体大小，默认为 None")
    var explicitFontSize: String?,
    @JsonProperty("explicit_font_color")
    @get:Schema(title = "explicit_font_color", description = "明水印字体颜色，格式为 \"R,G,B,A\"，默认为 None")
    var explicitFontColor: String?,
    @JsonProperty("explicit_padding")
    @get:Schema(title = "explicit_padding", description = "明水印间隔，默认为 None")
    var explicitPadding: String?,
    @JsonProperty("blind_background_color")
    @get:Schema(title = "blind_background_color", description = "暗水印背景色，格式为 \"R,G,B,A\"，默认为 None")
    var blindBackgroundColor: String?,
    @JsonProperty("blind_background_image")
    @get:Schema(title = "blind_background_image", description = "暗水印图片 URL，默认为 None")
    var blindBackgroundImage: String?
)
