import 'package:bkci_app/main.dart';
import 'package:bkci_app/models/experience.dart';
import 'package:bkci_app/pages/DetailScreen.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/AppListItem.dart';
import 'package:bkci_app/widgets/AppStoreListItem.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import '../utils/util.dart';
import 'package:bkci_app/utils/util.dart';

class ListItem extends StatelessWidget {
  final List<Experience> sublist;
  ListItem(this.sublist);

  void goDetail(String expId) {
    DetailScreenArgument args = DetailScreenArgument(expId: expId);
    DevopsApp.navigatorKey.currentState
        .pushNamed(DetailScreen.routePath, arguments: args);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Column(
        children: sublist
            .map((item) {
              List<Widget> widgets = [];
              int index = sublist.indexOf(item);
              if (item.isAppStore) {
                widgets.add(AppStoreListItem(
                  id: item.experienceHashId,
                  bundleIdentifier: item.bundleIdentifier,
                  title: item.experienceName,
                  leadingUrl: item.logoUrl,
                  subTitle: BkDevopsAppi18n.of(context).$t('goAppStoreTips'),
                  createTime: item.createTime,
                  externalUrl: item.externalUrl,
                ));
              } else {
                widgets.add(AppListItem(
                  onTap: goDetail,
                  id: item.experienceHashId,
                  bundleIdentifier: item.bundleIdentifier,
                  title: item.experienceName,
                  leadingUrl: item.logoUrl,
                  subTitle: item.subTitle,
                  createTime: item.createTime,
                  size: item.size,
                  lastDownloadHashId: item.lastDownloadHashId,
                  expired: item.expired,
                  appScheme: item.appScheme,
                ));
              }
              if (index != sublist.length - 1) {
                widgets.add(Divider(
                  indent: 150.px,
                ));
              }
              return widgets;
            })
            .expand((item) => item)
            .toList(),
      ),
    );
  }
}

class ExpHorizontalList extends StatelessWidget {
  final String title;
  final Function actionTap;
  final List<Experience> list;

  ExpHorizontalList({this.title, this.actionTap, this.list});

  @override
  Widget build(BuildContext context) {
    final collapseList = list.collapse(3);
    return Column(
      children: [
        GestureDetector(
          behavior: HitTestBehavior.opaque,
          child: Padding(
            padding: EdgeInsets.fromLTRB(32.0.px, 8.px, 32.px, 0.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                PFMediumText(
                  this.title,
                  style: TextStyle(
                    fontSize: 32.px,
                  ),
                ),
                IconButton(
                  icon: Icon(BkIcons.right),
                  color: '#979BA5'.color,
                  onPressed: actionTap,
                ),
              ],
            ),
          ),
          onTap: actionTap,
        ),
        Container(
          height: 260,
          child: ListView.builder(
            physics: BouncingScrollPhysics(),
            controller: ScrollController(),
            itemCount: collapseList.length,
            itemExtent: MediaQuery.of(context).size.width * .95,
            scrollDirection: Axis.horizontal,
            itemBuilder: (BuildContext context, int index) {
              return ListItem(collapseList[index]);
            },
          ),
        ),
      ],
    );
  }
}
