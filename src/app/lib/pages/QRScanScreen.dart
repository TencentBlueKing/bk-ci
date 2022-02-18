import 'dart:io' show Platform;

import 'package:bkci_app/pages/ArtifactoryDetail.dart';
import 'package:bkci_app/pages/DetailScreen.dart';
import 'package:bkci_app/pages/ExecuteDetailPage.dart';
import 'package:bkci_app/utils/i18n.dart';

import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:qr_code_scanner/qr_code_scanner.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:url_launcher/url_launcher.dart';

class QRScanScreen extends StatefulWidget {
  static const String routePath = '/qrscan';
  @override
  _QRScanScreenState createState() => _QRScanScreenState();
}

class _QRScanScreenState extends State<QRScanScreen> {
  final GlobalKey qrKey = GlobalKey();
  QRViewController controller;
  String qrData;
  String initUrl;

  void _onQRViewCreated(QRViewController _controller) {
    controller = _controller;

    _controller.scannedDataStream.listen((scanData) {
      if (qrData != scanData.code) {
        qrData = scanData.code;
        print('扫码成功，结果为：${scanData.code}');
        _openUrl(qrData);
      }
    });
  }

  @override
  void reassemble() {
    super.reassemble();
    if (Platform.isAndroid) {
      controller.pauseCamera();
    } else if (Platform.isIOS) {
      controller.resumeCamera();
    }
  }

  void _openUrl(String url) {
    final uri = Uri.parse(url);
    if (uri.queryParametersAll.containsKey('flag')) {
      parseUrl(uri);
    } else {
      _launchURL(url);
    }
  }

  void parseUrl(Uri uri) {
    switch (uri.queryParameters['flag']) {
      case 'experienceDetail':
        DetailScreenArgument args = DetailScreenArgument(
          expId: uri.queryParameters['experienceId'],
        );
        Navigator.of(context).pushReplacementNamed(
          DetailScreen.routePath,
          arguments: args,
        );
        break;
      case "buildArchive":
      case "buildReport":
        ExecuteDetailPageArgs args = ExecuteDetailPageArgs(
          projectId: uri.queryParameters['projectId'],
          pipelineId: uri.queryParameters['pipelineId'],
          buildId: uri.queryParameters['buildId'],
          initialIndex: uri.queryParameters['flag'] == 'buildArchive' ? 0 : 2,
        );
        Navigator.of(context).pushReplacementNamed(
          ExecuteDetailPage.routePath,
          arguments: args,
        );
        break;
      case 'artifactoryDetail':
        ArtifactoryDetailArgs args = ArtifactoryDetailArgs(
          projectId: uri.queryParameters['projectId'],
          artifactoryPath: decodePath(uri.queryParameters['artifactoryPath']),
          artifactoryType: uri.queryParameters['artifactoryType'],
        );
        Navigator.of(context).pushReplacementNamed(
          ArtifactoryDetail.routePath,
          arguments: args,
        );
        break;
    }
  }

  _launchURL(String url) async {
    if (await canLaunch(url)) {
      try {
        Response response = await Dio().head(url);

        final Uri uri = response.realUri;
        if (uri.queryParametersAll.containsKey('flag')) {
          parseUrl(uri);
        } else {
          throw ('without flag');
        }
      } catch (e) {
        launch(url, forceSafariVC: true);
        Navigator.of(context).pop();
      }
    } else {
      toast('无法识别：$url');
    }
  }

  @override
  void dispose() {
    controller?.dispose();
    qrData = null;
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: <Widget>[
          QRView(
            key: qrKey,
            overlay: QrScannerOverlayShape(
              borderColor: '#3CF9FE'.color,
              overlayColor: Color.fromRGBO(0, 0, 0, 0.7),
              borderLength: 32.px,
              borderWidth: 6.px,
              cutOutSize: 540.px,
            ),
            onQRViewCreated: _onQRViewCreated,
          ),
          Positioned(
            top: 74.px,
            child: Row(
              children: [
                IconButton(
                  icon: Icon(BkIcons.returnIcon),
                  color: '#979BA5'.color,
                  onPressed: () {
                    Navigator.pop(context);
                  },
                ),
                PFMediumText(
                  BkDevopsAppi18n.of(context).$t('scanQrCodeTitle'),
                  style: TextStyle(
                    fontSize: 36.px,
                    color: '#979BA5'.color,
                  ),
                ),
              ],
            ),
          ),
          Positioned(
            top: 1121.px,
            child: Container(
              alignment: Alignment.center,
              width: SizeFit.deviceWidth,
              child: PFText(
                BkDevopsAppi18n.of(context).$t('scanQrCodeTip'),
                style: TextStyle(
                  fontSize: 28.px,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
