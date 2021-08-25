import 'package:json_annotation/json_annotation.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/utils/constants.dart';

part 'pipeline.g.dart';

final statusColor = const {
  'FAILED': '#EA3636',
  'SUCCEED': '#3FC06D',
  'RUNNING': '#3A84FF',
  'QUEUE': '#3A84FF',
  'STAGE_SUCCESS': '#3FC06D',
  'CANCELED': '#FF9C01'
};

@JsonSerializable()
class Pipeline {
  final String projectId;
  final String projectName;
  final String pipelineId;
  final String pipelineName;
  final String pipelineDesc;
  final String latestBuildStatus;
  final int latestBuildNum;
  final int latestBuildStartTime;
  final int latestBuildEndTime;
  final String latestBuildUser;
  final int pipelineVersion;
  final bool canManualStartup;
  final bool hasCollect;
  final int deploymentTime;
  final int createTime;
  final String logoUrl;

  Pipeline({
    this.projectId,
    this.projectName,
    this.pipelineId,
    this.pipelineName,
    this.pipelineDesc,
    this.latestBuildStatus,
    this.latestBuildNum,
    this.latestBuildStartTime,
    this.latestBuildEndTime,
    this.latestBuildUser,
    this.pipelineVersion,
    this.canManualStartup,
    this.hasCollect,
    this.deploymentTime,
    this.createTime,
    this.logoUrl,
  });

  String get getStatus {
    return latestBuildStatus;
  }

  Color get statusColor {
    return statuColorMap[iconType];
  }

  String get iconType {
    return iconStatusMap[latestBuildStatus] ?? '';
  }

  IconData get icon {
    return iconMap[iconType];
  }

  String get getCreateDate {
    if (latestBuildStartTime == null) return '';
    return latestBuildStartTime > 0 ? latestBuildStartTime.yyMMdd : '';
  }

  String get subTitle {
    final buildStr = latestBuildNum > 0 ? '#$latestBuildNum' : '未构建';
    return '$buildStr      ${this.getCreateDate}';
  }

  factory Pipeline.fromJson(Map<String, dynamic> json) =>
      _$PipelineFromJson(json);
  Map<String, dynamic> toJson() => _$PipelineToJson(this);
}
