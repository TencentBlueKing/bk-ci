// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'Log.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Log _$LogFromJson(Map<String, dynamic> json) {
  return Log(
    message: json['message'] as String,
    tag: json['tag'] as String,
    subTag: json['subTag'] as String,
    jobId: json['jobId'] as String,
    executeCount: json['executeCount'] as int,
    lineNo: json['lineNo'] as int,
    timestamp: json['timestamp'] as int,
    priority: json['priority'] as int,
  );
}

Map<String, dynamic> _$LogToJson(Log instance) => <String, dynamic>{
      'message': instance.message,
      'tag': instance.tag,
      'subTag': instance.subTag,
      'jobId': instance.jobId,
      'executeCount': instance.executeCount,
      'lineNo': instance.lineNo,
      'timestamp': instance.timestamp,
      'priority': instance.priority,
    };
