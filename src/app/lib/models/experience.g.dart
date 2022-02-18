// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'experience.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Experience _$ExperienceFromJson(Map<String, dynamic> json) {
  return Experience(
    experienceHashId: json['experienceHashId'] as String,
    bundleIdentifier: json['bundleIdentifier'] as String,
    experienceName: json['experienceName'] as String,
    version: json['version'] as String,
    size: json['size'] as int,
    createTime: json['createTime'] as int,
    createDate: json['createDate'] as int,
    logoUrl: json['logoUrl'] as String,
    name: json['name'] as String,
    versionTitle: json['versionTitle'] as String,
    type: json['type'] as int,
    externalUrl: json['externalUrl'] as String,
    downloadTime: json['downloadTime'] as int,
    lastDownloadHashId: json['lastDownloadHashId'] as String,
    appStatus: json['appStatus'] as int,
    appScheme: json['appScheme'] as String,
    expired: json['expired'] as bool,
  );
}

Map<String, dynamic> _$ExperienceToJson(Experience instance) =>
    <String, dynamic>{
      'experienceHashId': instance.experienceHashId,
      'bundleIdentifier': instance.bundleIdentifier,
      'experienceName': instance.experienceName,
      'version': instance.version,
      'logoUrl': instance.logoUrl,
      'createTime': instance.createTime,
      'createDate': instance.createDate,
      'size': instance.size,
      'name': instance.name,
      'versionTitle': instance.versionTitle,
      'type': instance.type,
      'externalUrl': instance.externalUrl,
      'downloadTime': instance.downloadTime,
      'appStatus': instance.appStatus,
      'lastDownloadHashId': instance.lastDownloadHashId,
      'appScheme': instance.appScheme,
      'expired': instance.expired,
    };
