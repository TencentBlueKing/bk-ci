import 'dart:io' show Platform;

import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:bkci_app/utils/util.dart';

const String environment =
    String.fromEnvironment('BK_APP_ENV', defaultValue: '');

final String envPrefix =
    ['', 'gray'].contains(environment) ? '' : '$environment.';
const String GRAY_PROJECT_ID = 'grayproject';
final bool isGray = environment == 'gray';
// ignore: non_constant_identifier_names
final String BASE_URL_PREFIX = 'https://${envPrefix}bkdevops.qq.com';

const String CKEY_HEAD_FIELD = 'X-CKEY';
const String X_OTOKEN_FIELD = 'X-OTOKEN';
const String PLATFROM_HEAD_FIELD = 'X-DEVOPS-PLATFORM';
const String APPVERSION_HEAD_FIELD = 'X-DEVOPS-APP-VERSION';
const MethodChannel itloginChannel = const MethodChannel('flutter.itlogin');
const String EXPERIENCE_API_PREFIX = '/experience/api/app/experiences';
const String ARTIFACTORY_API_PREFIX = '/artifactory/api/app/artifactories';

const String WEWORK_AGENTID = 'xxx';
const String WEWORK_APPID = 'xxxxx';
const String WEWORK_CORPID = 'xxxx';

const String WECHAT_APPID = 'xxxx';
const String UNIVERSAL_LINK = 'xxxxx';
const String QQ_APP_ID = 'xxx';

const String CHOREOGRAPHY_RESOURCE_URL = 'https://bkdevops.qq.com/static/';
const String CURRENT_INSTALLING_EXP_ID_KEY = 'CURRENT_INSTALLING_EXP_ID';
const String INSTALLED_EXP_IDS_KEY = 'INSTALLED_EXP_IDS';
const String DOWNLOADED_EXP_IDS_KEY = 'DOWNLOADED_EXP_IDS';
const String SETTING_CONF_MAP_KEY = 'SETTING_CONF_MAP';

// ignore: non_constant_identifier_names
final String SHARE_URL_PREFIX = '$BASE_URL_PREFIX/share';

const Map WIDGET_TYPE_MAP = {
  'STRING': 'input',
  'BOOLEAN': 'boolean',
  'SVN_TAG': 'select',
  'CODE_LIB': 'select',
  'CONTAINER_TYPE': 'select',
  'ENUM': 'select',
  'ARTIFACTORY': 'select',
  'MULTIPLE': 'multiple',
  'GIT_REF': 'select',
  'SUB_PIPELINE': 'select'
};

class BkCategory {
  static const int GAME = 1;
  static const int TOOL = 2;
  static const int LIFE = 3;
  static const int SOCIAL = 4;
}

const Map<int, String> CATEGORY_MAP = {
  1: 'game',
  2: 'tools',
  3: 'life',
  4: 'social',
};

final iconStatusMap = const {
  'SUCCEED': 'success',
  'STAGE_SUCCESS': 'stageSuc',
  'CANCELED': 'cancel',
  'REVIEW_ABORT': 'cancel',
  'RUNNING': 'loading',
  'QUEUE': 'loading',
  'REVIEWING': 'loading',
  'REVIEW_PROCESSED': 'loading',
  'PREPARE_ENV': 'loading',
  'LOOP_WAITING': 'loading',
  'CALL_WAITING': 'loading',
  'FAILED': 'fail',
  'TERMINATE': 'fail',
  'HEARTBEAT_TIMEOUT': 'fail',
  'QUALITY_CHECK_FAIL': 'fail',
  'QUEUE_TIMEOUT': 'fail',
  'EXEC_TIMEOUT': 'fail'
};

final statuColorMap = {
  'success': '#3FC06D'.color,
  'fail': '#EA3636'.color,
  'cancel': '#FF9C01'.color,
  'stageSuc': '#3FC06D'.color,
};

final Map<String, IconData> iconMap = {
  'success': BkIcons.check,
  'fail': BkIcons.close,
  'cancel': BkIcons.jinzhi,
  'stageSuc': BkIcons.flag,
};

const List<String> downloadStatusLabel = [
  'undefined',
  'enqueued',
  'running',
  'complete',
  'failed',
  'canceled',
  'paused',
];

Map<String, String> cookies = {
  'platform': Platform.isAndroid ? 'ANDROID' : 'IOS',
};

final Map<int, String> expTypeMap = {
  0: '',
  1: '公开体验',
  2: '内部体验',
  3: '公开体验 & 内部体验',
  4: '公开体验',
};

final Map<String, String> platformMap = {'IOS': 'iOS', 'ANDROID': 'Android'};
