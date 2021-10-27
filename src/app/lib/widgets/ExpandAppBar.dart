import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/ExpandText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class ExpandAppBar extends StatelessWidget {
  final Color backgroundColor;
  final String title;
  final Widget actions;

  ExpandAppBar({
    this.title,
    this.actions,
    this.backgroundColor = Colors.white,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: backgroundColor,
      ),
      padding: EdgeInsets.fromLTRB(32.px, 25.px, 32.px, 25.px),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          GestureDetector(
            onTap: () {
              Navigator.pop(context);
            },
            child: Container(
              padding: EdgeInsets.all(5.px),
              child: Icon(
                BkIcons.returnIcon,
                size: 44.px,
                color: Colors.black,
              ),
            ),
          ),
          Expanded(
            child: Container(
              padding: EdgeInsets.fromLTRB(16.px, 4.px, 16.px, 0),
              child: ExpandableText(
                text: title ?? '',
                maxLines: 1,
                style: TextStyle(
                  color: Colors.black,
                  fontSize: 36.px,
                  fontFamily: 'PingFang-medium',
                  height: 54.px / 36.px,
                ),
              ),
            ),
          ),
          Container(
            child: actions,
          ),
        ],
      ),
    );
  }
}
