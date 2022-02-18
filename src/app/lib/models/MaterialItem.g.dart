// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'MaterialItem.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

MaterialItem _$MaterialItemFromJson(Map<String, dynamic> json) {
  return MaterialItem(
    name: json['name'] as String,
    elementId: json['elementId'] as String,
    records: (json['records'] as List)
        .map((e) => CommitRecord.fromJson(e as Map<String, dynamic>))
        .toList(),
  );
}

Map<String, dynamic> _$MaterialItemToJson(MaterialItem instance) =>
    <String, dynamic>{
      'name': instance.name,
      'elementId': instance.elementId,
      'records': instance.records,
    };
