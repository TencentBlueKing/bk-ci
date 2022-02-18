// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'Artifactory.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Artifactory _$ArtifactoryFromJson(Map<String, dynamic> json) {
  return Artifactory(
    name: json['name'] as String,
    fullName: json['fullName'] as String,
    path: json['path'] as String,
    fullPath: json['fullPath'] as String,
    size: json['size'] as int,
    folder: json['folder'] as bool,
    modifiedTime: json['modifiedTime'] as int,
    artifactoryType: json['artifactoryType'] as String,
    show: json['show'] as bool,
    canDownload: json['canDownload'] as bool,
    logoUrl: json['logoUrl'] as String,
    bundleIdentifier: json['bundleIdentifier'] as String,
    md5: json['md5'] as String,
    platform: json['platform'] as String,
  );
}

Map<String, dynamic> _$ArtifactoryToJson(Artifactory instance) =>
    <String, dynamic>{
      'name': instance.name,
      'fullName': instance.fullName,
      'path': instance.path,
      'fullPath': instance.fullPath,
      'size': instance.size,
      'folder': instance.folder,
      'modifiedTime': instance.modifiedTime,
      'artifactoryType': instance.artifactoryType,
      'show': instance.show,
      'canDownload': instance.canDownload,
      'logoUrl': instance.logoUrl,
      'bundleIdentifier': instance.bundleIdentifier,
      'md5': instance.md5,
      'platform': instance.platform,
    };
