import 'package:bkci_app/utils/i18n.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:intl/intl.dart';

class DatePicker extends StatefulWidget {
  final String name;
  final dynamic value;
  final String placeholder;
  final bool require;
  final Function handleChange;

  DatePicker(
      {this.name,
      this.value,
      this.placeholder,
      this.require = false,
      this.handleChange});

  @override
  _DatePickerState createState() => _DatePickerState();
}

class _DatePickerState extends State<DatePicker> {
  String showDate = '';
  DateTime selectDate;

  @override
  void initState() {
    super.initState();
    selectDate = widget.value > 0
        ? DateTime.fromMillisecondsSinceEpoch(widget.value)
        : DateTime.now();
  }

  String _getShowDate() {
    return widget.value > 0
        ? DateFormat('yyyy-MM-dd')
            .format(DateTime.fromMillisecondsSinceEpoch(widget.value))
        : '';
  }

  final TextEditingController _controller = new TextEditingController();

  @override
  Widget build(BuildContext context) {
    _controller.text = _getShowDate();

    return Container(
      padding: EdgeInsets.only(
        bottom: widget.require && _controller.text == '' ? 5.px : 0,
      ),
      child: InkWell(
        onTap: () async {
          dynamic result = await showDatePicker(
            context: context,
            initialDate: selectDate,
            firstDate: DateTime.now(),
            lastDate: DateTime(2120),
            confirmText: '确定',
            cancelText: '取消',
          );
          if (result != null) {
            selectDate = result;
            widget.handleChange(
              widget.name,
              // 加一天减1s
              result.millisecondsSinceEpoch + (24 * 3599 * 1000),
            );
          }
        },
        child: TextField(
          enabled: false,
          controller: _controller,
          decoration: InputDecoration(
            contentPadding: EdgeInsets.all(10.0),
            hintText: widget.placeholder ?? '请选择日期',
            hintStyle: TextStyle(
              color: Theme.of(context).hintColor,
              fontSize: 28.px,
            ),
            border: OutlineInputBorder(borderSide: BorderSide.none),
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
