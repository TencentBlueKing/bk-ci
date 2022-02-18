import 'package:bkci_app/main.dart';
import 'package:bkci_app/models/experience.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/AppIcon.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../utils/util.dart';
import 'package:bkci_app/pages/DetailScreen.dart';

class TencentAppList extends StatelessWidget {
  final List<Experience> list;
  final void Function() onTap;
  TencentAppList({@required this.list, @required this.onTap});

  Future<void> goDetail(Experience exp) async {
    if (exp.isAppStore) {
      if (await canLaunch(exp.externalUrl)) {
        launch(exp.externalUrl, forceSafariVC: true);
      }
    } else {
      DetailScreenArgument args =
          DetailScreenArgument(expId: exp.experienceHashId);
      DevopsApp.navigatorKey.currentState
          .pushNamed(DetailScreen.routePath, arguments: args);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(children: [
          PFMediumText(
            BkDevopsAppi18n.of(context).$t('tencentNeeded'),
            style: TextStyle(fontSize: 32.px),
          ),
          Padding(
            padding: EdgeInsets.only(left: 8.px),
            child: PFText(
              BkDevopsAppi18n.of(context).$t('tencentNeededDesc'),
              style: TextStyle(
                fontSize: 24.px,
              ),
            ),
          ),
        ]),
        Container(
          margin: EdgeInsets.only(top: 12.5.px),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              Expanded(
                flex: 1,
                child: Row(
                  children: [
                    for (final Experience item in list)
                      InkWell(
                        onTap: () => goDetail(item),
                        child: Container(
                          margin: EdgeInsets.only(right: 28.px),
                          child: AppIcon(
                            url: item.logoUrl,
                          ),
                        ),
                      )
                  ],
                ),
              ),
              InkWell(
                onTap: onTap,
                child: Container(
                  width: 46.px,
                  height: 100.px,
                  decoration: BoxDecoration(
                    color: '#F5F6FA'.color,
                    borderRadius: BorderRadius.circular(8.0.px),
                  ),
                  child: Icon(BkIcons.right),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
