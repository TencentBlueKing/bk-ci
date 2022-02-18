import 'dart:io';

import 'package:bkci_app/providers/DownloadProvider.dart';
import 'package:bkci_app/utils/AppSetting.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_downloader/flutter_downloader.dart';

enum APP_STATUS {
  UNKNOWN,
  UPGRADE,
  OPEN,
  DOWNLOAD,
  ENQUEUED,
  RUNNING,
  PAUSED,
  FAILED,
  INSTALL,
  CANCELED,
  EXPIRE
}

// Mix-in [DiagnosticableTreeMixin] to have access to [debugFillProperties] for the devtool
class BkGlobalStateProvider with ChangeNotifier, DiagnosticableTreeMixin {
  Map _installedExpMap = Map();
  bool _isAppForeground = true;
  Map _settings;

  Map get installedExpMap => _installedExpMap;
  bool get isAppForeground => _isAppForeground;
  Map get settings => _settings;

  BkGlobalStateProvider() {
    _installedExpMap = Storage.getInstalledExpMap();
    _settings = Storage.settingConf ?? AppSetting.getDefaultSetting();
  }

  Future setSettings(String key, value) async {
    _settings = {
      ..._settings,
      key: value,
    };
    await Storage.setSettingConf(_settings);
    notifyListeners();
  }

  String getSettingByKey(String key) {
    return _settings[key] ?? null;
  }

  void setAppForegroundState(bool isAppForeground) {
    _isAppForeground = isAppForeground;
    notifyListeners();
  }

  bool getIsInstalledExp(String lastDownloadHashId, String currentExpId) {
    return lastDownloadHashId == currentExpId;
  }

  bool getIsNewerExp(String lastDownloadHashId, String currentExpId) {
    return gtExpHashId(currentExpId, lastDownloadHashId);
  }

  bool isLastDownloadExpId(
      String bundleId, String currentExpId, String lastDownloadHashId) {
    String localLastDownloadHashId =
        Storage.getDownloadedExpById(bundleId) ?? lastDownloadHashId;

    return localLastDownloadHashId == currentExpId;
  }

  Future setLastDownloadExpId(String bundleId, String expId) async {
    await Storage.setDownloadedExpMapByBundleId(
      bundleId: bundleId,
      expId: expId,
    );
    notifyListeners();
  }

  Future setInstalledExpId(String bundleId, String expId) async {
    await Storage.setInstalledExpMapByBundleId(
      bundleId: bundleId,
      expId: expId,
    );
    notifyListeners();
  }

  Future<void> removeInstalledExpByBundleId(String bundleId) async {
    await Storage.setInstalledExpMapByBundleId(
      bundleId: bundleId,
      isAdd: false,
    );
    notifyListeners();
  }

  getDownloadStatus({
    String bundleId,
    String currentExpId,
    String lastDownloadHashId,
    bool expired,
    DownloadJob task,
  }) {
    String lastInstalledExpId = Storage.getInstalledExpById(bundleId);
    if (Platform.isIOS) {
      lastInstalledExpId = lastInstalledExpId ?? lastDownloadHashId;
    }
    final bool isInstalled =
        getIsInstalledExp(lastInstalledExpId, currentExpId);
    final bool isNewerExp = getIsNewerExp(lastInstalledExpId, currentExpId);
    if (isInstalled) {
      return APP_STATUS.OPEN;
    } else if (task?.status == DownloadTaskStatus.enqueued) {
      return APP_STATUS.ENQUEUED;
    } else if (task?.status == DownloadTaskStatus.canceled) {
      return APP_STATUS.CANCELED;
    } else if (task?.status == DownloadTaskStatus.undefined) {
      return APP_STATUS.UNKNOWN;
    } else if (task?.status == DownloadTaskStatus.running) {
      return APP_STATUS.RUNNING;
    } else if (task?.status == DownloadTaskStatus.paused) {
      return APP_STATUS.PAUSED;
    } else if (task?.status == DownloadTaskStatus.failed) {
      return APP_STATUS.FAILED;
    } else if (task?.status == DownloadTaskStatus.complete) {
      return APP_STATUS.INSTALL;
    } else if (isNewerExp) {
      return APP_STATUS.UPGRADE;
    } else if (expired) {
      return APP_STATUS.EXPIRE;
    } else {
      return APP_STATUS.DOWNLOAD;
    }
  }
}
