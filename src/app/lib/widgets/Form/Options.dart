import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter/cupertino.dart';

class Options extends StatefulWidget {
  final dynamic value;
  final List options;
  final bool multiple;
  final bool isArray;
  final Function handleChange;

  Options({
    this.value,
    this.options,
    this.multiple = false,
    this.isArray = false,
    this.handleChange,
  });

  @override
  _OptionsState createState() => _OptionsState();
}

class _OptionsState extends State<Options> {
  dynamic tmpValue;

  @override
  void initState() {
    super.initState();
    if (widget.multiple) {
      if (widget.isArray || widget.value is List) {
        tmpValue = widget.value.map((item) => item).toList();
      } else {
        tmpValue = widget.value != '' ? widget.value.split(',') : [];
      }
    } else {
      tmpValue = widget.value;
    }
  }

  _handleItemSelet(val) {
    setState(() {
      if (widget.multiple) {
        tmpValue.indexOf(val) >= 0 ? tmpValue.remove(val) : tmpValue.add(val);
      } else {
        tmpValue = val;
      }
      dynamic value =
          (widget.multiple && !widget.isArray) ? tmpValue.join(',') : tmpValue;
      widget.handleChange(value);
    });
  }

  List<Widget> optionListWidget() {
    List<Widget> widgets = [];
    widget.options.forEach((item) {
      widgets.add(selectItem(item));
      widgets.add(Divider(height: 1.px));
    });
    return widgets;
  }

  Widget selectItem(item) {
    return Container(
      child: ListTile(
        contentPadding: EdgeInsets.symmetric(horizontal: 0.px),
        title: PFText(
          item['value'],
          overflow: TextOverflow.ellipsis,
          maxLines: 5,
          style: TextStyle(
            fontSize: 28.px,
          ),
        ),
        trailing: Offstage(
          offstage: !(widget.multiple
              ? tmpValue.indexOf(item['key']) >= 0
              : tmpValue == item['key']),
          child: Container(
            width: 80.px,
            child: Center(
              child: Icon(
                BkIcons.checkSmall,
                color: Theme.of(context).primaryColor,
              ),
            ),
          ),
        ),
        onTap: () => {this._handleItemSelet(item['key'])},
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Column(children: optionListWidget());
  }
}
