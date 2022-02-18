import 'package:bkci_app/main.dart';
import 'package:bkci_app/pages/ITLoginScreen.dart';
import 'package:bkci_app/pages/MailLoginScreen.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class LoginScreen extends StatefulWidget {
  static const String routePath = 'login';

  @override
  _LoginScreenState createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  void goITLogin() {
    final String currentRoute = ModalRoute.of(context).settings.name;

    if (currentRoute == LoginScreen.routePath && mounted) {
      DevopsApp.navigatorKey.currentState.pushNamedAndRemoveUntil(
        ITLoginScreen.routePath,
        (route) => false,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        color: Colors.white,
        alignment: Alignment.topCenter,
        padding: EdgeInsets.only(
          top: 289.px,
          left: 90.px,
          right: 90.px,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              margin: EdgeInsets.only(
                bottom: 106.px,
              ),
              child: Image.asset(
                "assets/images/appLogo.png",
                width: 130.px,
                height: 130.px,
              ),
            ),
            SizedBox(
              height: 88.px,
              child: ElevatedButton(
                onPressed: () {
                  itloginChannel.invokeMethod('initITLogin');
                  Storage.setLoginType(LOGIN_TYPE.INTERNAL);
                  goITLogin();
                },
                child: PFText(
                  BkDevopsAppi18n.of(context).$t('tencentStaffLogin'),
                  style: TextStyle(
                    color: Colors.white,
                  ),
                ),
              ),
            ),
            SizedBox(height: 30),
            TextButton(
              onPressed: () {
                Storage.setLoginType(LOGIN_TYPE.EXTERNAL);
                Navigator.of(context).pushNamed(MailLoginScreen.routePath);
              },
              child: PFText(
                BkDevopsAppi18n.of(context).$t('tencentParnerLogin'),
                style: TextStyle(
                  color: Theme.of(context).primaryColor,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
