import 'dart:io';
import 'dart:typed_data';
import 'dart:ui';

import 'package:bk_tencent_share/bk_tencent_share.dart';
import 'package:bkci_app/models/ShareArgs.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/BkAppBar.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/Loading.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/ShareMenu.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/widgets/ShareGrid.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:image_gallery_saver/image_gallery_saver.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:qr_flutter/qr_flutter.dart';

class ShareQRCode extends StatelessWidget {
  static const String routePath = '/shareApp';

  final List<ShareTypeItem> shareTypes = [
    ShareTypeItem(
      icon: Image.asset(
        "assets/images/wework.png",
        width: 80.px,
      ),
      label: BkDevopsAppi18n.translate('wework'),
      platform: 'wework',
    ),
    ShareTypeItem(
      icon: Image.asset(
        "assets/images/wechat.png",
        width: 80.px,
      ),
      label: BkDevopsAppi18n.translate('wechat'),
      platform: 'wechat',
    ),
    ShareTypeItem(
      icon: Image.asset(
        "assets/images/qq.png",
        width: 80.px,
      ),
      label: BkDevopsAppi18n.translate('QQ'),
      platform: 'qq',
    ),
    ShareTypeItem(
      icon: Icon(
        BkIcons.download,
        color: '#979BA5'.color,
        size: 58.5.px,
      ),
      label: BkDevopsAppi18n.translate('saveImage'),
      platform: 'saveImage',
    ),
  ];

  final GlobalKey globalKey = GlobalKey();
  Future _share(BuildContext context, String platform) async {
    //检查是否有存储权限
    var status = await Permission.storage.status;
    if (!status.isGranted) {
      status = await Permission.storage.request();
      return;
    }

    Uint8List sourceBytes = await _capturePic(context);
    final qRFileName = 'qrcode_bkdevops.png';
    final String picPath = await saveImageToFile(
      context,
      sourceBytes,
      qRFileName,
    );
    final shareArgs = ShareArgs(
      kind: 'image',
      title: 'bkdevops',
      description: '',
      fileName: qRFileName,
      previewImageUrl: picPath,
      url: '',
    ).toJson();

    switch (platform) {
      case 'wechat':
        BkTencentShare.shareToWechat(shareArgs);
        break;
      case 'qq':
        BkTencentShare.shareToQQ(shareArgs);
        break;
      case 'wework':
        BkTencentShare.shareToWework(shareArgs);
        break;
      case 'saveImage':
        final loading = BkLoading.of(context);
        loading.during(_saveImage(context, sourceBytes));
    }
  }

  Future<void> _saveImage(BuildContext context, Uint8List sourceBytes) async {
    await ImageGallerySaver.saveImage(sourceBytes);
    toast(BkDevopsAppi18n.of(context).$t('saveImageSucc'));
  }

  Future _capturePic(BuildContext context) async {
    RenderRepaintBoundary boundary =
        globalKey.currentContext.findRenderObject();
    final image = await boundary.toImage(pixelRatio: SizeFit.deviceRatio);

    ByteData byteData = await image.toByteData(format: ImageByteFormat.png);
    return byteData.buffer.asUint8List();
  }

  Future saveImageToFile(
    BuildContext context,
    Uint8List sourceBytes,
    String qRFileName,
  ) async {
    Directory dir;
    if (Platform.isAndroid) {
      dir = await getExternalStorageDirectory();
    } else {
      dir = await getTemporaryDirectory();
    }

    String storagePath = '${dir.path}/$qRFileName';
    File file = new File(storagePath);

    if (!file.existsSync()) {
      file.createSync();
    }
    file.writeAsBytesSync(sourceBytes);

    return storagePath;
  }

  Widget qrcodeFooterBuilder(BuildContext context) {
    return Expanded(
      child: Container(
        alignment: Alignment.center,
        margin: EdgeInsets.only(top: 45.px),
        padding: EdgeInsets.fromLTRB(60.px, 45.px, 60.px, 56.px),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.only(
            bottomLeft: Radius.circular(15.px),
            bottomRight: Radius.circular(15.px),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              margin: EdgeInsets.only(bottom: 12.px),
              child: PFMediumText(
                BkDevopsAppi18n.of(context).$t('qrcodeTtitle'),
                style: TextStyle(
                  color: Colors.black,
                  fontSize: 36.px,
                  height: 54.px / 36.px,
                ),
              ),
            ),
            PFText(
              BkDevopsAppi18n.of(context).$t('appDesc'),
              style: TextStyle(
                fontSize: 26.px,
                height: 36.px / 26.px,
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: BkAppBar(
        title: BkDevopsAppi18n.of(context).$t('qrcode'),
      ),
      body: ListView(
        physics: const BouncingScrollPhysics(),
        children: [
          Container(
            alignment: Alignment.center,
            margin: EdgeInsets.fromLTRB(55.px, 55.px, 55.px, 0),
            child: RepaintBoundary(
              key: globalKey,
              child: Container(
                decoration: BoxDecoration(
                  image: DecorationImage(
                    image: AssetImage("assets/images/pack_share_bg.png"),
                    fit: BoxFit.cover,
                  ),
                  borderRadius: BorderRadius.circular(14.px),
                ),
                width: 640.px,
                height: 1024.px,
                child: Column(
                  children: [
                    Container(
                      margin: EdgeInsets.only(top: 179.px),
                      width: 360.px,
                      height: 360.px,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(12.px),
                        color: Colors.white,
                      ),
                      child: QrImage(
                        data:
                            '$BASE_URL_PREFIX/app/download/devops_app_download.html',
                        version: QrVersions.auto,
                        size: 300.0.px,
                        embeddedImage: AssetImage(
                          'assets/images/bkdevops_logo.png',
                        ),
                        embeddedImageStyle: QrEmbeddedImageStyle(
                          size: Size(64.px, 64.px),
                        ),
                      ),
                    ),
                    Container(
                      margin: EdgeInsets.only(top: 50.px, bottom: 0.px),
                      alignment: Alignment.center,
                      child: PFText(
                        BkDevopsAppi18n.of(context).$t('scanDownloadAppTips'),
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 26.px,
                        ),
                      ),
                    ),
                    qrcodeFooterBuilder(context),
                  ],
                ),
              ),
            ),
          ),
          Container(
            alignment: Alignment.center,
            margin: EdgeInsets.only(top: 50.px, bottom: 40.px),
            height: 40.px,
            child: PFText(
              BkDevopsAppi18n.of(context).$t('qrcodeShareTips'),
            ),
          ),
          Container(
            margin: EdgeInsets.only(left: 72.px, right: 72.px, bottom: 44.px),
            height: 168.px,
            child: ShareMenu(
              shareTypes: shareTypes,
              share: _share,
            ),
          ),
        ],
      ),
    );
  }
}
