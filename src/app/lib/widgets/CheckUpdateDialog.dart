import 'dart:io';

import 'package:bkci_app/providers/CheckUpdateProvider.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/ProgressBar.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter/rendering.dart';
import 'package:provider/provider.dart';

class CheckUpdateDialog extends StatelessWidget {
  CheckUpdateDialog({
    Key key,
    @required this.upgrade,
    @required this.content,
    @required this.version,
    @required this.close,
    this.isForceUpgrade = false,
  }) : super(key: key);

  final Future Function(BuildContext context, String text) upgrade;
  final String content;
  final String version;
  final bool isForceUpgrade;
  final Function close;
  Widget _progressBar(BuildContext context, Widget btn) {
    return Selector<CheckUpdateProvider, List>(selector: (context, provider) {
      return [
        provider.isUpgrading,
        provider.upgradeProgress,
        provider.isDownloaded,
      ];
    }, builder: (context, data, child) {
      if (data[2]) {
        Provider.of<CheckUpdateProvider>(context, listen: false)
            .installApk(context);
      }
      if (!data[0]) {
        return btn;
      }
      return Container(
        clipBehavior: Clip.hardEdge,
        decoration: ShapeDecoration(
          shape: StadiumBorder(
            side: BorderSide(
              color: Theme.of(context).primaryColor,
              width: 1.px,
            ),
          ),
        ),
        height: 48.px,
        child: ProgressBar(value: data[1]),
      );
    });
  }

  Widget _footerWidget(BuildContext context) {
    final Widget btn = _upgradeButton(
      context,
      () {
        upgrade(context, version);
      },
    );
    if (Platform.isIOS) return btn;
    return _progressBar(context, btn);
  }

  Widget _upgradeButton(BuildContext context, onPressed) {
    return SizedBox(
      height: 64.px,
      child: ElevatedButton(
        onPressed: onPressed,
        child: PFText(
          BkDevopsAppi18n.of(context).$t('upgradeImmediately'),
          style: TextStyle(
            color: Colors.white,
            fontSize: 30.px,
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: () async => false,
      child: Dialog(
        backgroundColor: Colors.transparent,
        elevation: 0,
        child: ConstrainedBox(
          constraints: BoxConstraints(
            maxHeight: 945.px,
          ),
          child: ConstrainedBox(
            constraints: BoxConstraints(
              maxHeight: 945.px,
            ),
            child: Stack(
              children: [
                Positioned.fill(
                  top: 36,
                  child: Container(
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(
                        4.px,
                      ),
                    ),
                  ),
                ),
                Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Image.asset(
                      'assets/images/update_dialog_bg.png',
                    ),
                    Flexible(
                      child: LayoutBuilder(
                        builder: (context, constraints) {
                          return Scrollbar(
                            key: ValueKey(constraints.maxHeight),
                            isAlwaysShown: true,
                            child: SingleChildScrollView(
                              child: Container(
                                padding: EdgeInsets.fromLTRB(
                                  38.px,
                                  33.px,
                                  30.px,
                                  0,
                                ),
                                child: PFText(
                                  content,
                                  style:
                                      TextStyle(fontSize: 24.px, height: 1.6),
                                ),
                              ),
                            ),
                          );
                        },
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.fromLTRB(
                        38.px,
                        68.px,
                        30.px,
                        isForceUpgrade ? 60.px : 0,
                      ),
                      child: _footerWidget(context),
                    ),
                    isForceUpgrade
                        ? SizedBox()
                        : TextButton(
                            onPressed: close,
                            style: TextButton.styleFrom(
                              padding: EdgeInsets.only(
                                top: 30.px,
                                bottom: 40.px,
                              ),
                            ),
                            child: PFText(
                              BkDevopsAppi18n.of(context).$t('upgradeLater'),
                              style: TextStyle(
                                color: '#999999'.color,
                                fontSize: 22.px,
                              ),
                            ),
                          ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
