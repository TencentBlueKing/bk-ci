import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/utils/i18n.dart';

class RecommendList extends StatelessWidget {
  RecommendList({Key key, this.list, this.onSelected});

  final List<String> list;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) {
    final itemList = list.map(
      (item) {
        return ListTile(
          title: PFText(
            item,
            textAlign: TextAlign.left,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: 28.px,
            ),
          ),
          contentPadding: EdgeInsets.symmetric(horizontal: 0.0),
          onTap: () {
            onSelected(item);
          },
        );
      },
    );
    final divided = ListTile.divideTiles(
      context: context,
      tiles: itemList,
    ).toList();

    return Container(
      padding: EdgeInsets.fromLTRB(32.px, 38.px, 0, 38.px),
      alignment: Alignment.topLeft,
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(BkDevopsAppi18n.of(context).$t('recommendSearch'),
            style: TextStyle(
                color: Colors.black,
                fontSize: 32.px,
                fontWeight: FontWeight.w700)),
        Column(children: divided)
      ]),
    );
  }
}
