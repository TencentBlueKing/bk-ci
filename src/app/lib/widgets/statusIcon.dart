import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class StatusIcon extends StatelessWidget {
  final IconData icon;
  final int size;
  final Color iconColor;
  final bool isLoading;

  StatusIcon({
    @required this.icon,
    this.size = 33,
    this.iconColor,
    this.isLoading,
  });

  @override
  Widget build(BuildContext context) {
    final primaryColor = Theme.of(context).primaryColor;

    if (isLoading) {
      return SizedBox(
        width: size.px,
        height: size.px,
        child: CupertinoActivityIndicator(
          radius: (size / 2).px,
        ),
      );
    }

    return Icon(
      icon,
      size: size.px,
      color: iconColor ?? primaryColor,
    );
  }
}
