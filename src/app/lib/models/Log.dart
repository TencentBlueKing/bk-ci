import 'package:json_annotation/json_annotation.dart';

part 'Log.g.dart';

@JsonSerializable()
class Log {
  final String message;
  final String tag;
  final String subTag;
  final String jobId;
  final int executeCount;
  final int lineNo;
  final int timestamp;
  final int priority;

  Log({
    this.message,
    this.tag,
    this.subTag,
    this.jobId,
    this.executeCount,
    this.lineNo,
    this.timestamp,
    this.priority,
  });

  factory Log.fromJson(Map<String, dynamic> json) => _$LogFromJson(json);
  Map<String, dynamic> toJson() => _$LogToJson(this);
}
