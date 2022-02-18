import 'package:bk_tencent_share/bk_tencent_share.dart';
import 'package:bkci_app/main.dart';
import 'package:bkci_app/models/ShareArgs.dart';
import 'package:bkci_app/pages/ExpQRCode.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/CupertinoBottomPopup.dart';
import 'package:bkci_app/widgets/Loading.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/ShareGrid.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter/services.dart';

class SharePopup {
  final ShareArgs shareArgs;
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
        BkIcons.qr,
        color: '#979BA5'.color,
        size: 58.5.px,
      ),
      label: BkDevopsAppi18n.translate('generateQRcode'),
      platform: 'qrcode',
    ),
    ShareTypeItem(
      icon: Icon(
        BkIcons.link,
        color: '#979BA5'.color,
        size: 58.5.px,
      ),
      label: BkDevopsAppi18n.translate('copyLink'),
      platform: 'copylink',
    )
  ];
  OverlayEntry _overlayEntry;
  bool isShow = false;

  SharePopup({
    this.shareArgs,
  });

  void _share(ShareTypeItem item) {
    switch (item.platform) {
      case 'wechat':
        BkTencentShare.shareToWechat(shareArgs.toJson());
        break;
      case 'qq':
        BkTencentShare.shareToQQ(shareArgs.toJson());
        break;
      case 'wework':
        BkTencentShare.shareToWework(shareArgs.toJson());
        break;
      case 'qrcode':
        DevopsApp.navigatorKey.currentState.pushNamed(
          ExpQRCode.routePath,
          arguments: shareArgs,
        );
        break;
      case 'copylink':
        BkLoading.of(DevopsApp.navigatorKey.currentContext)
            .during(_copyLink(shareArgs.url))
            .whenComplete(() {
          toast(
            BkDevopsAppi18n.of(DevopsApp.navigatorKey.currentContext)
                .$t('copyLinkSucc'),
          );
        });
        break;
    }
    this._handleDismiss();
  }

  Future _copyLink(String text) async {
    await Clipboard.setData(ClipboardData(text: text));
  }

  show(BuildContext context) {
    final OverlayState overlayState = Overlay.of(context);
    _overlayEntry = _createOverlayEntry();
    overlayState.insert(_overlayEntry);
  }

  _handleDismiss() {
    _overlayEntry.remove();
  }

  OverlayEntry _createOverlayEntry() {
    return OverlayEntry(
        builder: (BuildContext context) => contentBuilder(context));
  }

  Widget shareTypeBuilder(BuildContext context, ShareTypeItem item) {
    return ShareGrid(
      item: item,
      onTap: _share,
      border: Border.all(width: .5, color: Theme.of(context).dividerColor),
    );
  }

  Widget contentBuilder(BuildContext context) {
    return CupertinoBottomPopup(
      width: 686.px,
      height: 636.px,
      show: true,
      quickClose: true,
      onDismiss: _handleDismiss,
      child: Container(
        padding: EdgeInsets.fromLTRB(40.px, 38.px, 40.px, 38.px),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            PFMediumText(
              BkDevopsAppi18n.of(context).$t('shareTo'),
              style: TextStyle(
                fontSize: 36.px,
                height: 54.px / 36.px,
              ),
            ),
            Expanded(
              child: GridView.count(
                padding: EdgeInsets.only(top: 38.px),
                mainAxisSpacing: 42.px,
                crossAxisSpacing: 40.px,
                crossAxisCount: 4,
                childAspectRatio: 120 / 168,
                children: shareTypes
                    .map((item) => shareTypeBuilder(context, item))
                    .toList(),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
