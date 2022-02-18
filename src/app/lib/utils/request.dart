import 'dart:async';
import 'dart:io';

import 'package:bkci_app/main.dart';
import 'package:bkci_app/models/BkResponse.dart';
import 'package:bkci_app/utils/LoginUtil.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:dio/dio.dart';

BaseOptions options = BaseOptions(
  baseUrl: BASE_URL_PREFIX,
  connectTimeout: 5000,
  receiveTimeout: 8000,
  headers: isGray
      ? {
          'X-DEVOPS-PROJECT-ID': GRAY_PROJECT_ID, // 灰度
        }
      : {},
);

class CustomInterceptors extends Interceptor {
  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    print("REQUEST[${options?.method}] => PATH: ${options?.path}");
    if (!Storage.hasCkey() && options.extra['noNeedAuth'] == null) {
      return handler.reject(
        DioError(
          type: DioErrorType.other,
          requestOptions: options,
          response: Response(
            statusCode: 401,
            requestOptions: options,
          ),
        ),
        true,
      );
    }

    options.headers[PLATFROM_HEAD_FIELD] = Storage.platfrom;
    options.headers[APPVERSION_HEAD_FIELD] = Storage.appVersion;
    options.headers[Storage.cKeyRequestHeaderFieldName] = Storage.cKey;
    options.headers['cookie'] = getCookies();
    options.contentType = ContentType.json.value;
    options.responseType = ResponseType.json;
    return super.onRequest(options, handler);
  }

  @override
  Future onResponse(
    Response response,
    ResponseInterceptorHandler handler,
  ) async {
    final int statusCode = response?.statusCode;
    print("RESPONSE[$statusCode] => PATH: ${response?.requestOptions?.path}");
    if (statusCode == 401) {
      return handler.reject(
        DioError(
          type: DioErrorType.response,
          requestOptions: response.requestOptions,
          response: Response(
            statusCode: 401,
            requestOptions: response.requestOptions,
          ),
        ),
        true,
      );
    }

    final bkResult = BkResponse.fromJson(response.data);
    if (bkResult.status != 0) {
      return handler.reject(
        DioError(
          type: DioErrorType.response,
          requestOptions: response.requestOptions,
          response: Response(
            statusCode: 400,
            statusMessage: bkResult.message ?? '请求出错',
            requestOptions: response.requestOptions,
          ),
        ),
        true,
      );
    }

    return handler.resolve(Response(
      data: bkResult.data,
      statusCode: statusCode,
      requestOptions: response.requestOptions,
    ));
  }

  @override
  Future onError(DioError err, ErrorInterceptorHandler handler) async {
    final statusCode = err?.response?.statusCode;

    try {
      if (statusCode == 401) {
        await LoginUtil.logout();
        return;
      }

      final bkResult = BkResponse.fromJson(err?.response?.data);
      final formatErr = DioError(
        type: DioErrorType.response,
        requestOptions: err.requestOptions,
        response: Response(
          statusCode: statusCode ?? 400,
          statusMessage: bkResult.message ?? '请求出错',
          requestOptions: err.requestOptions,
        ),
      );
      showErrorToastMsg(formatErr);
      return super.onError(formatErr, handler);
    } catch (e) {
      print(e);
    }
  }
}

void showErrorToastMsg(DioError e) {
  if (e.type == DioErrorType.connectTimeout) {
    toast(e.requestOptions.path + "连接超时");
  } else if (e.type == DioErrorType.sendTimeout) {
    toast(e.requestOptions.path + "请求超时");
  } else if (e.type == DioErrorType.receiveTimeout) {
    toast(e.requestOptions.path + "响应超时");
  } else if (e.type == DioErrorType.response) {
    toast(e?.response?.statusMessage);
    e.error = e?.response?.statusMessage;
  } else if (e.type == DioErrorType.cancel) {
    toast(e.requestOptions.path + "请求取消");
  } else {
    toast(e.requestOptions.path + "未知错误");
  }
}

String get localeCode {
  String rawLocaleCode =
      BkDevopsAppi18n.of(DevopsApp.navigatorKey.currentContext)
          .locale
          .languageCode;
  if (rawLocaleCode == 'zh') {
    return '$rawLocaleCode-cn';
  }
  return rawLocaleCode;
}

getCookies() {
  String cookieStr = '';
  cookies.forEach((key, value) {
    cookieStr += '$key=$value;';
  });
  cookieStr += 'blueking_language=$localeCode';
  return cookieStr;
}

var ajax = Dio(options);
