// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'build.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Build _$BuildFromJson(Map<String, dynamic> json) {
  return Build(
    id: json['id'] as String,
    userId: json['userId'] as String,
    buildMsg: json['buildMsg'] as String,
    trigger: json['trigger'] as String,
    buildNum: json['buildNum'] as int,
    pipelineVersion: json['pipelineVersion'] as int,
    startTime: json['startTime'] as int,
    endTime: json['endTime'] as int,
    status: json['status'] as String,
    stageStatus: json['stageStatus'] as List,
    deleteReason: json['deleteReason'] as String,
    currentTimeStamp: json['currentTimeStamp'] as String,
    material: json['material'] as List,
    queueTime: json['queueTime'] as int,
    totalTime: json['totalTime'] as int,
    executeTime: json['executeTime'] as int,
    startType: json['startType'] as String,
    retry: json['retry'] as bool,
    recommendVersion: json['recommendVersion'] as String,
    mobileStart: json['mobileStart'] as bool,
    buildParameters: json['buildParameters'] as List,
    artifactList: json['artifactList'] as List,
  );
}

Map<String, dynamic> _$BuildToJson(Build instance) => <String, dynamic>{
      'id': instance.id,
      'userId': instance.userId,
      'buildMsg': instance.buildMsg,
      'trigger': instance.trigger,
      'buildNum': instance.buildNum,
      'pipelineVersion': instance.pipelineVersion,
      'startTime': instance.startTime,
      'endTime': instance.endTime,
      'status': instance.status,
      'stageStatus': instance.stageStatus,
      'deleteReason': instance.deleteReason,
      'currentTimeStamp': instance.currentTimeStamp,
      'material': instance.material,
      'queueTime': instance.queueTime,
      'totalTime': instance.totalTime,
      'executeTime': instance.executeTime,
      'startType': instance.startType,
      'recommendVersion': instance.recommendVersion,
      'retry': instance.retry,
      'mobileStart': instance.mobileStart,
      'buildParameters': instance.buildParameters,
      'artifactList': instance.artifactList,
    };
