// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'Pkg.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Pkg _$PkgFromJson(Map<String, dynamic> json) {
  return Pkg(
    experienceHashId: json['experienceHashId'] as String,
    size: json['size'] as int,
    logoUrl: json['logoUrl'] as String,
    experienceName: json['experienceName'] as String,
    createTime: json['createTime'] as int,
    bundleIdentifier: json['bundleIdentifier'] as String,
  );
}

Map<String, dynamic> _$PkgToJson(Pkg instance) => <String, dynamic>{
      'experienceHashId': instance.experienceHashId,
      'size': instance.size,
      'logoUrl': instance.logoUrl,
      'experienceName': instance.experienceName,
      'createTime': instance.createTime,
      'bundleIdentifier': instance.bundleIdentifier,
    };
