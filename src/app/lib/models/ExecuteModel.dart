import 'package:bkci_app/models/PipelineModel.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:flutter/material.dart';
import 'package:json_annotation/json_annotation.dart';
import 'package:bkci_app/utils/util.dart';

part 'ExecuteModel.g.dart';

@JsonSerializable()
class ExecuteModel {
  final String buildId;
  final String userId;
  final String buildMsg;
  final String trigger;
  final String status;
  final String cancelUserId;
  final int currentTimestamp;
  final int startTime;
  final int endTime;
  final int buildNum;
  final String packageVersion;
  final int pipelineVersion;
  final String pipelineId;
  final String pipelineName;
  final String projectId;
  final bool hasCollect;
  final List material;
  final int executeTime;
  final String remark;
  final PipelineModel model;

  ExecuteModel({
    this.buildId,
    this.buildMsg,
    this.userId,
    this.trigger,
    this.status,
    this.cancelUserId,
    this.packageVersion,
    this.pipelineVersion,
    this.pipelineId,
    this.pipelineName,
    this.projectId,
    this.currentTimestamp,
    this.startTime,
    this.buildNum,
    this.model,
    this.hasCollect,
    this.material,
    this.remark,
    this.executeTime,
    this.endTime,
  });

  String get iconType {
    return iconStatusMap[status] ?? '';
  }

  Color get statusColor {
    return statuColorMap[iconType];
  }

  IconData get icon {
    return iconMap[iconType];
  }

  bool get isLoading {
    return iconType == 'loading';
  }

  String get totalExecuteTime {
    if (endTime == null || startTime == null) return '--';
    return (endTime > startTime)
        ? ((endTime - startTime) ~/ 1000).duration
        : '--';
  }

  bool get isDone {
    return [
      'SUCCEED',
      'CANCELED',
      'FAILED',
      'TERMINATE',
      'HEARTBEAT_TIMEOUT',
      'QUALITY_CHECK_FAIL',
      'QUEUE_TIMEOUT',
      'EXEC_TIMEOUT',
    ].contains(status);
  }

  factory ExecuteModel.fromJson(Map<String, dynamic> json) =>
      _$ExecuteModelFromJson(json);
  Map<String, dynamic> toJson() => _$ExecuteModelToJson(this);
}
