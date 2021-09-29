import 'package:bkci_app/widgets/AppIcon.dart';
import 'package:bkci_app/widgets/DownloadButton.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class AppListItem extends StatelessWidget {
  final String id;
  final String bundleIdentifier;
  final String leadingUrl;
  final String title;
  final String subTitle;
  final String trailing;
  final int createTime;
  final Function onTap;
  final int size;
  final bool isInDownloadRecordsTab;
  final bool isInNeedUpgradeTab;
  final String lastDownloadHashId;
  final String appScheme;
  final bool expired;

  AppListItem({
    this.id,
    this.bundleIdentifier,
    this.leadingUrl,
    this.title,
    this.subTitle,
    this.trailing,
    this.createTime,
    this.onTap,
    this.size,
    this.isInDownloadRecordsTab = false,
    this.isInNeedUpgradeTab = false,
    this.lastDownloadHashId,
    this.appScheme,
    this.expired,
  });

  void _onTap() {
    if (onTap != null) {
      onTap(this.id);
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListTile(
      onTap: onTap != null ? _onTap : null,
      leading: AppIcon(url: leadingUrl),
      title: PFMediumText(
        title,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          fontSize: 28.px,
          color: '#000000'.color,
        ),
      ),
      subtitle: PFText(
        subTitle,
        style: TextStyle(
          fontSize: 24.px,
          color: '#979BA5'.color,
        ),
      ),
      trailing: DownloadButton(
        logoUrl: leadingUrl,
        bundleIdentifier: bundleIdentifier,
        createTime: createTime,
        size: size,
        expId: id,
        name: title,
        lastDownloadHashId: lastDownloadHashId,
        expired: expired,
        appScheme: appScheme,
        isInDownloadRecordsTab: isInDownloadRecordsTab,
        isInNeedUpgradeTab: isInNeedUpgradeTab,
      ),
    );
  }
}
