import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/utils/i18n.dart';

class InfoList extends StatelessWidget {
  InfoList({Key key, this.infoMap});

  final Map<String, dynamic> infoMap;

  @override
  Widget build(BuildContext context) {
    final clipList = infoMap['productOwner'].map<Widget>(
      (item) {
        return new SizedBox(
          height: 36.px,
          child: Container(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(6.px),
              color: '#E1ECFF'.color,
            ),
            padding: EdgeInsets.fromLTRB(10.px, 4.px, 10.px, 0.px),
            child: PFText(
              item,
              style: TextStyle(
                fontSize: 22.px,
                color: Theme.of(context).primaryColor,
              ),
            ),
          ),
        );
      },
    ).toList();

    Widget productOwnerWidget = Container(
      padding: EdgeInsets.fromLTRB(0, 24.px, 32.px, 24.px),
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(
              // 设置单侧边框的样式
              color: Theme.of(context).dividerColor,
              width: .5.px,
              style: BorderStyle.solid),
        ),
      ),
      child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            PFText(
              BkDevopsAppi18n.of(context).$t('productOwner') ?? key,
              style: TextStyle(
                fontSize: 28.px,
                color: Colors.black,
              ),
            ),
            Container(
              width: 494.px,
              child: Wrap(
                spacing: 8.px,
                runSpacing: 8.px,
                runAlignment: WrapAlignment.start,
                alignment: WrapAlignment.end,
                children: clipList,
              ),
            ),
          ]),
    );

    List<Widget> widgets = [];

    infoMap.forEach(
      (key, value) {
        if (key != 'productOwner') {
          widgets.add(
            Container(
              padding: EdgeInsets.fromLTRB(0, 24.px, 32.px, 24.px),
              decoration: BoxDecoration(
                border: Border(
                  bottom: BorderSide(
                      color: Theme.of(context).dividerColor,
                      width: .5.px,
                      style: BorderStyle.solid),
                ),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  PFText(
                    BkDevopsAppi18n.of(context).$t(key),
                    style: TextStyle(
                      fontSize: 28.px,
                      height: 40.px / 28.px,
                      color: Colors.black,
                    ),
                  ),
                  Container(
                    width: 494.px,
                    child: PFText(
                      value ?? '',
                      textAlign: TextAlign.end,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: 28.px,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          );
        }
      },
    );

    widgets.add(productOwnerWidget);

    return Container(
      alignment: Alignment.topLeft,
      padding: EdgeInsets.fromLTRB(32.px, 24.px, 0.px, 24.px),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          PFMediumText(
            BkDevopsAppi18n.of(context).$t('applicationInfo'),
            style: TextStyle(
              fontSize: 28.px,
            ),
          ),
          Column(children: widgets)
        ],
      ),
    );
  }
}
