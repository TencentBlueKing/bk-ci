import 'package:bkci_app/widgets/ShareGrid.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class ShareMenu extends StatelessWidget {
  final List<ShareTypeItem> shareTypes;
  final Future Function(BuildContext context, String platform) share;

  ShareMenu({
    this.shareTypes,
    this.share,
  });

  @override
  Widget build(BuildContext context) {
    return GridView.count(
      crossAxisCount: 4,
      physics: const NeverScrollableScrollPhysics(),
      childAspectRatio: 120.px / 168.px,
      children: [
        for (final item in shareTypes)
          ShareGrid(
            item: item,
            onTap: (item) {
              share(context, item.platform);
            },
          ),
      ],
    );
  }
}
