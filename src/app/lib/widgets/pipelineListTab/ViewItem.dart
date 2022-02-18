import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class ViewItem extends StatelessWidget {
  final Map viewItem;
  final Function onTap;
  final bool selected;

  ViewItem({this.viewItem, this.onTap, this.selected = false});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 89.px,
      child: ListTile(
        contentPadding: EdgeInsets.symmetric(horizontal: 0.px),
        title: PFText(
          viewItem['name'] ?? '',
          overflow: TextOverflow.ellipsis,
          maxLines: 1,
          style: TextStyle(
            fontSize: 28.px,
          ),
        ),
        trailing: Offstage(
          offstage: !selected,
          child: Container(
            width: 80.px,
            child: Center(
                child: Icon(
              BkIcons.checkSmall,
              color: Theme.of(context).primaryColor,
            )),
          ),
        ),
        onTap: () {
          onTap(viewItem);
        },
      ),
    );
  }
}
