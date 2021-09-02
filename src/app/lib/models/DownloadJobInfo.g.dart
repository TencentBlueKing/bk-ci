// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'DownloadJobInfo.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DownloadJobInfo _$DownloadJobInfoFromJson(Map<String, dynamic> json) {
  return DownloadJobInfo(
    id: json['id'] as String,
    bundleIdentifier: json['bundleIdentifier'] as String,
    expId: json['expId'] as String,
    url: json['url'] as String,
    platform: json['platform'] as String,
    size: json['size'] as int,
    createTime: json['createTime'] as int,
    logoUrl: json['logoUrl'] as String,
    name: json['name'] as String,
    jobType: _$enumDecode(_$DownloadJobTypeEnumMap, json['jobType']),
    destination: json['destination'] as String,
  );
}

Map<String, dynamic> _$DownloadJobInfoToJson(DownloadJobInfo instance) =>
    <String, dynamic>{
      'url': instance.url,
      'logoUrl': instance.logoUrl,
      'platform': instance.platform,
      'name': instance.name,
      'id': instance.id,
      'expId': instance.expId,
      'bundleIdentifier': instance.bundleIdentifier,
      'createTime': instance.createTime,
      'size': instance.size,
      'jobType': _$DownloadJobTypeEnumMap[instance.jobType],
      'destination': instance.destination,
    };

T _$enumDecode<T>(
  Map<T, dynamic> enumValues,
  dynamic source, {
  T unknownValue,
}) {
  if (source == null) {
    throw ArgumentError('A value must be provided. Supported values: '
        '${enumValues.values.join(', ')}');
  }

  final value = enumValues.entries
      .singleWhere((e) => e.value == source, orElse: () => null)
      ?.key;

  if (value == null && unknownValue == null) {
    throw ArgumentError('`$source` is not one of the supported values: '
        '${enumValues.values.join(', ')}');
  }
  return value ?? unknownValue;
}

const _$DownloadJobTypeEnumMap = {
  DownloadJobType.Exp: 'Exp',
  DownloadJobType.Artifact: 'Artifact',
};
