import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/widgets/BkIcons.dart';

class SearchRecord extends StatefulWidget {
  SearchRecord({Key key, this.list, this.onSelected, this.onClear});

  final List<String> list;
  final ValueChanged<String> onSelected;
  final Function onClear;

  @override
  _RecordState createState() => _RecordState();
}

class _RecordState extends State<SearchRecord> {
  @override
  Widget build(BuildContext context) {
    final clipList = widget.list.map(
      (item) {
        return ConstrainedBox(
          constraints: BoxConstraints(maxWidth: 330.px),
          child: ActionChip(
            backgroundColor: '#F0F1F5'.color,
            label: PFText(
              item,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 26.px,
              ),
            ),
            onPressed: () {
              widget.onSelected(item);
            },
          ),
        );
      },
    ).toList();

    return Column(
      children: [
        Container(
          padding: EdgeInsets.fromLTRB(32.px, 20.px, 32.px, 20.px),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  PFBoldText(
                    BkDevopsAppi18n.of(context).$t('searchRecord'),
                    style: TextStyle(
                      color: Colors.black,
                      fontSize: 32.px,
                    ),
                  ),
                  IconButton(
                    icon: Icon(BkIcons.delete),
                    onPressed: () {
                      widget.onClear();
                    },
                  ),
                ],
              ),
              Wrap(
                spacing: 20.px,
                runSpacing: 0,
                children: clipList,
              ),
            ],
          ),
        ),
        Divider()
      ],
    );
  }
}
