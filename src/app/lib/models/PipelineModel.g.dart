// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'PipelineModel.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PipelineModel _$PipelineModelFromJson(Map<String, dynamic> json) {
  return PipelineModel(
    name: json['name'] as String,
    desc: json['desc'] as String,
    stages: json['stages'] as List,
  );
}

Map<String, dynamic> _$PipelineModelToJson(PipelineModel instance) =>
    <String, dynamic>{
      'name': instance.name,
      'desc': instance.desc,
      'stages': instance.stages,
    };
