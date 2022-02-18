import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/AppIcon.dart';
import 'package:bkci_app/widgets/DownloadButton.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'dart:io';

import 'package:bkci_app/models/experienceDetail.dart';

class Summary extends StatelessWidget {
  Summary({Key key, this.detail});

  final ExperienceDetail detail;

  @override
  Widget build(BuildContext context) {
    String disableText = '';
    if (detail.expired || !detail.online) {
      disableText = BkDevopsAppi18n.of(context).$t('expire');
    }
    if ((detail.platform != 'ANDROID' && Platform.isAndroid) ||
        (detail.platform == 'ANDROID' && Platform.isIOS)) {
      disableText = BkDevopsAppi18n.of(context).$t('platformNotMatch');
    }
    return Container(
      height: 256.px,
      padding: EdgeInsets.fromLTRB(32.px, 40.px, 32.px, 40.px),
      child: Row(
        children: [
          AppIcon(
            width: 176.px,
            height: 176.px,
            url: detail.logoUrl,
          ),
          Container(
            padding: EdgeInsets.fromLTRB(36.px, 16.px, 0, 14.px),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                PFBoldText(
                  detail.experienceName,
                  style: TextStyle(
                    fontSize: 28.px,
                    height: 40.px / 28.px,
                  ),
                ),
                Padding(
                  padding: EdgeInsets.fromLTRB(0, 6.px, 0, 12.px),
                  child: PFText(
                    detail.getPublicType,
                    style: TextStyle(fontSize: 24.px),
                  ),
                ),
                disableText == ''
                    ? DownloadButton(
                        expId: detail.experienceHashId,
                        size: detail.size,
                        logoUrl: detail.logoUrl,
                        createTime: detail.createDate,
                        name: detail.experienceName,
                        bundleIdentifier: detail.bundleIdentifier,
                        appScheme: detail.appScheme,
                        lastDownloadHashId: detail.lastDownloadHashId,
                        expired: detail.expired,
                      )
                    : Container(
                        height: 48.px,
                        width: 200.px,
                        decoration: BoxDecoration(
                          color: Theme.of(context).backgroundColor,
                          borderRadius: BorderRadius.circular(32.px),
                        ),
                        child: Center(
                          child: PFText(
                            disableText,
                            style: TextStyle(
                              fontSize: 22.px,
                            ),
                          ),
                        ),
                      ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
