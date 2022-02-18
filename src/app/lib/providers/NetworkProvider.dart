import 'dart:async';

import 'package:connectivity/connectivity.dart';

class NetworkProvider {
  StreamSubscription<ConnectivityResult> _subscription;
  StreamController<ConnectivityResult> _networkStatusController =
      StreamController<ConnectivityResult>();

  NetworkProvider() {
    _invokeNetworkStatusListen();
  }

  void _invokeNetworkStatusListen() async {
    final initStatus = await Connectivity().checkConnectivity();
    _networkStatusController.sink.add(initStatus);

    _subscription =
        Connectivity().onConnectivityChanged.listen((networkStatus) {
      _networkStatusController.sink.add(networkStatus);
    });
  }

  void disposeStreams() {
    _subscription?.cancel();
    _networkStatusController?.close();
  }
}
