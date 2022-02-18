import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class BkLoading {
  BuildContext context;

  void hide() {
    Future.delayed(Duration.zero, () {
      Navigator.of(context, rootNavigator: true).pop();
    });
  }

  void show() {
    showDialog(
      context: context,
      barrierDismissible: false,
      useSafeArea: false,
      useRootNavigator: true,
      builder: (context) {
        return WillPopScope(
          onWillPop: () async => false,
          child: Container(
            child: Center(child: CircularProgressIndicator()),
          ),
        );
      },
    );
  }

  Future<T> during<T>(Future<T> future) {
    show();
    return future.whenComplete(() {
      hide();
    });
  }

  BkLoading(this.context);

  factory BkLoading.of(context) {
    return BkLoading(context);
  }
}
