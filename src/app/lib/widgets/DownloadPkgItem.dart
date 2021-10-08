import 'package:bkci_app/widgets/AppIcon.dart';
import 'package:bkci_app/widgets/Checkbox.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class DownloadPkgItem extends StatefulWidget {
  final bool selected;
  final String id;
  final Function onChange;
  final String leadingUrl;
  final String title;
  final String subTitle;
  final int createTime;
  final int size;

  DownloadPkgItem({
    this.id,
    this.selected,
    this.onChange,
    this.leadingUrl,
    this.title,
    this.subTitle,
    this.createTime,
    this.size,
  });

  @override
  _DownloadPkgItemState createState() => _DownloadPkgItemState();
}

class _DownloadPkgItemState extends State<DownloadPkgItem> {
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () {
        widget.onChange(widget.id);
      },
      child: Row(
        children: [
          Container(
            margin: EdgeInsets.only(left: 32.px),
            child: BkCheckbox(
              size: 48.px,
              checked: widget.selected,
            ),
          ),
          AppIcon(
            margin: EdgeInsets.only(left: 32.px),
            url: widget.leadingUrl,
          ),
          Expanded(
            child: Padding(
              padding: EdgeInsets.only(left: 28.px, top: 24.px, bottom: 27.px),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  PFText(
                    widget.title,
                    style: TextStyle(
                      fontSize: 28.px,
                      color: Colors.black,
                    ),
                  ),
                  PFText(
                    widget.subTitle,
                    style: TextStyle(
                      fontSize: 24.px,
                      color: '#979BA5'.color,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
