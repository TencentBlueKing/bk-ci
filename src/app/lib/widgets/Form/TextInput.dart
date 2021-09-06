import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter/cupertino.dart';

class TextInput extends StatefulWidget {
  final String name;
  final String value;
  final String placeholder;
  final int maxLength;
  final int maxLines;
  final bool require;
  final Function handleChange;

  TextInput({
    this.name,
    this.value,
    this.placeholder,
    this.maxLines,
    this.maxLength,
    this.require = false,
    this.handleChange,
  });

  @override
  _TextInputState createState() => _TextInputState();
}

class _TextInputState extends State<TextInput> {
  final TextEditingController _controller = TextEditingController();

  @override
  void initState() {
    super.initState();
    _controller.text = widget.value;
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.only(
        top: 15.px,
        bottom: widget.require && _controller.text == '' ? 5.px : 0,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CupertinoTextField(
            controller: _controller,
            onChanged: (val) {
              widget.handleChange(widget.name, val);
            },
            autocorrect: false,
            cursorColor: Theme.of(context).secondaryHeaderColor,
            cursorHeight: 42.px,
            placeholder: widget.placeholder ?? '',
            placeholderStyle: TextStyle(
              color: Theme.of(context).hintColor,
              fontSize: 28.px,
            ),
            decoration: BoxDecoration(
              color: Colors.white,
            ),
            maxLines: widget.maxLines ?? 1,
            maxLength: widget.maxLength ?? null,
            style: TextStyle(
              fontSize: 28.px,
              color: Theme.of(context).secondaryHeaderColor,
            ),
          ),
          Offstage(
            offstage: !(widget.require && _controller.text == ''),
            child: Container(
              padding: EdgeInsets.only(top: 20.px, left: 5.px),
              child: PFText(
                BkDevopsAppi18n.of(context).$t('notAllowEmpty'),
                style: TextStyle(
                  fontSize: 24.px,
                  color: Colors.red,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
