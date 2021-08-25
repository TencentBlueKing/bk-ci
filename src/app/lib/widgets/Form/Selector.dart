import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/BkIcons.dart';

import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter/cupertino.dart';
import 'package:bkci_app/widgets/CupertinoBottomPopup.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/Form/Options.dart';

class Selector extends StatefulWidget {
  final String name;
  final dynamic value;
  final String label;
  final List options;
  final bool multiple;
  final bool isArray;
  final String placeholder;
  final bool require;
  final Function handleChange;

  Selector({
    this.name,
    this.value,
    this.label,
    this.options,
    this.placeholder,
    this.multiple = false,
    this.isArray = false,
    this.require = false,
    this.handleChange,
  });

  @override
  _SelectorState createState() => _SelectorState();
}

class _SelectorState extends State<Selector> {
  bool popShow;
  dynamic tmpSelectValue;
  String showName;

  final TextEditingController _controller = new TextEditingController();
  OverlayEntry _overlayEntry;

  showPopup(BuildContext context) {
    final OverlayState overlayState = Overlay.of(context);
    _overlayEntry = _createOverlayEntry();
    overlayState.insert(_overlayEntry);
    popShow = true;
  }

  OverlayEntry _createOverlayEntry() {
    return OverlayEntry(
      builder: (BuildContext context) => contentBuilder(context),
    );
  }

  _handleDismiss() {
    setState(() {
      popShow = false;
      _overlayEntry.remove();
    });
  }

  _handleSelectData() {
    this._handleDismiss();
    widget.handleChange(widget.name, tmpSelectValue);
  }

  handleItemChange(val) {
    this.tmpSelectValue = val;
  }

  Widget contentBuilder(BuildContext context) {
    return CupertinoBottomPopup(
      key: ValueKey(widget.name),
      width: 686.px,
      height: 784.px,
      show: popShow,
      quickClose: true,
      onDismiss: _handleDismiss,
      child: Container(
        padding: EdgeInsets.fromLTRB(40.px, 38.px, 40.px, 38.px),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            PFMediumText(
              widget.label ?? '',
              style: TextStyle(
                fontSize: 36.px,
                height: 54.px / 36.px,
              ),
            ),
            Expanded(
              child: SingleChildScrollView(
                child: Options(
                  value: widget.value,
                  options: widget.options,
                  multiple: widget.multiple,
                  isArray: widget.isArray,
                  handleChange: handleItemChange,
                ),
              ),
            )
          ],
        ),
      ),
      footWidget: Container(
        padding: EdgeInsets.all(40.px),
        child: Row(children: [
          GestureDetector(
            onTap: _handleDismiss,
            child: Container(
              width: 300.px,
              alignment: Alignment.center,
              child: PFMediumText(
                BkDevopsAppi18n.of(context).$t('cancel'),
                style: TextStyle(
                  fontSize: 32.px,
                  color: '#3A84FF'.color,
                  height: 48.px / 32.px,
                ),
              ),
            ),
          ),
          Text(
            '|',
            style: TextStyle(color: '#DCDEE5'.color),
          ),
          GestureDetector(
            onTap: _handleSelectData,
            child: Container(
              width: 300.px,
              alignment: Alignment.center,
              child: PFMediumText(
                BkDevopsAppi18n.of(context).$t('confirm'),
                style: TextStyle(
                  fontSize: 32.px,
                  color: '#3A84FF'.color,
                  height: 48.px / 32.px,
                ),
              ),
            ),
          ),
        ]),
      ),
    );
  }

  // 根据选中的key展示相应value
  String getShowName() {
    if (widget.value.length == 0) {
      return '';
    }
    if (!widget.multiple) {
      List regex =
          widget.options.where((item) => item['key'] == widget.value).toList();
      return regex.length > 0 ? regex[0]['value'] : widget.value;
    } else {
      List tmpKeyArr = widget.isArray ? widget.value : widget.value.split(',');
      List<String> tmpNameArr = [];
      tmpKeyArr.forEach((keyItem) {
        List regex =
            widget.options.where((item) => item['key'] == keyItem).toList();
        String name = regex.length > 0 ? regex[0]['value'] : keyItem;
        tmpNameArr.add(name);
      });
      return tmpNameArr.join(',');
    }
  }

  @override
  Widget build(BuildContext context) {
    _controller.text = getShowName();

    return Container(
      padding: EdgeInsets.only(
        bottom: widget.require && _controller.text == '' ? 5.px : 0,
      ),
      child: InkWell(
        onTap: () {
          FocusScope.of(context).requestFocus(FocusNode());
          showPopup(context);
        },
        child: TextField(
          enabled: false,
          controller: _controller,
          style: TextStyle(
            fontSize: 28.px,
            color: Theme.of(context).secondaryHeaderColor,
          ),
          decoration: InputDecoration(
            contentPadding: EdgeInsets.all(10.0),
            hintText: widget.placeholder ?? '',
            hintStyle: TextStyle(
              color: Theme.of(context).hintColor,
              fontSize: 28.px,
            ),
            border: OutlineInputBorder(borderSide: BorderSide.none),
            suffixIcon: Icon(BkIcons.right),
            errorText: (widget.require && _controller.text == '')
                ? BkDevopsAppi18n.of(context).$t('notAllowEmpty')
                : null,
            errorStyle: TextStyle(
              color: Colors.red,
            ),
          ),
        ),
      ),
    );
  }
}
