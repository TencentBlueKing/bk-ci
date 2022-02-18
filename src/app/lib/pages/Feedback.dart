import 'package:bkci_app/providers/UserProvider.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/BkAppBar.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'dart:io';

import 'package:webview_flutter/webview_flutter.dart';

class FeedbackScreen extends StatefulWidget {
  static const String routePath = '/feedback';
  final String feedbackUri = 'https://support.qq.com/product/34592';
  @override
  _FeedbackScreenState createState() => _FeedbackScreenState();
}

class _FeedbackScreenState extends State<FeedbackScreen> {
  bool loading = false;

  @override
  void initState() {
    super.initState();
    if (Platform.isAndroid) WebView.platform = SurfaceAndroidWebView();
  }

  String getUserQuerys(String username, String avatar) {
    return username != null
        ? 'openid=$username&nickname=$username&avatar=$avatar'
        : '';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: BkAppBar(
          title: BkDevopsAppi18n.of(context).$t('feedback'),
        ),
        body: Stack(
          children: [
            Consumer<User>(
              builder: (BuildContext context, User user, child) {
                return WebView(
                  onPageStarted: (url) {
                    setState(() {
                      loading = true;
                    });
                  },
                  onPageFinished: (url) {
                    setState(() {
                      loading = false;
                    });
                  },
                  javascriptMode: JavascriptMode.unrestricted,
                  initialUrl:
                      '${widget.feedbackUri}?${getUserQuerys(user.user.englishName, user.user.avatars)}',
                );
              },
            ),
            loading
                ? Center(
                    child: CircularProgressIndicator(
                      backgroundColor: Theme.of(context).primaryColor,
                    ),
                  )
                : Container(),
          ],
        ));
  }
}
