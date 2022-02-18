// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'experienceDetail.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ExperienceDetail _$ExperienceDetailFromJson(Map<String, dynamic> json) {
  return ExperienceDetail(
    name: json['name'] as String,
    experienceHashId: json['experienceHashId'] as String,
    experienceName: json['experienceName'] as String,
    logoUrl: json['logoUrl'] as String,
    shareUrl: json['shareUrl'] as String,
    platform: json['platform'] as String,
    version: json['version'] as String,
    versionTitle: json['versionTitle'] as String,
    remark: json['remark'] as String,
    packageName: json['packageName'] as String,
    bundleIdentifier: json['bundleIdentifier'] as String,
    expired: json['expired'] as bool,
    canExperience: json['canExperience'] as bool,
    online: json['online'] as bool,
    publicExperience: json['publicExperience'] as bool,
    productOwner:
        (json['productOwner'] as List).map((e) => e as String).toList(),
    size: json['size'] as int,
    createDate: json['createDate'] as int,
    endDate: json['endDate'] as int,
    categoryId: json['categoryId'] as int,
    lastDownloadHashId: json['lastDownloadHashId'] as String,
    appScheme: json['appScheme'] as String,
    appStatus: json['appStatus'] as int,
    experienceCondition: json['experienceCondition'] as int,
  );
}

Map<String, dynamic> _$ExperienceDetailToJson(ExperienceDetail instance) =>
    <String, dynamic>{
      'name': instance.name,
      'experienceHashId': instance.experienceHashId,
      'experienceName': instance.experienceName,
      'logoUrl': instance.logoUrl,
      'shareUrl': instance.shareUrl,
      'platform': instance.platform,
      'version': instance.version,
      'versionTitle': instance.versionTitle,
      'remark': instance.remark,
      'packageName': instance.packageName,
      'expired': instance.expired,
      'canExperience': instance.canExperience,
      'online': instance.online,
      'publicExperience': instance.publicExperience,
      'productOwner': instance.productOwner,
      'size': instance.size,
      'categoryId': instance.categoryId,
      'createDate': instance.createDate,
      'endDate': instance.endDate,
      'bundleIdentifier': instance.bundleIdentifier,
      'appStatus': instance.appStatus,
      'lastDownloadHashId': instance.lastDownloadHashId,
      'appScheme': instance.appScheme,
      'experienceCondition': instance.experienceCondition,
    };
