import 'dart:io' show Platform;
import 'dart:isolate';
import 'dart:ui';

import 'package:bkci_app/main.dart';
import 'package:bkci_app/models/Appversion.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/widgets/CheckUpdateDialog.dart';
import 'package:bkci_app/widgets/Loading.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_downloader/flutter_downloader.dart';
import 'package:minimize_app/minimize_app.dart';
import 'package:path_provider/path_provider.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:version/version.dart';

// Mix-in [DiagnosticableTreeMixin] to have access to [debugFillProperties] for the devtool
class CheckUpdateProvider with ChangeNotifier, DiagnosticableTreeMixin {
  Appversion _newVersion;
  bool _needUpgrade = false;
  List<Appversion> _changelogList = [];
  int _upgradeProgress = 0;
  DownloadTaskStatus _upgradeStatus;
  String _upradeTaskId;
  bool _isDialogShow = false;
  bool _isUpgrading = false;
  bool _hasListener = false;

  Version get newSemver => _newVersion.semver;
  List<Appversion> get changelogList => _changelogList;
  bool get needUpgrade => _needUpgrade;
  int get upgradeProgress => _upgradeProgress;
  String get upgradeStatus => _upgradeStatus.toString();

  String get releaseContent =>
      _newVersion != null ? _newVersion.releaseContent : '';

  bool get isUpgrading =>
      [
        DownloadTaskStatus.running,
        DownloadTaskStatus.enqueued,
      ].contains(_upgradeStatus) ||
      _isUpgrading;

  bool get isDownloaded => DownloadTaskStatus.complete == _upgradeStatus;
  bool get isUpgradeDialogShow => _isDialogShow;

  static const String INS_NAME = 'check_update_port';
  ReceivePort _port = ReceivePort();

  Future checkUpdate() async {
    BuildContext context = DevopsApp?.navigatorKey?.currentContext;
    final newVersion = await BkLoading.of(context).during(getLastVersion());
    final currentAppSemver = Version.parse(Storage.appVersion);

    _needUpgrade = currentAppSemver < newVersion.semver;
    _newVersion = newVersion;

    Future.delayed(Duration.zero, () {
      if (_needUpgrade && !_isDialogShow) {
        _showUpdateDialog(newVersion);
      } else if (!_needUpgrade) {
        toast(BkDevopsAppi18n.translate('noNeedUpgradeTips'));
      }
    });
  }

  Future hasNewVersion() async {
    final newVersion = await getLastVersion();
    final currentAppSemver = Version.parse(Storage.appVersion);

    final isAppUpdatePopupShowed =
        Storage.isAppUpdatePopupShowed(newVersion.versionId);
    _needUpgrade = currentAppSemver < newVersion.semver;
    _newVersion = newVersion;

    if (_needUpgrade &&
        !_isDialogShow &&
        (!isAppUpdatePopupShowed || newVersion.isForceUpgrade)) {
      _showUpdateDialog(newVersion);
    }
  }

  Future<void> _showUpdateDialog(Appversion newVersion) async {
    BuildContext context = DevopsApp?.navigatorKey?.currentContext;
    _isDialogShow = true;
    Storage.setAppUpdatePopupShowed(newVersion.versionId);
    return showDialog(
      context: context,
      barrierDismissible: false, // user must tap button!
      builder: (BuildContext context) {
        return CheckUpdateDialog(
          content: newVersion.releaseContent,
          version: newVersion.versionId,
          isForceUpgrade: newVersion.isForceUpgrade,
          upgrade: _upgrade,
          close: () {
            _close(context);
          },
        );
      },
    );
  }

  void _close(BuildContext context) {
    _isDialogShow = false;
    Navigator.pop(context);
  }

  Future _upgrade(BuildContext context, String version) async {
    if (_isUpgrading) return;
    if (Platform.isIOS) {
      final String plistUrl =
          'itms-services://?action=download-manifest&url=$BASE_URL_PREFIX/app/download/devops_app.plist';
      final supported = await canLaunch(plistUrl);

      if (supported) {
        await launch(plistUrl);
        MinimizeApp.minimizeApp();
      }
      return;
    }
    _isUpgrading = true;
    _upgradeProgress = 0;
    FlutterDownloader.registerCallback(downloadCallback);
    _bindBackgroundIsolate(context);

    notifyListeners();

    final savePath = await getExternalStorageDirectory();
    final savedDir = savePath.path;
    final taskId = await BkLoading.of(context).during(FlutterDownloader.enqueue(
      url: '$BASE_URL_PREFIX/app/download/devops_app.apk',
      fileName: 'devops_app_$version.apk',
      savedDir: savedDir,
      mimeType: 'application/vnd.android.package-archive',
      showNotification: false,
      openFileFromNotification: false,
    ));

    _upradeTaskId = taskId;
  }

  Future getAppChangelog() async {
    final response = await ajax.get(
      '/support/api/app/app/version',
      queryParameters: {
        'channelType': Storage.platfrom,
      },
    );
    final List<Appversion> result = [];

    response.data.forEach((ele) {
      final Appversion version = Appversion.fromJson(ele);
      result.add(version);
    });

    _changelogList = result;

    notifyListeners();
  }

  Future getLastVersion() async {
    final response = await ajax.get(
      '/support/api/app/app/version/last',
      queryParameters: {
        'channelType': Storage.platfrom,
      },
    );

    return Appversion.fromJson(response.data);
  }

  Future installApk(BuildContext context) async {
    _unbindBackgroundIsolate();
    _isUpgrading = false;
    _isDialogShow = false;
    Navigator.pop(context);
    if (_upradeTaskId != null) {
      Future.delayed(Duration(seconds: 1), () async {
        await FlutterDownloader.open(taskId: _upradeTaskId);
        await FlutterDownloader.remove(taskId: _upradeTaskId);

        _upradeTaskId = null;
        _upgradeStatus = null;
      });
    }
  }

  void _bindBackgroundIsolate(BuildContext context) {
    bool isSuccess =
        IsolateNameServer.registerPortWithName(_port.sendPort, INS_NAME);
    if (!isSuccess) {
      _unbindBackgroundIsolate();
      _bindBackgroundIsolate(context);
      return;
    }
    if (!_hasListener) {
      _port.listen(
        (dynamic data) {
          _hasListener = true;
          DownloadTaskStatus status = data[1];
          int progress = data[2];

          _upgradeProgress = progress;
          _upgradeStatus = status;
          notifyListeners();
        },
      );
    }
  }

  void _unbindBackgroundIsolate() {
    IsolateNameServer.removePortNameMapping(INS_NAME);
  }

  static void downloadCallback(
    String id,
    DownloadTaskStatus status,
    int progress,
  ) {
    final SendPort send = IsolateNameServer.lookupPortByName(INS_NAME);
    send.send([id, status, progress]);
  }
}
