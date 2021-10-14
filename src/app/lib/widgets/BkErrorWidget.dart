import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/BkAppBar.dart';
import 'package:bkci_app/widgets/NoAuth.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class BkErrorWidget extends StatelessWidget {
  final flutterErrorDetails;
  final String errorMsg;
  final String authTitle;
  final String authDesc;
  final String title;
  final bool withAppBar;

  BkErrorWidget({
    this.flutterErrorDetails,
    this.title,
    this.errorMsg,
    this.authTitle,
    this.authDesc,
    this.withAppBar = true,
  });

  Widget buildErrorMsg(BuildContext context) {
    final String flutterErrorMsg = flutterErrorDetails is FlutterErrorDetails
        ? flutterErrorDetails.exceptionAsString()
        : flutterErrorDetails.toString();
    return Column(
      children: [
        Icon(
          Icons.error,
          color: Colors.red,
          size: 100,
        ),
        Text(
          errorMsg ?? flutterErrorMsg,
          style: TextStyle(color: Colors.blue, fontSize: 26.px),
          textAlign: TextAlign.start,
        )
      ],
    );
  }

  Widget buildContent(BuildContext context) {
    final is403 = flutterErrorDetails is DioError &&
        flutterErrorDetails?.response?.statusCode == 403;
    return is403
        ? NoAuth(
            title: authTitle,
            desc: flutterErrorDetails?.response?.statusMessage is String
                ? (flutterErrorDetails?.response?.statusMessage as String)
                    .replaceAll(RegExp(r'^\[\d+\]'), '')
                : authDesc,
          )
        : SingleChildScrollView(
            child: Container(
              padding: EdgeInsets.only(left: 20.px, right: 20.px, top: 80.px),
              child: buildErrorMsg(context),
            ),
          );
  }

  @override
  Widget build(BuildContext context) {
    if (withAppBar) {
      return Scaffold(
        appBar: BkAppBar(
          title: title ?? BkDevopsAppi18n.translate('error'),
        ),
        body: buildContent(context),
      );
    }

    return buildContent(context);
  }
}
