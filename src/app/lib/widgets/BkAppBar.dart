import 'package:bkci_app/main.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

import 'PFText.dart';

class BkAppBar extends StatelessWidget with PreferredSizeWidget {
  @override
  final Size preferredSize;
  final Color shadowColor;
  final Color backgroundColor;
  final String title;
  final List<Widget> actions;

  BkAppBar({
    Key key,
    this.title,
    this.actions,
    this.backgroundColor = Colors.white,
    this.shadowColor,
  })  : preferredSize = Size.fromHeight(100.px),
        super(key: key);

  void goBack() {
    DevopsApp.navigatorKey.currentState.pop();
  }

  @override
  Widget build(BuildContext context) {
    return AppBar(
      brightness: Brightness.light,
      automaticallyImplyLeading: false,
      shadowColor: shadowColor,
      backgroundColor: backgroundColor,
      titleSpacing: -8.px,
      leadingWidth: 88.px,
      title: PFMediumText(
        title,
        style: TextStyle(
          fontSize: 36.px,
          color: Colors.black,
        ),
      ),
      leading: GestureDetector(
        onTap: goBack,
        child: Container(
          color: Colors.white,
          child: Icon(
            BkIcons.returnIcon,
            size: 44.px,
            color: Colors.black,
          ),
        ),
      ),
      actions: (actions ?? [])
          .map(
            (widget) => Padding(
              padding: EdgeInsets.only(right: 32.px),
              child: widget,
            ),
          )
          .toList(),
    );
  }
}
