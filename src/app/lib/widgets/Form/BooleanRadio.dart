import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter/cupertino.dart';

class BooleanRadio extends StatefulWidget {
  final String name;
  final dynamic value;
  final List options;
  final Function handleChange;

  BooleanRadio({this.name, this.value, this.options, this.handleChange});

  @override
  _BooleanRadioState createState() => _BooleanRadioState();
}

class _BooleanRadioState extends State<BooleanRadio> {
  @override
  Widget build(BuildContext context) {
    return Container(
      child: Row(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          for (final option in widget.options)
            Container(
              padding: EdgeInsets.only(right: 50.px),
              child: Row(
                children: [
                  Radio(
                    value: option['key'],
                    groupValue: widget.value,
                    activeColor: Theme.of(context).primaryColor,
                    onChanged: (val) {
                      FocusScope.of(context).requestFocus(FocusNode());
                      widget.handleChange(widget.name, val);
                    },
                  ),
                  PFText(
                    option['value'],
                    style: TextStyle(
                      fontSize: 28.px,
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}
