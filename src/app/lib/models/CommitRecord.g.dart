// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'CommitRecord.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

CommitRecord _$CommitRecordFromJson(Map<String, dynamic> json) {
  return CommitRecord(
    type: json['type'] as int,
    pipelineId: json['pipelineId'] as String,
    buildId: json['buildId'] as String,
    commit: json['commit'] as String,
    committer: json['committer'] as String,
    commitTime: json['commitTime'] as int,
    comment: json['comment'] as String,
    repoId: json['repoId'] as String,
    elementId: json['elementId'] as String,
    url: json['url'] as String,
  );
}

Map<String, dynamic> _$CommitRecordToJson(CommitRecord instance) =>
    <String, dynamic>{
      'type': instance.type,
      'pipelineId': instance.pipelineId,
      'buildId': instance.buildId,
      'commit': instance.commit,
      'committer': instance.committer,
      'commitTime': instance.commitTime,
      'comment': instance.comment,
      'repoId': instance.repoId,
      'elementId': instance.elementId,
      'url': instance.url,
    };
