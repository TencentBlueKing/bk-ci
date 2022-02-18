import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class ShareTypeItem {
  final Widget icon;
  final String label;
  final String platform;

  ShareTypeItem({
    this.icon,
    this.label,
    this.platform,
  });
}

class ShareGrid extends StatelessWidget {
  final ShareTypeItem item;
  final Border border;
  final Function(ShareTypeItem item) onTap;

  ShareGrid({
    this.item,
    this.onTap,
    this.border,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        onTap(item);
      },
      child: Column(
        children: [
          Container(
            alignment: Alignment.center,
            margin: EdgeInsets.only(bottom: 14.px),
            width: 120.px,
            height: 120.px,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(24.px),
              border: border,
              color: Colors.white,
            ),
            child: item.icon,
          ),
          Container(
            height: 34.px,
            alignment: Alignment.center,
            child: PFText(
              item.label,
              style: TextStyle(
                fontSize: 24.px,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
