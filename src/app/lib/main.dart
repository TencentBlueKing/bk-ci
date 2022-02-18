import 'dart:async';

import 'package:bk_tencent_share/bk_tencent_share.dart';
import 'package:bkci_app/pages/App.dart';
import 'package:bkci_app/pages/GuidePage.dart';
import 'package:bkci_app/pages/LoginScreen.dart';
import 'package:bkci_app/providers/DownloadProvider.dart';
import 'package:bkci_app/providers/ExperienceProvider.dart';
import 'package:bkci_app/providers/HomeProvider.dart';
import 'package:bkci_app/providers/PkgProvider.dart';
import 'package:bkci_app/providers/UserProvider.dart';
import 'package:bkci_app/providers/CheckUpdateProvider.dart';
import 'package:bkci_app/providers/BkGlobalStateProvider.dart';
import 'package:bkci_app/routes/BkNavigatorObserver.dart';
import 'package:bkci_app/utils/AppSetting.dart';
import 'package:bkci_app/utils/DioRetryInterceptor.dart';

import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/providers/ProjectProvider.dart';
import 'package:bkci_app/widgets/BkErrorWidget.dart';
import 'package:bkci_app/widgets/RestartWidget.dart';
import 'package:connectivity/connectivity.dart';
import 'package:dio/dio.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_downloader/flutter_downloader.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';
import './utils/i18n.dart';
import './utils/util.dart';
import './routes/routes.dart';
import 'utils/Storage.dart';
import 'utils/request.dart';

class DevopsApp extends StatefulWidget {
  static final GlobalKey<NavigatorState> navigatorKey =
      new GlobalKey<NavigatorState>();

  static Color primaryColor = '#3A84FF'.color;
  static Color lightGrayColor = '#F0F1F5'.color;

  @override
  _DevopsAppState createState() => _DevopsAppState();
}

class _DevopsAppState extends State<DevopsApp> {
  @override
  Widget build(BuildContext context) {
    final homeRoute =
        Storage.hasCkey() ? BkDevopsApp.routePath : LoginScreen.routePath;
    precacheImage(AssetImage("assets/images/guide_bg.jpg"), context);
    precacheImage(AssetImage("assets/images/guide_0.png"), context);
    return Selector<BkGlobalStateProvider, String>(
      selector: (BuildContext context, BkGlobalStateProvider provider) {
        return provider.settings['locale'];
      },
      builder: (BuildContext context, String localeCode, _) {
        return MaterialApp(
          onGenerateTitle: (context) => BkDevopsAppi18n.of(context).$t('title'),
          localizationsDelegates: [
            BkDevopsAppi18n.delegate,
            GlobalMaterialLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
          ],
          navigatorObservers: [BkNavigatorObserver(), routeObserver],
          localeResolutionCallback: (deviceLocale, supportedLocales) {
            final validDeviceLocale = supportedLocales.where((Locale locale) =>
                locale.languageCode == deviceLocale.languageCode);
            final validLocalLocale = supportedLocales
                .where((Locale locale) => locale.languageCode == localeCode);
            if (validLocalLocale.isNotEmpty) {
              return Locale(localeCode);
            } else if (validDeviceLocale.isNotEmpty) {
              return deviceLocale;
            }

            return const Locale('zh', 'CN');
          },
          locale: Locale(localeCode),
          supportedLocales: AppSetting.localeList.map(
            (e) => Locale(
              e['key'],
              e['countryCode'],
            ),
          ),
          theme: ThemeData(
            cupertinoOverrideTheme: NoDefaultCupertinoThemeData(
              primaryColor: DevopsApp.primaryColor,
            ),
            textSelectionTheme: TextSelectionThemeData(
              cursorColor: DevopsApp.primaryColor,
            ),
            appBarTheme: AppBarTheme(
              brightness: Brightness.light,
              centerTitle: false,
              color: Colors.white,
              actionsIconTheme: IconThemeData(
                color: Colors.black,
              ),
              iconTheme: IconThemeData(
                color: Colors.black,
              ),
              textTheme: TextTheme(
                headline6: TextStyle(
                  color: Colors.black,
                  fontSize: 36.px,
                  fontFamily: 'PingFang-medium',
                ),
              ),
            ),
            elevatedButtonTheme: ElevatedButtonThemeData(
              style: ElevatedButton.styleFrom(
                primary: DevopsApp.primaryColor,
                textStyle: TextStyle(
                  color: Colors.white,
                ),
              ),
            ),
            outlinedButtonTheme: OutlinedButtonThemeData(
              style: OutlinedButton.styleFrom(
                side: BorderSide(
                  color: DevopsApp.primaryColor,
                ),
              ),
            ),
            fontFamily: 'PingFang',
            backgroundColor: DevopsApp.lightGrayColor,
            scaffoldBackgroundColor: DevopsApp.lightGrayColor,
            primarySwatch: createMaterialColor(Colors.white),
            visualDensity: VisualDensity.adaptivePlatformDensity,
            primaryColor: DevopsApp.primaryColor,
            dividerColor: '#DCDEE5'.color,
            hintColor: '#C4C6CC'.color,
            secondaryHeaderColor: '#63656E'.color,
            textButtonTheme: TextButtonThemeData(
              style: ButtonStyle(
                foregroundColor:
                    MaterialStateProperty.all(DevopsApp.primaryColor),
              ),
            ),
            textTheme: TextTheme(
              bodyText1: TextStyle(color: '#63656E'.color),
              bodyText2: TextStyle(color: '#313238'.color),
              subtitle1: TextStyle(color: '#000000'.color),
              subtitle2: TextStyle(color: '#979BA5'.color),
              headline1: TextStyle(color: '#313238'.color),
              headline2: TextStyle(color: '#313238'.color),
            ),
          ),
          navigatorKey: DevopsApp.navigatorKey,
          initialRoute:
              Storage.getGuideShowed ? homeRoute : GuidePage.routePath,
          onGenerateRoute: (RouteSettings routeSettings) =>
              AppRoutes.onGenerateRoute(routeSettings),
          onUnknownRoute: (RouteSettings routeSettings) => MaterialPageRoute(
            builder: AppRoutes.unknowRoute(routeSettings),
          ),
        );
      },
    );
  }
}

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  FlutterError.onError = (FlutterErrorDetails details) async {
    // 转发至 Zone 做统一处理
    Zone.current.handleUncaughtError(details.exception, details.stack);
  };

  ErrorWidget.builder = (FlutterErrorDetails flutterErrorDetails) =>
      BkErrorWidget(flutterErrorDetails: flutterErrorDetails);

  asyncMain();
}

void asyncMain() async {
  await Storage.init();

  ajax.interceptors.addAll([
    LogInterceptor(),
    DioRetryInterceptor(
      dio: ajax,
      connectivity: Connectivity(),
      retryOptions: RetryOptions(
        retryTimes: DEFAULT_RETRY_TIMES,
      ),
    ),
    CustomInterceptors(),
  ]);
  await FlutterDownloader.initialize();
  await BkTencentShare.register(
    WECHAT_APPID,
    UNIVERSAL_LINK,
    QQ_APP_ID,
    WEWORK_APPID,
    WEWORK_CORPID,
    WEWORK_AGENTID,
  );

  runZonedGuarded(() async {
    runApp(
      RestartWidget(
        child: MultiProvider(
          // key: ObjectKey(Storage.cKey),
          providers: [
            ChangeNotifierProvider<BkGlobalStateProvider>(
              create: (_) => BkGlobalStateProvider(),
            ),
            ChangeNotifierProvider<User>(
              create: (_) => User(),
            ),
            ChangeNotifierProvider<HomeProvider>(
              create: (_) => HomeProvider(),
            ),
            ChangeNotifierProvider<ExperienceProvider>(
                create: (_) => ExperienceProvider()),
            ChangeNotifierProvider<ProjectInfoProvider>(
              create: (_) => ProjectInfoProvider(),
            ),
            ChangeNotifierProvider<CheckUpdateProvider>(
              create: (_) => CheckUpdateProvider(),
            ),
            ChangeNotifierProvider<DownloadProvider>(
              create: (_) => DownloadProvider(),
            ),
            // ProxyProvider<DownloadProvider, PkgProvider>(
            //   update: (_, downloadProvider, pkgProvider) {
            //     pkgProvider.checkInstallPkgUpdate();
            //     return;
            //   },
            // ),
            ChangeNotifierProvider<PkgProvider>(
              create: (_) => PkgProvider(),
            ),
          ],
          child: DevopsApp(),
        ),
      ),
    );
  }, (error, stackTrace) async {
    print('global zone error catch, $error, $stackTrace');
  });
}
