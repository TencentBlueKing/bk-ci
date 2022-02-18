import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class BkCheckbox extends StatelessWidget {
  final bool checked;
  final String activeColor;
  final String defaultColor;
  final Function handleChange;
  final double size;

  BkCheckbox({
    this.checked,
    this.handleChange,
    this.size = 32.0,
    this.activeColor = '#3A84FF',
    this.defaultColor = '#C4C6CC',
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: handleChange != null
          ? () {
              handleChange(!checked);
            }
          : null,
      child: Icon(
        checked ? BkIcons.check : BkIcons.circle,
        size: 52.px,
        color: (checked ? activeColor : defaultColor).color,
      ),
    );
  }
}
