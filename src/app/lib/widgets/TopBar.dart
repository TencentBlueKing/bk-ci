import 'dart:io' show Platform;
import 'package:badges/badges.dart';
import 'package:bkci_app/main.dart';
import 'package:bkci_app/pages/InstallationManagement.dart';
import 'package:bkci_app/pages/QRScanScreen.dart';
import 'package:bkci_app/pages/SearchScreen.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:flutter/material.dart';
import './BkIcons.dart';
import './CircleIconButton.dart';
import '../utils/util.dart';

class TopBar extends StatelessWidget {
  final NavigatorState currentState = DevopsApp.navigatorKey.currentState;
  void goSearch() {
    currentState.pushNamed(SearchScreen.routePath);
  }

  void goScan(BuildContext context) {
    Navigator.of(context).pushNamed(QRScanScreen.routePath);
  }

  void goInstallationManagement(bool hasPendingUpgradePkg) {
    currentState.pushNamed(
      InstallationManagement.routePath,
      arguments: hasPendingUpgradePkg,
    );
  }

  @override
  Widget build(BuildContext context) {
    var widgets = [
      CircleIconButton(
        width: 68.px,
        height: 68.px,
        iconSize: 44.px,
        icon: BkIcons.scan,
        onPressed: () {
          goScan(context);
        },
        toolTips: BkDevopsAppi18n.of(context).$t('scan'),
      ),
      Expanded(
        child: GestureDetector(
          onTap: goSearch,
          child: Container(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(23),
              color: Colors.white10,
            ),
            height: 68.px,
            alignment: Alignment.centerLeft,
            padding: EdgeInsets.only(left: 24.px),
            margin: EdgeInsets.fromLTRB(24.px, 0, 24.px, 0),
            child: Opacity(
              opacity: 0.4,
              child: Row(
                children: [
                  Icon(
                    BkIcons.search,
                    color: Colors.white,
                    size: 44.px,
                  ),
                  Padding(
                    padding: EdgeInsets.only(left: 16.px),
                    child: Text(
                      BkDevopsAppi18n.of(context).$t('searchExp'),
                      style: TextStyle(color: Colors.white, fontSize: 32.px),
                    ),
                  )
                ],
              ),
            ),
          ),
        ),
      )
    ];

    if (Platform.isAndroid) {
      widgets.add(
        Container(
          width: 68.px,
          height: 68.px,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: Colors.white10,
          ),
          child: Badge(
            position: BadgePosition(end: 0.px, top: 0.px),
            showBadge: false,
            child: GestureDetector(
              child: Icon(
                BkIcons.download,
                color: Colors.white,
              ),
              onTap: () {
                goInstallationManagement(false);
              },
            ),
          ),
        ),
      );
    }

    return Row(children: widgets);
  }
}
