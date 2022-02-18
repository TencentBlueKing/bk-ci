import 'package:flutter/material.dart';

const String NAVIGATE_PUSH = 'NAVIGATE_PUSH';
const String NAVIGATE_POP = 'NAVIGATE_POP';
const String NAVIGATE_REPLACE = 'NAVIGATE_REPLACE';
const String NAVIGATE_REMOVE = 'NAVIGATE_REMOVE';

class BkNavigatorObserver extends NavigatorObserver {
  String getRouteName(Route<dynamic> previousRoute) {
    return previousRoute?.settings?.name ?? '';
  }

  @override
  void didPush(Route<dynamic> route, Route<dynamic> previousRoute) {
    print('navigator observe, did push');
    if ((previousRoute is TransitionRoute) && previousRoute.opaque) {
      //全屏不透明，通常是一个page
    } else {
      //全屏透明，通常是一个弹窗
    }
  }

  @override
  void didPop(Route<dynamic> currentRoute, Route<dynamic> nextRoute) {
    super.didPop(currentRoute, nextRoute);
    // var previousName = getRouteName(previousRoute);

    print('navigator observe, did pop');
    // print('YM----->NavObserverDidPop--Current:' +
    //     route.settings.name +
    //     '  Previous:' +
    //     previousName);
  }

  @override
  void didStopUserGesture() {
    super.didStopUserGesture();
  }

  @override
  void didStartUserGesture(Route<dynamic> route, Route<dynamic> previousRoute) {
    super.didStartUserGesture(route, previousRoute);
  }

  @override
  void didReplace({Route<dynamic> newRoute, Route<dynamic> oldRoute}) {
    print('navigator observe, did replace');
    super.didReplace(newRoute: newRoute, oldRoute: oldRoute);
  }

  @override
  void didRemove(Route<dynamic> route, Route<dynamic> previousRoute) {
    print('navigator observe, did remove');
    super.didRemove(route, previousRoute);
  }
}
