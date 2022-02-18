// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'project.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Project _$ProjectFromJson(Map<String, dynamic> json) {
  return Project(
    projectCode: json['projectCode'] as String,
    projectName: json['projectName'] as String,
    logoUrl: json['logoUrl'] as String,
    projectSource: json['projectSource'] as int,
  );
}

Map<String, dynamic> _$ProjectToJson(Project instance) => <String, dynamic>{
      'projectCode': instance.projectCode,
      'projectName': instance.projectName,
      'logoUrl': instance.logoUrl,
      'projectSource': instance.projectSource,
    };
