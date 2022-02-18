import 'package:json_annotation/json_annotation.dart';

part 'ShareArgs.g.dart';

@JsonSerializable()
class ShareArgs {
  final String title;
  final String description;
  final String previewImageUrl;
  final String url;
  final String kind;
  final String fileName;
  final String packageName;
  final int endDate;
  final bool isArtifact;
  final bool isPublicExperience;

  ShareArgs({
    this.title,
    this.description,
    this.previewImageUrl,
    this.url,
    this.kind,
    this.endDate,
    this.packageName,
    this.fileName,
    this.isArtifact = false,
    this.isPublicExperience = false,
  });

  factory ShareArgs.fromJson(Map<String, dynamic> json) =>
      _$ShareArgsFromJson(json);
  Map<String, dynamic> toJson() => _$ShareArgsToJson(this);
}
