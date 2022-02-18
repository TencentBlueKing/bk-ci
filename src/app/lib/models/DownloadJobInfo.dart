import 'package:json_annotation/json_annotation.dart';

part 'DownloadJobInfo.g.dart';

enum DownloadJobType {
  Exp,
  Artifact,
}

@JsonSerializable()
class DownloadJobInfo {
  final String url;
  final String logoUrl;
  final String platform;
  final String name;
  final String expId;
  final String bundleIdentifier;
  final int createTime;
  final int size;
  final DownloadJobType jobType;
  final String destination;
  String id;

  DownloadJobInfo({
    this.id,
    this.bundleIdentifier,
    this.expId,
    this.url,
    this.platform,
    this.size,
    this.createTime,
    this.logoUrl,
    this.name,
    this.jobType,
    this.destination,
  });

  factory DownloadJobInfo.fromJson(Map<String, dynamic> json) =>
      _$DownloadJobInfoFromJson(json);
  Map<String, dynamic> toJson() => _$DownloadJobInfoToJson(this);
}
