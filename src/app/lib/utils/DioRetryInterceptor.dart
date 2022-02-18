import 'dart:async';
import 'dart:io';

import 'package:connectivity/connectivity.dart';
import 'package:dio/dio.dart';

const int DEFAULT_RETRY_TIMES = 3;
typedef FutureOr<bool> RetryEvaluator(DioError error);

class RetryOptions {
  static const RETRY_EXTRA_KEY = 'CACHE_RETRY_REQUEST_KEY';
  final int retryTimes;
  final Duration retryInterval;
  final RetryEvaluator _retryEvaluator;

  RetryEvaluator get retryEvaluator =>
      this._retryEvaluator ?? defaultRetryEvaluator;

  const RetryOptions({
    this.retryTimes = DEFAULT_RETRY_TIMES,
    RetryEvaluator retryEvaluator,
    this.retryInterval = const Duration(seconds: 2),
  })  : assert(retryTimes != null),
        assert(retryInterval != null),
        this._retryEvaluator = retryEvaluator;

  factory RetryOptions.noRetry() {
    return RetryOptions(retryTimes: 0);
  }

  static FutureOr<bool> defaultRetryEvaluator(DioError error) {
    return [DioErrorType.cancel, DioErrorType.response].indexOf(error.type) ==
        -1;
  }

  factory RetryOptions.fromExtra(RequestOptions request) {
    return request.extra[RETRY_EXTRA_KEY];
  }

  FutureOr<bool> _shouldRetry(DioError err) async {
    return this.retryTimes > 0 && await retryEvaluator(err);
  }

  RetryOptions copyWith({
    int retryTimes,
    Duration retryInterval,
  }) {
    return RetryOptions(
      retryTimes: retryTimes ?? this.retryTimes,
      retryInterval: retryInterval ?? this.retryInterval,
    );
  }

  Map<String, dynamic> toExtra() {
    return {
      RETRY_EXTRA_KEY: this,
    };
  }
}

FutureOr<Response> retryRequest(Dio dio, RequestOptions request) {
  return dio.request(
    request.path,
    cancelToken: request.cancelToken,
    data: request.data,
    onReceiveProgress: request.onReceiveProgress,
    onSendProgress: request.onSendProgress,
    queryParameters: request.queryParameters,
    options: Options(
      extra: request.extra,
    ),
  );
}

class DioRetryInterceptor extends Interceptor {
  final Dio dio;
  final Connectivity connectivity;
  final RetryOptions retryOptions;

  DioRetryInterceptor({
    this.dio,
    this.connectivity,
    retryOptions,
  })  : assert(dio != null),
        assert(connectivity != null),
        this.retryOptions = retryOptions ?? const RetryOptions();

  bool _isSocketException(DioError err) {
    return err.type == DioErrorType.other &&
        err.error != null &&
        err.error is SocketException;
  }

  Future<Response> scheduleRequestRetry(RequestOptions requestOptions) async {
    final responseCompleter = Completer<Response>();
    StreamSubscription streamSubscription;

    streamSubscription =
        connectivity.onConnectivityChanged.listen((connectivityResult) async {
      if (connectivityResult != ConnectivityResult.none) {
        streamSubscription.cancel();
        responseCompleter.complete(retryRequest(dio, requestOptions));
      }
    });
    return responseCompleter.future;
  }

  Future onError(DioError err, ErrorInterceptorHandler handler) async {
    final statusCode = err?.response?.statusCode;

    if (statusCode == 401) {
      return super.onError(err, handler);
    }

    var retryOptions =
        RetryOptions.fromExtra(err.requestOptions) ?? this.retryOptions;

    print('retry interceptor : ${retryOptions.retryTimes}');
    try {
      if (_isSocketException(err)) {
        // 网络错误
        print(
            'retry interceptor no connect ${err.type}, ${err.requestOptions.path}');

        return handler.resolve(await scheduleRequestRetry(err.requestOptions));
      } else if (await retryOptions._shouldRetry(err)) {
        // 是否需要重试
        print(
            "retry interceptor [${err.requestOptions.uri}] An error occured during request, trying a again (remaining tries: ${retryOptions.retryTimes}, error: ${err.error})");
        if (retryOptions.retryInterval.inMilliseconds > 0) {
          await Future.delayed(retryOptions.retryInterval);
        }

        retryOptions =
            retryOptions.copyWith(retryTimes: retryOptions.retryTimes - 1);

        err.requestOptions.extra = err.requestOptions.extra
          ..addAll(retryOptions.toExtra());
        print('retry interceptor : ${retryOptions.retryTimes}');
        final response = await retryRequest(dio, err.requestOptions);
        return handler.resolve(response);
      }
    } catch (e) {
      return handler.next(e);
    }

    return super.onError(err, handler);
  }
}
