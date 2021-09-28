import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

enum EmptyPic {
  SEARCH,
  NORMAL,
}

class Empty extends StatelessWidget {
  final String title;
  final String subTitle;
  final EmptyPic pic;

  Empty({
    this.title,
    this.subTitle,
    this.pic,
  });

  String get picName {
    switch (pic) {
      case EmptyPic.NORMAL:
        return 'empty';
      case EmptyPic.SEARCH:
        return 'searchEmpty';
      default:
        return 'empty';
    }
  }

  String get defaultI18nLabel {
    switch (pic) {
      case EmptyPic.NORMAL:
        return 'noData';
      case EmptyPic.SEARCH:
        return 'searchEmptyTips';
      default:
        return 'noData';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      alignment: Alignment.center,
      padding: EdgeInsets.only(left: 30.px, right: 30.px),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Container(
            width: 240.px,
            height: 200.px,
            padding: EdgeInsets.only(
              bottom: 26.px,
            ),
            child: Image(
              image: AssetImage('assets/images/$picName.png'),
              fit: BoxFit.contain,
            ),
          ),
          PFText(
            title ?? BkDevopsAppi18n.of(context).$t(defaultI18nLabel),
            style: TextStyle(
              fontSize: 32.px,
            ),
          ),
          Container(
            padding: EdgeInsets.only(top: 20.px),
            child: PFText(
              subTitle ?? '',
              style: TextStyle(
                fontSize: 32.px,
              ),
            ),
          )
        ],
      ),
    );
  }
}
