import 'package:bkci_app/models/userInfo.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

// Mix-in [DiagnosticableTreeMixin] to have access to [debugFillProperties] for the devtool
class User with ChangeNotifier, DiagnosticableTreeMixin {
  UserInfo _user;

  UserInfo get user => _user;

  User() {
    fetchUser();
  }

  Future fetchUser() async {
    final response = await ajax.get('/staffInfo.json');
    final user = UserInfo.fromJson(response.data);
    _user = user;
    notifyListeners();
    return user;
  }

  /// Makes `User` readable inside the devtools by listing all of its properties
  @override
  void debugFillProperties(DiagnosticPropertiesBuilder properties) {
    super.debugFillProperties(properties);
    properties.add(DiagnosticsProperty<UserInfo>('user', user));
  }
}
