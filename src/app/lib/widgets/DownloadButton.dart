import 'dart:io';
import 'dart:ui';

import 'package:bkci_app/providers/DownloadProvider.dart';
import 'package:bkci_app/providers/BkGlobalStateProvider.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/ConfirmDialog.dart';
import 'package:bkci_app/widgets/Loading.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/BkDialog.dart';
import 'package:bkci_app/widgets/ProgressBar.dart';
import 'package:connectivity/connectivity.dart';
import 'package:device_apps/device_apps.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_downloader/flutter_downloader.dart';
import 'package:minimize_app/minimize_app.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

class ButtonConf {
  final String label;
  final Color bgColor;
  final Color fontColor;
  final Color borderColor;

  ButtonConf({
    this.label,
    this.bgColor,
    this.borderColor,
    this.fontColor,
  });
}

class DownloadBtnInfo {
  final String taskId;
  final int progress;
  final DownloadTaskStatus status;
  final String bundleIdentifier;
  final APP_STATUS appDownloadStatus;
  final bool isInstalledExp;
  final bool needUpgrade;

  DownloadBtnInfo({
    this.taskId,
    this.progress,
    this.status,
    this.bundleIdentifier,
    this.appDownloadStatus,
    this.isInstalledExp,
    this.needUpgrade,
  });
}

class ErrorConfirmAction {
  final String label;
  final Function press;

  ErrorConfirmAction({
    this.label,
    this.press,
  });
}

class DownloadButton extends StatefulWidget {
  final String expId;
  final String bundleIdentifier;
  final String logoUrl;
  final String name;
  final int createTime;
  final int size;
  final bool isInDownloadRecordsTab;
  final bool isInNeedUpgradeTab;
  final bool expired;
  final String appScheme;
  final String lastDownloadHashId;

  DownloadButton({
    this.expId,
    this.bundleIdentifier,
    this.name,
    this.logoUrl,
    this.createTime,
    this.size,
    this.isInDownloadRecordsTab = false,
    this.isInNeedUpgradeTab = false,
    this.expired = false,
    this.appScheme,
    this.lastDownloadHashId = '',
  });

  @override
  _DownloadButtonState createState() => _DownloadButtonState();
}

class _DownloadButtonState extends State<DownloadButton> {
  final double width = 120.px;
  final double height = 48.px;
  final List<String> upgradeTheme = [
    '#3A84FF',
    '#3A84FF',
    '#FFFFFF',
  ];

  final List<String> openTheme = [
    '#E1ECFF',
    '#E1ECFF',
    '#3A84FF',
  ];

  final List<String> expireTheme = [
    '#F0F1F5',
    '#F0F1F5',
    '#63656E',
  ];

  final List<String> defaultTheme = [
    '#FFFFFF',
    '#3A84FF',
    '#3A84FF',
  ];

  Future _handlePressed(
    BuildContext context,
    DownloadBtnInfo task,
  ) async {
    final downloadProvider =
        Provider.of<DownloadProvider>(context, listen: false);

    switch (task.appDownloadStatus) {
      case APP_STATUS.RUNNING:
      case APP_STATUS.ENQUEUED:
        BkLoading.of(context).during(downloadProvider.pause(task.taskId));
        break;
      case APP_STATUS.PAUSED:
        BkLoading.of(context).during(downloadProvider.resume(task.taskId));
        break;
      case APP_STATUS.FAILED:
        BkLoading.of(context).during(downloadProvider.retry(task.taskId));
        break;
      case APP_STATUS.INSTALL:
        BkLoading.of(context).during(downloadProvider.open(task.taskId));
        break;
      case APP_STATUS.OPEN:
        openAndroidApp(context, task, downloadProvider);
        break;
      case APP_STATUS.EXPIRE:
        // expired
        break;
      default:
        downloadConfirm(context, () async {
          await download(downloadProvider);
          setDownloadStorage();
        });
    }
  }

  ButtonConf getBtnConf([DownloadBtnInfo task, APP_STATUS appDownloadStatus]) {
    String i18nLabel = 'download';
    List<String> theme = defaultTheme;

    switch (appDownloadStatus) {
      case APP_STATUS.FAILED:
      case APP_STATUS.UNKNOWN:
      case APP_STATUS.CANCELED:
        i18nLabel = downloadStatusLabel[task?.status?.value] ?? 'unknow';
        break;
      case APP_STATUS.INSTALL:
        i18nLabel = 'install';
        break;
      case APP_STATUS.UPGRADE:
        theme = upgradeTheme;
        i18nLabel = 'upgrade';
        break;
      case APP_STATUS.OPEN:
        theme = openTheme;
        i18nLabel = 'open';
        break;
      case APP_STATUS.EXPIRE:
        theme = expireTheme;
        i18nLabel = 'expired';
        break;
      default:
        i18nLabel = 'download';
        theme = defaultTheme;
    }

    return ButtonConf(
      label: i18nLabel,
      bgColor: theme[0].color,
      borderColor: theme[1].color,
      fontColor: theme[2].color,
    );
  }

  Future download(downloadProvider) async {
    await downloadProvider.downloadExp(
      widget.bundleIdentifier,
      widget.expId,
      widget.name,
      widget.logoUrl,
      widget.createTime,
      widget.size,
    );
    await downloadProvider.getAllDownloadTasks();
  }

  Future downloadConfirm(BuildContext context, callback) async {
    ConnectivityResult connectivity = await Connectivity().checkConnectivity();
    if (connectivity == ConnectivityResult.mobile) {
      showDialog(
        context: context,
        builder: (context) {
          return ConfirmDialog(
            height: 260.px,
            confirmLabel: BkDevopsAppi18n.of(context).$t('download'),
            contentMsg:
                "${BkDevopsAppi18n.of(context).$t('networkConfirmTips')}（${widget.size.mb}）${BkDevopsAppi18n.of(context).$t('flow')}",
            confirm: () {
              return BkLoading.of(context).during(callback());
            },
          );
        },
      );
    } else {
      BkLoading.of(context).during(callback());
    }
  }

  installApp(BuildContext context) {
    downloadConfirm(context, () async {
      try {
        final downloadProvider =
            Provider.of<DownloadProvider>(context, listen: false);
        final downloadUrl = await downloadProvider.getDownloadUrl(widget.expId);
        final itmsUrl = getItmsUrl(downloadUrl.url);
        final supported = await canLaunch(itmsUrl);

        if (supported) {
          await launch(itmsUrl);
          await setDownloadStorage();
          MinimizeApp.minimizeApp();
        }
      } catch (e) {
        print('获取下载地址失败, $e');
      }
    });
  }

  Future setDownloadStorage() {
    final provider = Provider.of<BkGlobalStateProvider>(context, listen: false);
    final futures = [
      provider.setLastDownloadExpId(widget.bundleIdentifier, widget.expId),
    ];
    if (Platform.isIOS) {
      futures.add(
          provider.setInstalledExpId(widget.bundleIdentifier, widget.expId));
    }
    return Future.wait(futures);
  }

  Future openIOSApp(BuildContext context) async {
    try {
      if (widget.appScheme is String) {
        String urlScheme = '${widget.appScheme}://';
        final result = await launch(urlScheme);
        if (!result) {
          throw ('App无法唤起, $urlScheme');
        }
      } else {
        throw ('app Scheme格式非法, ${widget.appScheme}');
      }
    } catch (e) {
      _showSchemeErrorAlert(
        context,
        widget.expired
            ? BkDevopsAppi18n.of(context).$t('expExpiredTips')
            : BkDevopsAppi18n.of(context).$t('reDownloadTips'),
        widget.expired
            ? [
                ErrorConfirmAction(
                  label: BkDevopsAppi18n.of(context).$t('confirm'),
                ),
              ]
            : [
                ErrorConfirmAction(
                  label: BkDevopsAppi18n.of(context).$t('confirm'),
                  press: () {
                    installApp(context);
                  },
                ),
                ErrorConfirmAction(
                  label: BkDevopsAppi18n.of(context).$t('cancel'),
                ),
              ],
      );
    }
  }

  Future openAndroidApp(
    BuildContext context,
    DownloadBtnInfo task,
    DownloadProvider downloadProvider,
  ) async {
    try {
      final bool canOpen = await DeviceApps.openApp(task.bundleIdentifier);
      if (!canOpen) {
        Provider.of<BkGlobalStateProvider>(context, listen: false)
            .removeInstalledExpByBundleId(task.bundleIdentifier);
        throw ('无法唤起App');
      }
    } catch (e) {
      print('e， $e');
      toast(BkDevopsAppi18n.of(context).$t('openRemoveAppError'));
      return;
      // return showDialog<void>(
      //   context: context,
      //   barrierDismissible: false, // user must tap button!
      //   builder: (BuildContext context) {
      //     return BkDialog(
      //       height: 260.px,
      //       title: BkDevopsAppi18n.of(context).$t('openError'),
      //       content: PFText(
      //         BkDevopsAppi18n.of(context).$t('openRemoveAppError'),
      //       ),
      //       actions: [
      //         ErrorConfirmAction(
      //           label: BkDevopsAppi18n.of(context).$t('confirm'),
      //           press: () {
      //             Future.microtask(() {
      //               downloadConfirm(context, () async {
      //                 await downloadProvider.removeDownloadByTaskId(
      //                     task.taskId, true);
      //                 await download(downloadProvider);
      //                 setDownloadStorage();
      //               });
      //             });
      //           },
      //         ),
      //         ErrorConfirmAction(
      //           label: BkDevopsAppi18n.of(context).$t('cancel'),
      //         ),
      //       ],
      //     );
      //   },
      // );
    }
  }

  Future<void> _showSchemeErrorAlert(
      BuildContext context, String content, List actions) async {
    return showDialog<void>(
      context: context,
      barrierDismissible: false, // user must tap button!
      builder: (BuildContext context) {
        return BkDialog(
          height: 480.px,
          title: BkDevopsAppi18n.of(context).$t('openError'),
          content: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              for (final i in [0, 1, 2, 3])
                PFText(
                  BkDevopsAppi18n.of(context).$t('openErrorContentDesc$i'),
                  style: TextStyle(
                    fontSize: 26.px,
                  ),
                ),
              PFMediumText(
                '\n$content',
                style: TextStyle(
                  fontSize: 26.px,
                ),
              ),
            ],
          ),
          actions: actions,
        );
      },
    );
  }

  Widget btnBuilder(BuildContext context, Function onPressed,
      {DownloadBtnInfo task, APP_STATUS appDownloadStatus}) {
    final btnConf = getBtnConf(task, appDownloadStatus);
    return OutlinedButton(
      style: OutlinedButton.styleFrom(
        padding: EdgeInsets.symmetric(horizontal: 0),
        backgroundColor: btnConf.bgColor ?? Colors.white,
        side: BorderSide(
          width: 0,
          color: btnConf.borderColor ?? Theme.of(context).primaryColor,
          style: BorderStyle.solid,
        ),
        shape: StadiumBorder(),
      ),
      onPressed: appDownloadStatus == APP_STATUS.EXPIRE ? null : onPressed,
      child: PFBoldText(
        BkDevopsAppi18n.of(context).$t(btnConf.label),
        style: TextStyle(
          color: btnConf.fontColor ?? Theme.of(context).primaryColor,
          fontSize: 22.px,
        ),
      ),
    );
  }

  Widget buildProgressBar(BuildContext context, int progress, bool isPause) {
    return ClipPath(
      clipper: ShapeBorderClipper(
        shape: const StadiumBorder(),
      ),
      child: Container(
        width: width,
        height: height,
        alignment: Alignment.center,
        decoration: ShapeDecoration(
          color: Colors.white,
          shape: StadiumBorder(
            side: BorderSide(
              width: 1.px,
              color: Theme.of(context).primaryColor,
              style: BorderStyle.solid,
            ),
          ),
        ),
        child: ProgressBar(value: progress, isPause: isPause),
      ),
    );
  }

  Widget _boxBuilder(BuildContext context, Function onPressed,
      [DownloadBtnInfo task]) {
    final bool isRunning = [
      APP_STATUS.RUNNING,
      APP_STATUS.PAUSED,
      APP_STATUS.ENQUEUED,
    ].contains(task?.appDownloadStatus);
    return SizedBox(
      width: width,
      height: height,
      child: isRunning
          ? GestureDetector(
              onTap: onPressed,
              child: buildProgressBar(
                context,
                task?.progress,
                APP_STATUS.PAUSED == task?.appDownloadStatus,
              ),
            )
          : btnBuilder(
              context,
              onPressed,
              task: task,
              appDownloadStatus: task?.appDownloadStatus,
            ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (Platform.isIOS) {
      return Selector<BkGlobalStateProvider, APP_STATUS>(
        selector: (context, globalStateProvider) {
          return globalStateProvider.getDownloadStatus(
            expired: widget.expired ?? false,
            currentExpId: widget.expId,
            bundleId: widget.bundleIdentifier,
            lastDownloadHashId: widget.lastDownloadHashId,
          );
        },
        builder: (context, appDownloadStatus, child) => SizedBox(
          width: width,
          height: height,
          child: btnBuilder(
            context,
            () {
              switch (appDownloadStatus) {
                case APP_STATUS.OPEN:
                  openIOSApp(context);
                  break;
                default:
                  installApp(context);
                  break;
              }
            },
            appDownloadStatus: appDownloadStatus,
          ),
        ),
      );
    }
    return Selector2<DownloadProvider, BkGlobalStateProvider, DownloadBtnInfo>(
      selector: (context, downloadProvider, globalStateProvider) {
        DownloadJob task;

        int progress = 0;
        DownloadTaskStatus status;
        String taskId;

        if (downloadProvider != null &&
            downloadProvider.downloadJobs.isNotEmpty) {
          task = downloadProvider.downloadJobs.find<DownloadJob>(
            (DownloadJob task) {
              return (task.jobInfo != null &&
                  task.jobInfo.expId == widget.expId);
            },
          );
        }
        if (task != null) {
          progress = task.progress;
          status = task.status;
          taskId = task.taskId;
        }

        return DownloadBtnInfo(
          taskId: taskId,
          progress: progress,
          status: status,
          bundleIdentifier: widget.bundleIdentifier,
          appDownloadStatus: globalStateProvider.getDownloadStatus(
            expired: widget.expired ?? false,
            currentExpId: widget.expId,
            bundleId: widget.bundleIdentifier,
            lastDownloadHashId: widget.lastDownloadHashId,
            task: task,
          ),
        );
      },
      builder: (context, value, child) {
        return _boxBuilder(context, () {
          _handlePressed(context, value);
        }, value);
      },
    );
  }
}
