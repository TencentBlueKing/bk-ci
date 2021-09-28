import 'package:bkci_app/models/experience.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/widgets/AppIcon.dart';
import 'package:bkci_app/widgets/DownloadButton.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';

class ExperienceListItem extends StatelessWidget {
  const ExperienceListItem({
    Key key,
    @required this.item,
    @required this.onTap,
  }) : super(key: key);

  final Experience item;
  final Function onTap;

  @override
  Widget build(BuildContext context) {
    final textStyle = TextStyle(
      color: '#979BA5'.color,
      fontSize: 24.px,
      height: 34.px / 24.px,
    );
    final int size = item.size;
    return ListTile(
      onTap: () {
        onTap(context, item.experienceHashId);
      },
      tileColor: Colors.white,
      contentPadding: EdgeInsets.fromLTRB(32.px, 24.px, 32.px, 20.px),
      leading: AppIcon(
        url: item.logoUrl,
      ),
      title: PFMediumText(
        item.versionTitle ?? '--',
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          color: Colors.black,
          fontSize: 28.px,
          height: 40.px / 28.px,
        ),
      ),
      subtitle: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: EdgeInsets.only(bottom: 4.px),
            child: PFText(
              item.experienceName ?? '--',
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: textStyle,
            ),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                flex: 1,
                child: PFText(
                  '${size.mb}',
                  style: textStyle,
                ),
              ),
              Expanded(
                flex: 2,
                child: PFText(
                  '版本：${item.version}',
                  style: textStyle,
                ),
              )
            ],
          ),
        ],
      ),
      trailing: DownloadButton(
        expId: item.experienceHashId,
        bundleIdentifier: item.bundleIdentifier,
        createTime: item.createDate,
        name: item.experienceName,
        logoUrl: item.logoUrl,
        size: size,
        lastDownloadHashId: item.lastDownloadHashId,
        appScheme: item.appScheme,
      ),
    );
  }
}
