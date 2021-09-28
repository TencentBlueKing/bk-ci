import 'package:bkci_app/main.dart';
import 'package:bkci_app/pages/LoginScreen.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/widgets/RestartWidget.dart';
import 'package:flutter/material.dart';

class LoginUtil {
  static Future<void> logout() async {
    await clearCkey();
    NavigatorState navigatorState = DevopsApp?.navigatorKey?.currentState;

    final currentRoute = ModalRoute.of(navigatorState.context)?.settings?.name;

    if (currentRoute != LoginScreen.routePath) {
      toast('登录态已失效，请重新登录');

      navigatorState.pushNamedAndRemoveUntil(
        LoginScreen.routePath,
        (route) => false,
      );
      RestartWidget.restartApp(navigatorState.context);
    }
  }

  static Future<void> clearCkey() async {
    await Storage.storage.remove(CKEY_HEAD_FIELD);
    Storage.updateCkey(null);
  }
}
