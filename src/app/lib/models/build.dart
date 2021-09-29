import 'package:bkci_app/utils/constants.dart';
import 'package:flutter/material.dart';
import 'package:json_annotation/json_annotation.dart';
import 'package:bkci_app/utils/util.dart';

part 'build.g.dart';

@JsonSerializable()
class Build {
  final String id;
  final String userId;
  final String buildMsg;
  final String trigger;
  final int buildNum;
  final int pipelineVersion;
  final int startTime;
  final int endTime;
  final String status;
  final List stageStatus;
  final String deleteReason;
  final String currentTimeStamp;
  final List material;
  final int queueTime;
  final int totalTime;
  final int executeTime;
  final String startType;
  final String recommendVersion;
  final bool retry;
  final bool mobileStart;
  final List buildParameters;
  final List artifactList;

  Build({
    this.id,
    this.userId,
    this.buildMsg,
    this.trigger,
    this.buildNum,
    this.pipelineVersion,
    this.startTime,
    this.endTime,
    this.status,
    this.stageStatus,
    this.deleteReason,
    this.currentTimeStamp,
    this.material,
    this.queueTime,
    this.totalTime,
    this.executeTime,
    this.startType,
    this.retry,
    this.recommendVersion,
    this.mobileStart,
    this.buildParameters,
    this.artifactList,
  });

  String get getStatus {
    return status;
  }

  String get getCreateDate {
    return startTime.yMdhm;
  }

  List<String> get getAppVersions {
    List<String> versions = [];
    if (artifactList != null) {
      artifactList.forEach((item) {
        if (item['appVersion'] != null &&
            item['appVersion'] != '' &&
            versions.indexOf(item['appVersion']) == -1) {
          versions.add(item['appVersion']);
        }
      });
    }
    return versions;
  }

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

  String get getTimeUser {
    return '${startTime.mdhm}     $userId';
  }

  factory Build.fromJson(Map<String, dynamic> json) => _$BuildFromJson(json);
  Map<String, dynamic> toJson() => _$BuildToJson(this);
}
