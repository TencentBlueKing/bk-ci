// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'Appversion.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Appversion _$AppversionFromJson(Map<String, dynamic> json) {
  return Appversion(
    id: json['id'] as int,
    versionId: json['versionId'] as String,
    releaseDate: json['releaseDate'] as int,
    releaseContent: json['releaseContent'] as String,
    channelType: json['channelType'] as int,
    updateType: json['updateType'] as int,
  );
}

Map<String, dynamic> _$AppversionToJson(Appversion instance) =>
    <String, dynamic>{
      'id': instance.id,
      'versionId': instance.versionId,
      'releaseDate': instance.releaseDate,
      'releaseContent': instance.releaseContent,
      'channelType': instance.channelType,
      'updateType': instance.updateType,
    };
