import 'dart:math';

import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class ProgressBar extends StatefulWidget {
  final int value;
  final Color bgColor;
  final bool isPause;

  ProgressBar({
    Key key,
    this.value,
    this.bgColor,
    this.isPause = false,
  })  : assert(value != null),
        super(key: key);

  @override
  _ProgressBarState createState() => _ProgressBarState();
}

class _ProgressBarState extends State<ProgressBar> {
  int progress = 0;

  @override
  void initState() {
    super.initState();
    progress = widget.value;
  }

  @override
  void didUpdateWidget(covariant ProgressBar oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.value != widget.value) {
      setState(() {
        progress = widget.value;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final String text = widget.isPause ? '继续' : '$progress%';
    return LayoutBuilder(
      builder: (BuildContext context, BoxConstraints constraints) => Material(
        color: widget.bgColor ?? Colors.transparent,
        child: Stack(
          children: [
            Center(
              child: PFText(
                text,
                style: TextStyle(
                  fontSize: 24.px,
                  color: Colors.blue,
                ),
              ),
            ),
            AnimatedContainer(
              duration: Duration(milliseconds: 188),
              height: constraints.maxHeight,
              width: (max(0, progress) / 100) * constraints.maxWidth,
              clipBehavior: Clip.hardEdge,
              child: OverflowBox(
                alignment: Alignment.topLeft,
                maxWidth: constraints.maxWidth,
                child: Container(
                  alignment: Alignment.center,
                  width: constraints.maxWidth,
                  child: PFText(
                    text,
                    style: TextStyle(
                      fontSize: 24.px,
                      color: widget.isPause ? Colors.blue : Colors.white,
                    ),
                  ),
                ),
              ),
              decoration: widget.isPause
                  ? BoxDecoration(color: '#6CBAFF'.color.withAlpha(48))
                  : BoxDecoration(
                      gradient: LinearGradient(
                        colors: [
                          '#6CBAFF'.color,
                          '#3A84FF'.color,
                        ],
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }
}
