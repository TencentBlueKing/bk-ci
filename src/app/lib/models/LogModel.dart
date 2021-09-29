import 'package:bkci_app/models/Log.dart';
import 'package:json_annotation/json_annotation.dart';

part 'LogModel.g.dart';

final statusMap = {
  999: 'logErrorTips',
  3: 'logRevertTips',
  2: 'logCleanTips',
  1: 'logEmptyTips',
};

@JsonSerializable()
class LogModel {
  final String buildId;
  final int startLineNo;
  final int endLineNo;
  final List<Log> logs;
  final int timeUsed;
  final int status;
  final bool hasMore;
  final bool finished;

  LogModel({
    this.buildId,
    this.startLineNo,
    this.endLineNo,
    this.logs,
    this.timeUsed,
    this.status,
    this.hasMore,
    this.finished,
  });

  String get statusLabel {
    return statusMap[status] ?? 'logEmptyTips';
  }

  factory LogModel.fromJson(Map<String, dynamic> json) =>
      _$LogModelFromJson(json);
  Map<String, dynamic> toJson() => _$LogModelToJson(this);
}
