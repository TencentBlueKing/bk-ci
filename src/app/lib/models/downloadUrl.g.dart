// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'downloadUrl.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DownloadUrl _$DownloadUrlFromJson(Map<String, dynamic> json) {
  return DownloadUrl(
    url: json['url'] as String,
    platform: json['platform'] as String,
    size: json['size'] as int,
  );
}

Map<String, dynamic> _$DownloadUrlToJson(DownloadUrl instance) =>
    <String, dynamic>{
      'url': instance.url,
      'platform': instance.platform,
      'size': instance.size,
    };
