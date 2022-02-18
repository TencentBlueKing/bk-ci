import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class StatusTag extends StatelessWidget {
  final String status;
  final IconData icon;
  final Color background;
  final bool isLoading;

  StatusTag({
    @required this.icon,
    @required this.status,
    this.isLoading,
    this.background,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 16.px),
      margin: EdgeInsets.symmetric(horizontal: 16.px),
      height: 36.px,
      decoration: BoxDecoration(
        color: background ?? Theme.of(context).primaryColor,
        borderRadius: BorderRadius.circular(
          21.px,
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Padding(
            padding: EdgeInsets.only(
              right: 6.px,
            ),
            child: isLoading
                ? Theme(
                    data: ThemeData(
                      cupertinoOverrideTheme: CupertinoThemeData(
                        brightness: Brightness.dark,
                      ),
                    ),
                    child: CupertinoActivityIndicator(
                      radius: 10.px,
                    ),
                  )
                : Icon(
                    icon,
                    size: 24.px,
                    color: Colors.white,
                  ),
          ),
          PFMediumText(
            BkDevopsAppi18n.of(context).$t(status) ?? '--',
            style: TextStyle(
              color: Colors.white,
              fontSize: 22.px,
            ),
          ),
        ],
      ),
    );
  }
}
