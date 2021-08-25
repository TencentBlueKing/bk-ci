// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'pipeline.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Pipeline _$PipelineFromJson(Map<String, dynamic> json) {
  return Pipeline(
    projectId: json['projectId'] as String,
    projectName: json['projectName'] as String,
    pipelineId: json['pipelineId'] as String,
    pipelineName: json['pipelineName'] as String,
    pipelineDesc: json['pipelineDesc'] as String,
    latestBuildStatus: json['latestBuildStatus'] as String,
    latestBuildNum: json['latestBuildNum'] as int,
    latestBuildStartTime: json['latestBuildStartTime'] as int,
    latestBuildEndTime: json['latestBuildEndTime'] as int,
    latestBuildUser: json['latestBuildUser'] as String,
    pipelineVersion: json['pipelineVersion'] as int,
    canManualStartup: json['canManualStartup'] as bool,
    hasCollect: json['hasCollect'] as bool,
    deploymentTime: json['deploymentTime'] as int,
    createTime: json['createTime'] as int,
    logoUrl: json['logoUrl'] as String,
  );
}

Map<String, dynamic> _$PipelineToJson(Pipeline instance) => <String, dynamic>{
      'projectId': instance.projectId,
      'projectName': instance.projectName,
      'pipelineId': instance.pipelineId,
      'pipelineName': instance.pipelineName,
      'pipelineDesc': instance.pipelineDesc,
      'latestBuildStatus': instance.latestBuildStatus,
      'latestBuildNum': instance.latestBuildNum,
      'latestBuildStartTime': instance.latestBuildStartTime,
      'latestBuildEndTime': instance.latestBuildEndTime,
      'latestBuildUser': instance.latestBuildUser,
      'pipelineVersion': instance.pipelineVersion,
      'canManualStartup': instance.canManualStartup,
      'hasCollect': instance.hasCollect,
      'deploymentTime': instance.deploymentTime,
      'createTime': instance.createTime,
      'logoUrl': instance.logoUrl,
    };
