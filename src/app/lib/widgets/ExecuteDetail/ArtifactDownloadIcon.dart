import 'dart:io';

import 'package:bkci_app/models/Artifactory.dart';
import 'package:bkci_app/providers/DownloadProvider.dart';
import 'package:bkci_app/providers/PkgProvider.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/ConfirmDialog.dart';
import 'package:bkci_app/widgets/Loading.dart';
import 'package:connectivity/connectivity.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter_downloader/flutter_downloader.dart';
import 'package:minimize_app/minimize_app.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

class ArtifactoryInfo {
  final String taskId;
  final DownloadTaskStatus status;
  final String bundleIdentifier;
  final bool isInstalled;
  final int progress;

  ArtifactoryInfo({
    this.taskId,
    this.status,
    this.bundleIdentifier,
    this.isInstalled,
    this.progress,
  });
}

class ArtifactDownloadIcon extends StatelessWidget {
  final Artifactory artifactory;
  final String projectId;
  final double iconSize;
  final Color iconColor;
  final Widget Function(
    BuildContext context,
    dynamic data,
    Function handler,
    ArtifactoryInfo value,
  ) entryBuilder;

  ArtifactDownloadIcon({
    this.artifactory,
    this.projectId,
    this.iconSize,
    this.iconColor,
    this.entryBuilder,
  });

  Future _handlePressed(
    BuildContext context,
    ArtifactoryInfo task,
  ) async {
    final downloadProvider =
        Provider.of<DownloadProvider>(context, listen: false);
    if (task.taskId == null ||
        task.status == DownloadTaskStatus.canceled ||
        task.status == DownloadTaskStatus.undefined) {
      ConnectivityResult connectivity =
          await Connectivity().checkConnectivity();
      if (task.taskId != null) {
        await downloadProvider.removeDownloadByTaskId(task.taskId, true);
      }
      if (connectivity == ConnectivityResult.mobile) {
        showDialog(
            context: context,
            builder: (context) {
              return ConfirmDialog(
                height: 260.px,
                confirmLabel: BkDevopsAppi18n.of(context).$t('download'),
                contentMsg:
                    "${BkDevopsAppi18n.of(context).$t('networkConfirmTips')}（${artifactory.size.mb}）${BkDevopsAppi18n.of(context).$t('flow')}",
                confirm: () {
                  return BkLoading.of(context)
                      .during(download(downloadProvider));
                },
              );
            });
      } else {
        BkLoading.of(context).during(download(downloadProvider));
      }
    } else if (task.status == DownloadTaskStatus.enqueued ||
        task.status == DownloadTaskStatus.running) {
      BkLoading.of(context).during(downloadProvider.pause(task.taskId));
    } else if (task.status == DownloadTaskStatus.paused) {
      BkLoading.of(context).during(downloadProvider.resume(task.taskId));
    } else if (task.status == DownloadTaskStatus.failed) {
      BkLoading.of(context).during(downloadProvider.retry(task.taskId));
    } else if (task.status == DownloadTaskStatus.complete) {
      BkLoading.of(context).during(downloadProvider.open(task.taskId));
    }
  }

  Future download(DownloadProvider downloadProvider) async {
    await downloadProvider.downloadArtifact(
      artifactory,
      projectId,
    );
    await downloadProvider.getAllDownloadTasks();
  }

  Future installApp(BuildContext context) async {
    final downloadProvider =
        Provider.of<DownloadProvider>(context, listen: false);
    final downloadUrl = await downloadProvider.getArtifactoryDownloadUrl(
      projectId,
      artifactory.artifactoryType,
      artifactory.fullPath,
    );
    final itmsUrl = getItmsUrl(downloadUrl.url);

    final supported = await canLaunch(itmsUrl);

    if (supported) {
      await launch(itmsUrl);
      MinimizeApp.minimizeApp();
    }
  }

  _getIconByStatus(BuildContext context, ArtifactoryInfo task) {
    String tooltip = task.status != null
        ? downloadStatusLabel[task.status.value]
        : 'download';
    IconData icon = BkIcons.download;
    if (task.isInstalled) {
      icon = Icons.save_rounded;
    }

    if (DownloadTaskStatus.complete == task.status) {
      icon = Icons.donut_large;
    } else if (DownloadTaskStatus.canceled == task.status) {
      icon = Icons.cancel;
    } else if (DownloadTaskStatus.failed == task.status) {
      icon = Icons.sms_failed;
    } else if (DownloadTaskStatus.paused == task.status) {
      icon = Icons.play_circle_outline;
    } else if (DownloadTaskStatus.running == task.status) {
      icon = Icons.pause_circle_outline;
    }

    return [icon, tooltip];
  }

  Widget _boxBuilder(
    BuildContext context,
    IconData icon,
    Function onPressed,
    String tooltip,
  ) {
    return IconButton(
      padding: EdgeInsets.all(0),
      iconSize: iconSize,
      color: iconColor,
      icon: Icon(
        icon,
      ),
      tooltip: tooltip,
      onPressed: onPressed,
    );
  }

  @override
  Widget build(BuildContext context) {
    if (Platform.isIOS) {
      if (entryBuilder != null) {
        return entryBuilder(
          context,
          [null, 'download'],
          () {
            installApp(context);
          },
          null,
        );
      }
      return _boxBuilder(context, BkIcons.download, () {
        installApp(context);
      }, 'download');
    }
    return Selector2<DownloadProvider, PkgProvider, ArtifactoryInfo>(
      selector: (context, downloadProvider, pkgProvider) {
        DownloadJob task;
        int progress = 0;
        bool isInstalled = false;
        DownloadTaskStatus status;
        String taskId;
        String bundleIdentifier;
        if (downloadProvider != null &&
            downloadProvider.downloadJobs.isNotEmpty) {
          task = downloadProvider.downloadJobs.find<DownloadJob>(
            (DownloadJob task) {
              return (task.jobInfo != null &&
                  (task.jobInfo.expId == artifactory.identify ||
                      task.jobInfo.expId == artifactory.uniqueId));
            },
          );
        }

        if (task != null) {
          progress = task.progress;
          status = task.status;
          taskId = task.taskId;
          bundleIdentifier = task.jobInfo.bundleIdentifier;
          isInstalled = (pkgProvider.installedPkgs ?? []).any(
            (element) =>
                task.jobInfo.bundleIdentifier == element.bundleIdentifier,
          );
        }

        return ArtifactoryInfo(
          taskId: taskId,
          status: status,
          progress: progress,
          bundleIdentifier: bundleIdentifier,
          isInstalled: isInstalled,
        );
      },
      builder: (context, value, child) {
        final childProps = _getIconByStatus(context, value);
        if (entryBuilder != null) {
          return entryBuilder(
            context,
            childProps,
            () {
              _handlePressed(context, value);
            },
            value,
          );
        }
        return _boxBuilder(
          context,
          childProps[0],
          () {
            _handlePressed(context, value);
          },
          childProps[1],
        );
      },
    );
  }
}
