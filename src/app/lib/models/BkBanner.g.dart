// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'BkBanner.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

BkBanner _$BkBannerFromJson(Map<String, dynamic> json) {
  return BkBanner(
    type: json['type'] as int,
    experienceHashId: json['experienceHashId'] as String,
    bannerUrl: json['bannerUrl'] as String,
    externalUrl: json['externalUrl'] as String,
  );
}

Map<String, dynamic> _$BkBannerToJson(BkBanner instance) => <String, dynamic>{
      'type': instance.type,
      'experienceHashId': instance.experienceHashId,
      'bannerUrl': instance.bannerUrl,
      'externalUrl': instance.externalUrl
    };
