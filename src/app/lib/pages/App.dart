import 'dart:async';
import 'dart:io';

import 'package:badges/badges.dart';
import 'package:bkci_app/pages/Experience.dart';
import 'package:bkci_app/pages/Home.dart';
import 'package:bkci_app/pages/My.dart';
import 'package:bkci_app/pages/Pipeline.dart';
import 'package:bkci_app/providers/BkGlobalStateProvider.dart';
import 'package:bkci_app/providers/CheckUpdateProvider.dart';
import 'package:bkci_app/providers/DownloadProvider.dart';

import 'package:bkci_app/utils/BkQuickAction.dart';
import 'package:bkci_app/utils/Storage.dart';

import 'package:bkci_app/utils/constants.dart';
import 'package:connectivity/connectivity.dart';
import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../widgets/BkIcons.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/util.dart';

class AppTabConf {
  final IconData icon;
  final IconData activeIcon;
  final String title;
  final Widget widget;
  final bool show;

  AppTabConf({
    this.icon,
    this.activeIcon,
    this.title,
    this.widget,
    this.show = true,
  });
}

class BkDevopsApp extends StatefulWidget {
  static const String routePath = '/';

  @override
  _BkDevopsAppState createState() => _BkDevopsAppState();
}

class _BkDevopsAppState extends State<BkDevopsApp> with WidgetsBindingObserver {
  final bkQuickAction = BkQuickAction();

  int _selectedIndex = 0;
  StreamSubscription subscription;

  final List<AppTabConf> tabs = [
    AppTabConf(
      icon: BkIcons.home,
      activeIcon: BkIcons.homeFill,
      title: 'home',
      widget: HomeScreen(),
      show: Storage.loginType == LOGIN_TYPE.INTERNAL,
    ),
    AppTabConf(
      icon: BkIcons.experience,
      activeIcon: BkIcons.experienceFill,
      title: 'experience',
      widget: ExperienceScreen(),
      show: true,
    ),
    AppTabConf(
      icon: BkIcons.pipeline,
      title: 'pipeline',
      widget: PipelineScreen(),
      show: Storage.loginType == LOGIN_TYPE.INTERNAL,
    ),
    AppTabConf(
      icon: BkIcons.user,
      activeIcon: BkIcons.userFill,
      title: 'my',
      widget: MyScreen(),
      show: true,
    ),
  ].where((element) => element.show).toList();

  @override
  void initState() {
    super.initState();
    subscription = Connectivity()
        .onConnectivityChanged
        .listen((ConnectivityResult result) {
      if ([
            ConnectivityResult.mobile,
            ConnectivityResult.none,
          ].contains(result) &&
          Platform.isAndroid) {
        Provider.of<DownloadProvider>(context, listen: false).pauseAll();
      }
    });
    _selectedIndex = Provider.of<BkGlobalStateProvider>(context, listen: false)
            .settings['initRoute'] ??
        0;
    Future.microtask(() {
      Provider.of<CheckUpdateProvider>(context, listen: false).hasNewVersion();
    });

    if (Platform.isIOS) {
      bkQuickAction();
    }
    WidgetsBinding.instance.addObserver(this);
    itloginChannel.setMethodCallHandler(platformCallHandler);

    if (Platform.isAndroid) {
      initUniLink();
    }
  }

  Future initUniLink() async {
    final initUniLink = await itloginChannel.invokeMethod('getInitUniLink');

    if (initUniLink != null) handleUniLink(context, initUniLink);
  }

  Future<void> platformCallHandler(MethodCall call) async {
    switch (call.method) {
      case "uni_link":
        if (call.arguments != null) handleUniLink(context, call.arguments);
        break;
      case "bkPackageInstalled":
      case "bkPackageUpdated":
        final String currentInstallingExpId =
            Storage.getCurrentInstallingExpId();
        if (currentInstallingExpId != null) {
          await Provider.of<BkGlobalStateProvider>(context, listen: false)
              .setInstalledExpId(call.arguments, currentInstallingExpId);
        }
        break;
      case "bkPackageRemove":
        await Provider.of<BkGlobalStateProvider>(context, listen: false)
            .removeInstalledExpByBundleId(call.arguments);
        break;
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    switch (state) {
      case AppLifecycleState.resumed:
        if (mounted) {
          Provider.of<CheckUpdateProvider>(context, listen: false)
              .hasNewVersion();
        }
        Provider.of<BkGlobalStateProvider>(context, listen: false)
            .setAppForegroundState(true);
        break;
      case AppLifecycleState.inactive:
        Provider.of<BkGlobalStateProvider>(context, listen: false)
            .setAppForegroundState(false);
        break;
      case AppLifecycleState.paused:
      case AppLifecycleState.detached:
        break;
    }
  }

  @override
  void dispose() {
    super.dispose();
    subscription.cancel();
    WidgetsBinding.instance.removeObserver(this);
  }

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  Widget _bottomNavBarItemBuilder(AppTabConf tab, [isActive = false]) {
    IconData icon =
        isActive && tab.activeIcon != null ? tab.activeIcon : tab.icon;
    if (tab.title == 'my') {
      return Selector<CheckUpdateProvider, bool>(
        selector: (context, provider) => provider.needUpgrade,
        builder: (context, value, _) => Badge(
          position: BadgePosition(top: 0.px, end: 0),
          toAnimate: false,
          showBadge: value,
          child: Icon(icon),
        ),
      );
    }
    return Icon(icon);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: DefaultTextStyle(
        child: tabs[_selectedIndex].widget,
        style: TextStyle(
          fontSize: 22.px,
          color: Theme.of(context).secondaryHeaderColor,
        ),
      ),
      bottomNavigationBar: CupertinoTabBar(
        backgroundColor: Colors.white,
        items: <BottomNavigationBarItem>[
          for (final tab in tabs)
            if (tab.show)
              BottomNavigationBarItem(
                icon: _bottomNavBarItemBuilder(tab),
                activeIcon: _bottomNavBarItemBuilder(
                  tab,
                  true,
                ),
                label: BkDevopsAppi18n.of(context).$t(
                  tab.title,
                ),
              ),
        ],
        currentIndex: _selectedIndex,
        inactiveColor: Theme.of(context).secondaryHeaderColor,
        activeColor: Theme.of(context).primaryColor,
        onTap: _onItemTapped,
      ),
    );
  }
}
