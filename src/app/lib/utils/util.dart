import 'dart:io' show Platform;
import 'dart:math';

import 'package:bkci_app/pages/ArtifactoryDetail.dart';
import 'package:bkci_app/pages/DetailScreen.dart';
import 'package:bkci_app/pages/ExecuteDetailPage.dart';
import 'package:bkci_app/providers/CheckUpdateProvider.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:hashids2/hashids2.dart';
import 'package:intl/intl.dart';
import 'dart:ui';

import 'package:provider/provider.dart';

const double BASE_WIDTH = 750;

class SizeFit {
  static MediaQueryData _mediaQueryData;
  static double _screenWidth;
  static double _screenHeight;
  static double _deviceRatio;
  static double _ratio;

  static void initialSize(double baseWidth) {
    _mediaQueryData = MediaQueryData.fromWindow(window);

    _screenWidth = _mediaQueryData.size.width;
    _screenHeight = _mediaQueryData.size.height;
    _deviceRatio = _mediaQueryData.devicePixelRatio;

    _ratio = _screenWidth / (baseWidth ?? 750);
  }

  static double px(double number) {
    if (!(_ratio is double || _ratio is int)) {
      SizeFit.initialSize(BASE_WIDTH);
    }
    return number * _ratio;
  }

  static get deviceHeight {
    return _screenHeight;
  }

  static get deviceWidth {
    return _screenWidth;
  }

  static get deviceRatio {
    return _deviceRatio;
  }
}

extension intFit on int {
  double get px {
    return SizeFit.px(this.toDouble());
  }
}

extension doubleFit on double {
  double get px {
    return SizeFit.px(this);
  }
}

extension hexColor on String {
  Color get color {
    final String str = this.replaceFirst('#', '');
    return Color(int.parse('FF$str', radix: 16));
  }
}

extension jsList on List {
  collapse(int len) {
    if (this.length <= 1) {
      return [this];
    }
    final List<List> result = [];
    int index = 1;

    while (index * len < this.length) {
      List temp = this.skip((index - 1) * len).take(len).toList();
      result.add(temp);
      index++;
    }
    List temp = this.skip((index - 1) * len).toList();
    result.add(temp);

    return result;
  }

  T find<T>(bool Function(T item) cb) {
    return this.firstWhere((item) => cb(item), orElse: () => null);
  }
}

MaterialColor createMaterialColor(Color color) {
  List strengths = <double>[.05];
  Map swatch = <int, Color>{};
  final int r = color.red, g = color.green, b = color.blue;

  for (int i = 1; i < 10; i++) {
    strengths.add(0.1 * i);
  }
  strengths.forEach((strength) {
    final double ds = 0.5 - strength;
    swatch[(strength * 1000).round()] = Color.fromRGBO(
      r + ((ds < 0 ? r : (255 - r)) * ds).round(),
      g + ((ds < 0 ? g : (255 - g)) * ds).round(),
      b + ((ds < 0 ? b : (255 - b)) * ds).round(),
      1,
    );
  });
  return MaterialColor(color.value, swatch);
}

String bytesToSize(int bytes, [int round = 2]) {
  if (bytes <= 0 || bytes.isNaN) {
    return '0 B';
  }
  int step = 1024;
  const unit = ['B', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
  int index = log(bytes) ~/ log(step);

  return "${(bytes / pow(step, index)).toStringAsFixed(round)} ${unit[index]}";
}

extension sizeFormat on int {
  String get mb {
    return bytesToSize(this);
  }
}

extension secondStampFormat on int {
  String get yyMMdd {
    return DateFormat('yyyy-MM-dd')
        .format(DateTime.fromMillisecondsSinceEpoch(this));
  }

  String get yMdhms {
    return DateFormat('yyyy-MM-dd  HH:mm:ss')
        .format(DateTime.fromMillisecondsSinceEpoch(this));
  }

  String get yMdhm {
    return DateFormat('yyyy-MM-dd  HH:mm')
        .format(DateTime.fromMillisecondsSinceEpoch(this));
  }

  String get mdhm {
    return DateFormat('MM-dd  HH:mm')
        .format(DateTime.fromMillisecondsSinceEpoch(this));
  }

  String get mmd {
    return DateFormat.MMMMd().format(DateTime.fromMillisecondsSinceEpoch(this));
  }

  String get duration {
    if (this < 1) return '--';
    final duration = Duration(seconds: this);
    final day = duration.inDays > 0
        ? '${duration.inDays}${BkDevopsAppi18n.translate("day")} '
        : '';
    final hour = (duration.inHours % 24) > 0
        ? '${duration.inHours % 24}${BkDevopsAppi18n.translate("hour")} '
        : '';
    final mins = (duration.inMinutes % 60) > 0
        ? '${duration.inMinutes % 60}${BkDevopsAppi18n.translate("minute")} '
        : '';
    final seconds = duration.inSeconds > 0 ? duration.inSeconds % 60 : 0;
    return '$day$hour$mins$seconds${BkDevopsAppi18n.translate("second")}';
  }
}

bool platformMatch(String pkgName) {
  final regStr = Platform.isAndroid ? r'\.(apk|aab|obb)$' : r'\.ipa$';
  final reg = RegExp(
    regStr,
    caseSensitive: false,
  );
  return reg.hasMatch(pkgName);
}

String encodePath(String path) {
  return path.replaceAll(RegExp('/'), '@@');
}

String decodePath(String path) {
  return path.replaceAll(RegExp('@@'), '/');
}

toast(String msg) {
  Fluttertoast.showToast(
    msg: msg,
    gravity: ToastGravity.TOP,
    fontSize: 24.px,
    backgroundColor: Colors.black,
  );
}

void handleUniLink(BuildContext context, String uniLinkUrl) {
  var routeArgs;
  String routeName = '';
  final currentNav = Navigator.of(context);
  final navigate = currentNav.canPop()
      ? currentNav.pushReplacementNamed
      : currentNav.pushNamed;

  final args = uniLinkUrl
      .replaceAll(RegExp('/app/(project|experience)'), '') // hack 旧版本app地址
      .split('/');
  final page = args[1];
  final params = args.sublist(2);
  if (Provider.of<CheckUpdateProvider>(context, listen: false)
      .isUpgradeDialogShow) {
    return;
  }

  switch (page) {
    case 'expDetail':
      routeArgs = DetailScreenArgument(expId: params.last);
      routeName = DetailScreen.routePath;
      break;
    case 'buildDetail':
      final int initialIndex = compatibleTabIndex(params[3]);
      routeArgs = ExecuteDetailPageArgs(
        projectId: params[0],
        pipelineId: params[1],
        buildId: params[2],
        initialIndex: initialIndex,
      );
      routeName = ExecuteDetailPage.routePath;
      break;
    case 'artifactoryDetail':
      routeArgs = ArtifactoryDetailArgs(
        projectId: params[0],
        artifactoryType: params[1],
        artifactoryPath: decodePath(params.last),
      );
      routeName = ArtifactoryDetail.routePath;
      break;
  }
  if (routeName != '') {
    navigate(
      routeName,
      arguments: routeArgs,
    );
  }
}

int compatibleTabIndex(String tabStr) {
  switch (tabStr) {
    case 'archiveTab':
    case '0':
      return 0;
    case 'reportTab':
    case '2':
      return 2;
    default:
      return 0;
  }
}

const String HASH_SALT = "jhy^3(@So0";
const String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
const int HASH_LENGTH = 8;

final expHashIds = HashIds(
  salt: HASH_SALT,
  minHashLength: HASH_LENGTH,
  alphabet: ALPHABET,
);

bool gtExpHashId(String hashId1, String hashId2) {
  final decodeHash1 = expHashIds.decode(hashId1 ?? '');
  final decodeHash2 = expHashIds.decode(hashId2 ?? '');
  final isEmpty = decodeHash1.isEmpty || decodeHash2.isEmpty;

  return isEmpty ? false : decodeHash1.first > decodeHash2.first;
}

String getItmsUrl(String url) {
  final cKey = Storage.cKey;

  final Map<String, String> queryParameters = {
    Storage.cKeyFieldName: cKey,
    ...(isGray ? {'x-devops-project-id': GRAY_PROJECT_ID} : {})
  };

  final String queryStr = Uri(queryParameters: queryParameters).query;
  final addCkeyUrl = '$url&$queryStr';
  return 'itms-services://?action=download-manifest&url=${Uri.encodeComponent(addCkeyUrl)}';
}

String hyphenator(String word) {
  if (word is String) {
    return word.split('').join('\u200b');
  }
  return word;
}
