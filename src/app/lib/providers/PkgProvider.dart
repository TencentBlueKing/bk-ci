import 'dart:collection';
import 'dart:convert';

import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/models/DownloadJobInfo.dart';
import 'package:bkci_app/models/Pkg.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:device_apps/device_apps.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_downloader/flutter_downloader.dart';

class PkgProvider with ChangeNotifier, DiagnosticableTreeMixin {
  List<Pkg> _pendingUpgradePkgs;
  List<DownloadJobInfo> _installedPkgs;
  List<Application> _apps;

  List<Pkg> get pendingUpgradePkgs => _pendingUpgradePkgs;
  List<DownloadJobInfo> get installedPkgs => _installedPkgs;
  List<Application> get apps => _apps;

  PkgProvider() {
    checkInstallPkgUpdate();
  }

  Future checkInstallPkgUpdate() async {
    // TODO:目前只检查已下载任务的包是否需要更新
    final installedDownloadJob = await getInstalledDownloadJob();
    final List data = installedDownloadJob
        .map(
          (job) {
            if (job.jobType == DownloadJobType.Exp) {
              // 只统计体验，忽略构件
              return {
                'bundleIdentifier': job.bundleIdentifier,
                'createTime': job.createTime,
              };
            }
            return null;
          },
        )
        .where((e) => e != null)
        .toList();
    final response = await ajax
        .post('$EXPERIENCE_API_PREFIX/download/checkVersion', data: data);

    final List<Pkg> result = [];
    response.data.forEach((e) {
      result.add(Pkg.fromJson(e));
    });
    _pendingUpgradePkgs = result;
    notifyListeners();
  }

  static Future getCompleteTask(
      [DownloadTaskStatus status = DownloadTaskStatus.complete]) async {
    final downloadedPkg = await FlutterDownloader.loadTasksWithRawQuery(
      query:
          'SELECT * FROM task WHERE status=${(status ?? DownloadTaskStatus.complete).value}',
    );
    return downloadedPkg;
  }

  static Future getDownloadJobByStatus(DownloadTaskStatus status) async {
    final downloadedTask = await getCompleteTask(status);

    return downloadedTask
        .map(
          (e) {
            final res = Storage.getString(e.taskId);
            if (res != null) {
              return DownloadJobInfo.fromJson(jsonDecode(res));
            }
            return null;
          },
        )
        .where(
          (item) => item != null && item.jobType == DownloadJobType.Exp,
        )
        .toList();
  }

  static Future getInstalledPkgByBundleId(String bundleId) async {
    final _apps = await DeviceApps.getInstalledApplications();

    return _apps.find<Application>(
      (Application element) => element.packageName == bundleId,
    );
  }

  Future getInstalledDownloadJob() async {
    final completeTask = await getCompleteTask();

    _apps = await DeviceApps.getInstalledApplications();

    final bundleIdentifierMap = HashMap<String, DownloadJobInfo>();
    completeTask.forEach(
      (e) {
        final res = Storage.getString(e.taskId);
        if (res != null) {
          final job = DownloadJobInfo.fromJson(jsonDecode(res));
          bundleIdentifierMap[job.bundleIdentifier] = job;
        }
        return null;
      },
    );
    _installedPkgs = _apps
        .where((element) => bundleIdentifierMap[element.packageName] != null)
        .map(
          (element) => bundleIdentifierMap[element.packageName],
        )
        .toList();

    return _installedPkgs;
  }
}
