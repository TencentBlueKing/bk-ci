import 'dart:io' show Platform;

import 'package:bkci_app/main.dart';
import 'package:bkci_app/pages/App.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:bkci_app/utils/Storage.dart';

class ITLoginScreen extends StatefulWidget {
  static const String routePath = 'itlogin';

  @override
  _ITLoginScreenState createState() => _ITLoginScreenState();
}

class _ITLoginScreenState extends State<ITLoginScreen>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    if (Storage.hasCkey()) {
      Future.delayed(Duration.zero, goApp);
      return;
    }
    WidgetsBinding.instance.addObserver(this);
    handleCkeyReceive();
  }

  void handleCkeyReceive() {
    itloginChannel.setMethodCallHandler(platformCallHandler);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    switch (state) {
      case AppLifecycleState.resumed:
        if (mounted && Platform.isAndroid) {
          itloginChannel.invokeMethod('checkITLogin');
        }
        break;
      case AppLifecycleState.inactive:
      case AppLifecycleState.paused:
      case AppLifecycleState.detached:
        break;
    }
  }

  Future<void> platformCallHandler(MethodCall call) async {
    switch (call.method) {
      case "loginResult":
        if (Storage.cKeyValid(call.arguments)) {
          await Storage.setString(CKEY_HEAD_FIELD, call.arguments);
          Future.delayed(Duration.zero, goApp);
        }
        break;
    }
  }

  void goApp() {
    final String currentRoute = ModalRoute.of(context).settings.name;

    if (currentRoute == ITLoginScreen.routePath && mounted) {
      DevopsApp.navigatorKey.currentState.pushNamedAndRemoveUntil(
        BkDevopsApp.routePath,
        (route) => false,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      child: Center(
        child: CircularProgressIndicator(
          backgroundColor: Theme.of(context).primaryColor,
        ),
      ),
    );
  }
}
