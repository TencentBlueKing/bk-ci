import 'package:bkci_app/main.dart';
import 'package:bkci_app/pages/QRScanScreen.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:flutter/material.dart';
import 'package:quick_actions/quick_actions.dart';

const String SCAN_ACTION_CONST = 'SCAN_QRCODE';

class BkQuickAction {
  final QuickActions quickActions = QuickActions();
  static List<ShortcutItem> qucickActionList = <ShortcutItem>[
    ShortcutItem(
      type: SCAN_ACTION_CONST,
      localizedTitle: BkDevopsAppi18n.translate('scan'),
      icon: 'quickScan',
    ),
  ];
  call() {
    quickActions.setShortcutItems(qucickActionList);
    quickActions.initialize(handleAction);
  }

  handleAction(String actionType) {
    if (actionType == SCAN_ACTION_CONST) {
      NavigatorState navigatorState = DevopsApp?.navigatorKey?.currentState;
      final currentRoute =
          ModalRoute.of(navigatorState.context)?.settings?.name;
      if (currentRoute != QRScanScreen.routePath) {
        navigatorState.pushNamed(QRScanScreen.routePath);
      }
    }
  }
}
