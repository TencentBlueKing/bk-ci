import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/AppIcon.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:url_launcher/url_launcher.dart';

class AppStoreListItem extends StatelessWidget {
  final String id;
  final String bundleIdentifier;
  final String leadingUrl;
  final String title;
  final String subTitle;
  final int createTime;
  final String externalUrl;

  AppStoreListItem({
    this.id,
    this.bundleIdentifier,
    this.leadingUrl,
    this.title,
    this.subTitle,
    this.externalUrl,
    this.createTime,
  });

  Future<void> _onTap() async {
    if (await canLaunch(externalUrl)) {
      launch(this.externalUrl, forceSafariVC: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListTile(
      onTap: _onTap,
      leading: AppIcon(
        url: leadingUrl,
      ),
      title: PFMediumText(
        title,
        style: TextStyle(
          fontSize: 28.px,
          color: Colors.black,
        ),
      ),
      subtitle: PFText(
        subTitle,
        style: TextStyle(
          fontSize: 24.px,
          color: '#979BA5'.color,
        ),
      ),
      trailing: SizedBox(
        height: 48.px,
        width: 120.px,
        child: OutlinedButton(
          onPressed: _onTap,
          style: OutlinedButton.styleFrom(
            shape: StadiumBorder(),
            side: BorderSide(
              width: 0.8.px,
              color: Theme.of(context).primaryColor,
            ),
          ),
          child: PFMediumText(
            BkDevopsAppi18n.of(context).$t('download'),
            style: TextStyle(
              fontSize: 22.px,
              color: Theme.of(context).primaryColor,
            ),
          ),
        ),
      ),
    );
  }
}
