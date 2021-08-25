import 'dart:async';

class PollDataModel<T> {
  final T value;
  final provider;

  PollDataModel({
    this.value,
    this.provider,
  }) : assert(provider != null);
}

class PollProvider<T> {
  final Future<T> Function() apiRequest;
  final FutureOr<bool> Function(T, T) shouldContinueTask;
  final Duration duration;
  final T initialData;
  StreamController<PollDataModel<T>> _controller =
      StreamController<PollDataModel<T>>();
  bool _polling = false;
  T _value;
  Timer _timer;

  get pollStream => _controller.stream;
  T get value => _value;

  PollProvider({
    this.apiRequest,
    this.duration,
    this.initialData,
    this.shouldContinueTask,
  }) : assert(apiRequest != null) {
    this._value = initialData;

    this.fetchData();
    print('init polling $initialData');
    if (initialData == null || shouldContinueTask(initialData, null)) {
      this.startPolling();
    }
  }

  void startPolling() {
    print('polling api start, $_timer');
    if (_timer == null) {
      _timer = Timer.periodic(duration, (t) {
        fetchData();
      });
    }
  }

  Future fetchData() async {
    if (this._polling) {
      return;
    }

    this._polling = true;
    try {
      final T res = await apiRequest();
      print('polling api res, $res');
      if (shouldContinueTask is Function &&
          !shouldContinueTask(res, this._value)) {
        print('polling should pause');
        clearPeriodic();
      }
      this._value = res;
      if (!_controller.isClosed) {
        _controller.add(PollDataModel<T>(
          provider: this,
          value: this._value,
        ));
      }
    } catch (e) {
      print('polling error, $e');
    } finally {
      this._polling = false;
    }
  }

  void disposeStreams() {
    print("polling disposeStreams");
    _controller?.close();
    _timer?.cancel();
  }

  void clearPeriodic() {
    print("polling clear timer");
    _timer?.cancel();
    _timer = null;
  }
}
