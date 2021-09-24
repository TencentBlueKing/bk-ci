import 'dart:io';
import 'dart:typed_data';
import 'dart:ui';

import 'package:bk_tencent_share/bk_tencent_share.dart';
import 'package:bkci_app/models/ShareArgs.dart';
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

enum expTypeEnum {
  Artifactory,
  PublicExp,
  PrivateExp,
}

class ExpQRCode extends StatelessWidget {
  static const String routePath = '/qrcode';
  final ShareArgs args;

  ExpQRCode({this.args});

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
      label: 'QQ',
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

    Uint8List sourceBytes = await _capturePic(context, args);
    final qRFileName = 'qrcode_${args.title.toLowerCase()}.png';
    final String picPath = await saveImageToFile(
      context,
      sourceBytes,
      qRFileName,
    );
    final shareArgs = ShareArgs(
      kind: 'image',
      title: args.title,
      description: args.description,
      fileName: qRFileName,
      previewImageUrl: picPath,
      url: args.url,
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
        loading.during(_saveImage(context, sourceBytes)).whenComplete(() {
          toast(BkDevopsAppi18n.of(context).$t('saveImageSucc'));
        });
    }
  }

  Future<void> _saveImage(BuildContext context, Uint8List sourceBytes) {
    return ImageGallerySaver.saveImage(sourceBytes);
  }

  Future _capturePic(BuildContext context, ShareArgs args) async {
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
    String savePath;
    if (Platform.isAndroid) {
      Directory dir = await getExternalStorageDirectory();
      savePath = dir.path;
    } else {
      Directory dir = await getTemporaryDirectory();
      savePath = dir.path;
    }

    String storagePath = '$savePath/$qRFileName';
    File file = new File(storagePath);

    if (!file.existsSync()) {
      file.createSync();
    }
    file.writeAsBytesSync(sourceBytes);

    return storagePath;
  }

  Widget qrcodeFooterBuilder(BuildContext context) {
    String tip = BkDevopsAppi18n.of(context).$t('bkdevopsQrScanTips');
    final TextStyle style = TextStyle(
      color: Colors.white,
      fontSize: 26.px,
    );

    final children = [
      PFText(tip, style: style),
    ];

    if (!args.isArtifact) {
      String validPeriod = args.endDate.yyMMdd;
      String suffix = BkDevopsAppi18n.of(context).$t('qrcodeValidPeriod');
      children.add(PFText(validPeriod + suffix, style: style));
    }

    return Opacity(
      opacity: args.isArtifact ? 1 : 0.6,
      child: Container(
        alignment: Alignment.center,
        margin: EdgeInsets.only(top: 84.px),
        height: 147.px,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: children,
        ),
      ),
    );
  }

  int getExpTypeConst() {
    return (args.isArtifact
            ? expTypeEnum.Artifactory
            : args.isPublicExperience
                ? expTypeEnum.PublicExp
                : expTypeEnum.PrivateExp)
        .index;
  }

  Container buildLabel(BuildContext context) {
    int expType = getExpTypeConst();

    List<String> bgColor = [
      '#B1B1B1',
      '#3A84FF',
      '#E79A34',
    ];
    List<String> expTypeLabelList = [
      'artifact',
      'publicExp',
      'privateExp',
    ];

    return Container(
      alignment: Alignment.topRight,
      child: Container(
        decoration: BoxDecoration(
          color: bgColor[expType].color,
          borderRadius: BorderRadius.only(
            topRight: Radius.circular(14.px),
            bottomLeft: Radius.circular(14.px),
          ),
        ),
        padding: EdgeInsets.symmetric(
          vertical: 12.px,
          horizontal: 36.px,
        ),
        child: PFBoldText(
          BkDevopsAppi18n.of(context)
              .$t(expTypeLabelList[expType] ?? 'artifact'),
          style: TextStyle(
            color: '#F5F6FA'.color,
            fontSize: 32.px,
          ),
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
              child: Stack(
                children: [
                  Container(
                    decoration: BoxDecoration(
                      image: DecorationImage(
                        image: AssetImage(
                          'assets/images/pack_share_bg.png',
                        ),
                        fit: BoxFit.cover,
                      ),
                      borderRadius: BorderRadius.circular(14.px),
                    ),
                    padding: EdgeInsets.only(left: 80.px, right: 80.px),
                    width: 640.px,
                    height: 1024.px,
                    child: Column(
                      children: [
                        Container(
                          margin: EdgeInsets.only(top: 241.px),
                          width: 360.px,
                          height: 360.px,
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(12.px),
                            color: Colors.white,
                          ),
                          child: QrImage(
                            data: args.url,
                            version: QrVersions.auto,
                            size: 300.0.px,
                            embeddedImage: NetworkImage(
                              args.previewImageUrl,
                            ),
                            embeddedImageStyle: QrEmbeddedImageStyle(
                              size: Size(64.px, 64.px),
                            ),
                          ),
                        ),
                        Container(
                          height: 54.px,
                          margin: EdgeInsets.only(
                            top: 50.px,
                            bottom: 10.px,
                          ),
                          child: PFMediumText(
                            args.title,
                            maxLines: 1,
                            textAlign: TextAlign.center,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 36.px,
                            ),
                          ),
                        ),
                        Expanded(
                          child: Container(
                            alignment: Alignment.center,
                            child: PFText(
                              args.packageName,
                              textAlign: TextAlign.center,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 28.px,
                              ),
                            ),
                          ),
                        ),
                        qrcodeFooterBuilder(context),
                      ],
                    ),
                  ),
                  buildLabel(context),
                ],
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
