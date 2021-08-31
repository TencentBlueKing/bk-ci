import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:bkci_app/utils/constants.dart';
import 'package:package_info/package_info.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:version/version.dart';

enum LOGIN_TYPE {
  INTERNAL,
  EXTERNAL,
}

const String LOGIN_TYPE_KEY = 'LOGIN_TYPE';
const String IS_APP_VERSION_POPUP_SHOW = 'IS_APP_VERSION_POPUP_SHOW';
const String IS_GUIDE_SHOW = 'IS_GUIDE_SHOW';

class Storage {
  static SharedPreferences storage;
  static String cKey;
  static String appVersion;
  static LOGIN_TYPE loginType;
  static String cKeyFieldName;
  static String cKeyRequestHeaderFieldName;
  static int platfrom = Platform.isAndroid ? 1 : 2;

  static Future init() async {
    storage = await SharedPreferences.getInstance();
    PackageInfo packageInfo = await PackageInfo.fromPlatform();
    final _ckey = storage.getString(CKEY_HEAD_FIELD);
    if (cKeyValid(_ckey)) {
      Storage.cKey = _ckey;
    }
    final lt = storage.getString(LOGIN_TYPE_KEY) ??
        LOGIN_TYPE.INTERNAL.index.toString();

    Storage.loginType = LOGIN_TYPE.values[int.tryParse(lt)];
    setCkeyField();
    if (packageInfo.version.isNotEmpty) {
      appVersion = packageInfo.version;
    }
  }

  static setCkeyField() {
    switch (Storage.loginType) {
      case LOGIN_TYPE.INTERNAL:
        Storage.cKeyFieldName = 'cKey';
        Storage.cKeyRequestHeaderFieldName = CKEY_HEAD_FIELD;
        break;
      case LOGIN_TYPE.EXTERNAL:
        Storage.cKeyFieldName = 'oToken';
        Storage.cKeyRequestHeaderFieldName = X_OTOKEN_FIELD;
        break;
    }
  }

  static cKeyValid(val) {
    return val != null && val != '' && val != 'null' && val is String;
  }

  static Future setString(String key, String value) async {
    if (key == CKEY_HEAD_FIELD) {
      updateCkey(value);
    }
    final result = await storage.setString(key, value);
    return result;
  }

  static getString(String key) {
    return storage.getString(key);
  }

  static Future setList(String key, List<String> value) async {
    final result = await storage.setStringList(key, value);
    return result;
  }

  static getList<StringList>(String key) {
    List res = storage.getStringList(key);
    return res;
  }

  static updateCkey(String key) {
    Storage.cKey = key;
  }

  static setLoginType(LOGIN_TYPE loginType) {
    Storage.setString(LOGIN_TYPE_KEY, loginType.index.toString());
    Storage.loginType = loginType;
    setCkeyField();
  }

  static hasCkey() {
    final _ckey = storage.getString(CKEY_HEAD_FIELD);
    return storage.containsKey(CKEY_HEAD_FIELD) && cKeyValid(_ckey);
  }

  static Future<void> setDownloadedExpMapByBundleId({
    String bundleId,
    bool isAdd = true,
    String expId,
  }) async {
    Map downloadedExpMap = getDownloadededExpMap();
    if (isAdd) {
      downloadedExpMap = {
        ...downloadedExpMap,
        '$bundleId': expId,
      };
    } else {
      downloadedExpMap.remove(bundleId);
    }

    await Storage.setString(
      DOWNLOADED_EXP_IDS_KEY,
      jsonEncode(downloadedExpMap),
    );
  }

  static getDownloadededExpMap() {
    final downloadedExpMapStr = Storage.getString(DOWNLOADED_EXP_IDS_KEY);
    return downloadedExpMapStr != null
        ? jsonDecode(downloadedExpMapStr)
        : Map();
  }

  static getDownloadedExpById(String bundleId) {
    final downloadedExpMap = getDownloadededExpMap();

    return downloadedExpMap[bundleId] ?? null;
  }

  static Future<void> setInstalledExpMapByBundleId({
    String bundleId,
    bool isAdd = true,
    String expId,
  }) async {
    Map installedExpMap = getInstalledExpMap();
    if (isAdd) {
      installedExpMap = {
        ...installedExpMap,
        '$bundleId': expId,
      };
    } else {
      installedExpMap.remove(bundleId);
    }

    await Storage.setString(
      INSTALLED_EXP_IDS_KEY,
      jsonEncode(installedExpMap),
    );
  }

  static getCurrentInstallingExpId() {
    return Storage.getString(CURRENT_INSTALLING_EXP_ID_KEY);
  }

  static setCurrentInstallingExpId(String expId) {
    return Storage.setString(CURRENT_INSTALLING_EXP_ID_KEY, expId);
  }

  static getInstalledExpMap() {
    final installedExpMapStr = Storage.getString(INSTALLED_EXP_IDS_KEY);
    return installedExpMapStr != null ? jsonDecode(installedExpMapStr) : Map();
  }

  static getInstalledExpById(String bundleId) {
    final installedExpMap = getInstalledExpMap();

    return installedExpMap[bundleId] ?? null;
  }

  static bool isAppUpdatePopupShowed(String versionId) {
    return storage.getBool('${IS_APP_VERSION_POPUP_SHOW}_$versionId') ?? false;
  }

  static setAppUpdatePopupShowed(String versionId) async {
    storage
        .getKeys()
        .where(
          (value) => value.startsWith(IS_APP_VERSION_POPUP_SHOW),
        )
        .toList()
        .map(
          (e) => storage.remove(e),
        );
    return storage.setBool('${IS_APP_VERSION_POPUP_SHOW}_$versionId', true);
  }

  static setGuideShowed() {
    return storage.setString(IS_GUIDE_SHOW, Storage.appVersion);
  }

  static bool get getGuideShowed {
    try {
      final currentAppSemver = Version.parse(Storage.appVersion);
      final showedAppSemver = Version.parse(storage.getString(IS_GUIDE_SHOW));

      return currentAppSemver.major == showedAppSemver.major;
    } catch (e) {
      print(e);
      return false;
    }
  }

  static Map get settingConf {
    try {
      final settingConfStr = storage.getString(SETTING_CONF_MAP_KEY);
      return jsonDecode(settingConfStr);
    } catch (e) {
      return null;
    }
  }

  static FutureOr<bool> setSettingConf(Map val) {
    return storage.setString(SETTING_CONF_MAP_KEY, jsonEncode(val));
  }
}
