// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'LogModel.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

LogModel _$LogModelFromJson(Map<String, dynamic> json) {
  return LogModel(
    buildId: json['buildId'] as String,
    startLineNo: json['startLineNo'] as int,
    endLineNo: json['endLineNo'] as int,
    logs: (json['logs'] as List)
        .map((e) => Log.fromJson(e as Map<String, dynamic>))
        .toList(),
    timeUsed: json['timeUsed'] as int,
    status: json['status'] as int,
    hasMore: json['hasMore'] as bool,
    finished: json['finished'] as bool,
  );
}

Map<String, dynamic> _$LogModelToJson(LogModel instance) => <String, dynamic>{
      'buildId': instance.buildId,
      'startLineNo': instance.startLineNo,
      'endLineNo': instance.endLineNo,
      'logs': instance.logs,
      'timeUsed': instance.timeUsed,
      'status': instance.status,
      'hasMore': instance.hasMore,
      'finished': instance.finished,
    };
