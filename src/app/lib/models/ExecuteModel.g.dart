// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'ExecuteModel.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ExecuteModel _$ExecuteModelFromJson(Map<String, dynamic> json) {
  return ExecuteModel(
    buildId: json['buildId'] as String,
    buildMsg: json['buildMsg'] as String,
    userId: json['userId'] as String,
    trigger: json['trigger'] as String,
    status: json['status'] as String,
    cancelUserId: json['cancelUserId'] as String,
    packageVersion: json['packageVersion'] as String,
    pipelineVersion: json['pipelineVersion'] as int,
    pipelineId: json['pipelineId'] as String,
    pipelineName: json['pipelineName'] as String,
    projectId: json['projectId'] as String,
    currentTimestamp: json['currentTimestamp'] as int,
    startTime: json['startTime'] as int,
    endTime: json['endTime'] as int,
    buildNum: json['buildNum'] as int,
    model: PipelineModel.fromJson(json['model'] as Map<String, dynamic>),
    hasCollect: json['hasCollect'] as bool,
    material: json['material'] as List,
    remark: json['remark'] as String,
    executeTime: json['executeTime'] as int,
  );
}

Map<String, dynamic> _$ExecuteModelToJson(ExecuteModel instance) =>
    <String, dynamic>{
      'buildId': instance.buildId,
      'userId': instance.userId,
      'buildMsg': instance.buildMsg,
      'trigger': instance.trigger,
      'status': instance.status,
      'cancelUserId': instance.cancelUserId,
      'currentTimestamp': instance.currentTimestamp,
      'startTime': instance.startTime,
      'endTime': instance.endTime,
      'buildNum': instance.buildNum,
      'packageVersion': instance.packageVersion,
      'pipelineVersion': instance.pipelineVersion,
      'pipelineId': instance.pipelineId,
      'pipelineName': instance.pipelineName,
      'projectId': instance.projectId,
      'hasCollect': instance.hasCollect,
      'material': instance.material,
      'executeTime': instance.executeTime,
      'remark': instance.remark,
      'model': instance.model,
    };
