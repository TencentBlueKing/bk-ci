import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class BkDialog extends StatefulWidget {
  final String title;
  final Widget content;
  final List actions;
  final double height;
  const BkDialog({
    Key key,
    this.title,
    this.height,
    this.content,
    this.actions,
  }) : super(key: key);

  @override
  _BkDialogState createState() => _BkDialogState();
}

class _BkDialogState extends State<BkDialog> {
  bool loading = true;
  @override
  Widget build(BuildContext context) {
    return SimpleDialog(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(32.px),
      ),
      title: Center(
        child: PFMediumText(
          widget.title,
          style: TextStyle(
            color: Colors.black,
            fontSize: 36.px,
          ),
        ),
      ),
      contentPadding: EdgeInsets.all(0),
      children: [
        Container(
          height: widget.height ?? 500.px,
          child: Column(
            children: [
              Expanded(
                child: Padding(
                  padding: EdgeInsets.symmetric(
                    vertical: 24.px,
                    horizontal: 63.px,
                  ),
                  child: SingleChildScrollView(
                    child: widget.content,
                  ),
                ),
              ),
              Divider(
                indent: 36.px,
                endIndent: 36.px,
              ),
              SizedBox(
                height: 100.px,
                child: Row(
                  children: [
                    for (final action in widget.actions)
                      Expanded(
                        child: TextButton(
                          child: Center(
                            child: PFMediumText(
                              action.label,
                              style: TextStyle(
                                color: Theme.of(context).primaryColor,
                                fontSize: 28.px,
                              ),
                            ),
                          ),
                          onPressed: () {
                            Navigator.of(context).pop();
                            if (action.press is Function) {
                              action.press();
                            }
                          },
                        ),
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
