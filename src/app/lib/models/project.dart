import 'package:json_annotation/json_annotation.dart';

part 'project.g.dart';

enum PROJECT_TYPE {
  UNKNOW,
  BKCI,
  STREAM,
}

@JsonSerializable()
class Project {
  final String projectCode;
  final String projectName;
  final String logoUrl;
  final int projectSource;

  Project({
    this.projectCode,
    this.projectName,
    this.logoUrl,
    this.projectSource,
  });

  bool get isStreamProject {
    return this.projectSource == PROJECT_TYPE.STREAM.index;
  }

  factory Project.fromJson(Map<String, dynamic> json) =>
      _$ProjectFromJson(json);
  Map<String, dynamic> toJson() => _$ProjectToJson(this);
}
