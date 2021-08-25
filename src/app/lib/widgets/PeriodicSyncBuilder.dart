import 'dart:async';

import 'package:bkci_app/providers/PollProvider.dart';
import 'package:bkci_app/routes/routes.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

class PeriodicSyncBuilder<T> extends StatefulWidget {
  final Future<T> Function() future;
  final Duration duration;
  final Widget child;
  final T initialData;
  final FutureOr<bool> Function(T, T) shouldContinueTask;
  final Function(BuildContext, dynamic) catchError;
  PeriodicSyncBuilder({
    this.future,
    this.duration,
    this.initialData,
    this.child,
    this.shouldContinueTask,
    this.catchError,
  }) : assert(child != null);

  @override
  _PeriodicSyncBuilderState<T> createState() => _PeriodicSyncBuilderState<T>();
}

class _PeriodicSyncBuilderState<T> extends State<PeriodicSyncBuilder<T>>
    with WidgetsBindingObserver, RouteAware {
  PollDataModel<T> provider;
  @override
  void initState() {
    super.initState();
    provider = PollDataModel<T>(
      value: widget.initialData,
      provider: PollProvider(
        apiRequest: widget.future,
        duration: widget.duration,
        initialData: widget.initialData,
        shouldContinueTask: widget.shouldContinueTask,
      ),
    );
  }

  clearSyncTimer() {
    provider.provider.disposeStreams();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);
    switch (state) {
      case AppLifecycleState.resumed:
        provider.provider.startPolling();
        break;
      case AppLifecycleState.inactive:
      case AppLifecycleState.paused:
      case AppLifecycleState.detached:
        provider.provider.clearPeriodic();
        break;
    }
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    routeObserver.subscribe(this, ModalRoute.of(context));
  }

  @override
  void dispose() {
    super.dispose();
    routeObserver.unsubscribe(this);
    clearSyncTimer();
  }

  @override
  void didPushNext() {
    super.didPushNext();
    provider.provider.clearPeriodic();
  }

  @override
  void didPopNext() {
    super.didPopNext();
    provider.provider.startPolling();
  }

  @override
  Widget build(BuildContext context) {
    return StreamProvider<PollDataModel<T>>.value(
      updateShouldNotify:
          (PollDataModel<T> previous, PollDataModel<T> current) {
        print(
            'rebuild previous: ${previous.value}, ${current.value}, ${previous.value != current.value}');
        return previous.value != current.value;
      },
      value: provider.provider.pollStream,
      catchError: widget.catchError ?? (context, error) => error,
      initialData: provider,
      child: widget.child,
    );
  }
}
