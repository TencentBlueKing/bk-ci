// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'ShareArgs.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ShareArgs _$ShareArgsFromJson(Map<String, dynamic> json) {
  return ShareArgs(
    title: json['title'] as String,
    description: json['description'] as String,
    previewImageUrl: json['previewImageUrl'] as String,
    url: json['url'] as String,
    kind: json['kind'] as String,
    endDate: json['endDate'] as int,
    packageName: json['packageName'] as String,
    fileName: json['fileName'] as String,
    isArtifact: json['isArtifact'] as bool,
    isPublicExperience: json['isPublicExperience'] as bool,
  );
}

Map<String, dynamic> _$ShareArgsToJson(ShareArgs instance) => <String, dynamic>{
      'title': instance.title,
      'description': instance.description,
      'previewImageUrl': instance.previewImageUrl,
      'url': instance.url,
      'kind': instance.kind,
      'fileName': instance.fileName,
      'packageName': instance.packageName,
      'endDate': instance.endDate,
      'isArtifact': instance.isArtifact,
      'isPublicExperience': instance.isPublicExperience,
    };
