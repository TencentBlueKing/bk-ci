import 'package:json_annotation/json_annotation.dart';

part 'downloadUrl.g.dart';

@JsonSerializable()
class DownloadUrl {
  final String url;
  final String platform;
  final int size;

  DownloadUrl({
    this.url,
    this.platform,
    this.size,
  });

  factory DownloadUrl.fromJson(Map<String, dynamic> json) =>
      _$DownloadUrlFromJson(json);
  Map<String, dynamic> toJson() => _$DownloadUrlToJson(this);
}
