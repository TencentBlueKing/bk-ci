import 'dart:math';

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';

import 'package:bkci_app/utils/i18n.dart';

class ExpandableText extends StatefulWidget {
  final String text;
  final int maxLines;
  final String expandText;
  final String collapseText;
  final bool expanded;
  final Color linkColor;
  final TextStyle style;
  final TextDirection textDirection;
  final TextAlign textAlign;
  final bool showCollapse;
  final TextSpan customExpandWidget;
  final Size expandWidgetSize;
  final double textScaleFactor;
  final String semanticsLabel;
  final TextStyle linkStyle;
  final Function beforeToggle;

  ExpandableText(
    this.text, {
    Key key,
    this.expandText,
    this.maxLines = 2,
    this.expanded = false,
    this.linkColor,
    this.showCollapse = true,
    this.customExpandWidget,
    this.expandWidgetSize,
    this.collapseText,
    this.style,
    this.textDirection,
    this.textAlign,
    this.textScaleFactor,
    this.semanticsLabel,
    this.linkStyle,
    this.beforeToggle,
  })  : assert(text != null),
        super(key: key);

  @override
  _ExpandableTextState createState() => _ExpandableTextState();
}

class _ExpandableTextState extends State<ExpandableText> {
  bool _expanded = false;
  TapGestureRecognizer _tapGestureRecognizer;
  TextPainter textPainter;
  TextStyle effectiveTextStyle;
  double textScaleFactor;

  @override
  void initState() {
    super.initState();
    _expanded = widget.expanded;
    _tapGestureRecognizer = TapGestureRecognizer()..onTap = _toggleExpanded;
  }

  @override
  void dispose() {
    _tapGestureRecognizer.dispose();
    super.dispose();
  }

  _toggleExpanded() async {
    final res = await widget.beforeToggle?.call(!_expanded) ?? true;
    if (res) {
      setState(() {
        _expanded = !_expanded;
      });
    }
  }

  Size measureText(
    BuildContext context,
    double minWidth,
    double maxWidth,
    TextSpan textSpan,
    double textScaleFactor,
    TextAlign textAlign,
    TextDirection textDirection,
  ) {
    if (textPainter == null) {
      final locale = Localizations.localeOf(context);
      textPainter = TextPainter(
        text: textSpan,
        textAlign: textAlign,
        textDirection: textDirection,
        textScaleFactor: textScaleFactor,
        maxLines: widget.maxLines,
        locale: locale,
      );
    } else {
      textPainter.text = textSpan;
    }

    textPainter.layout(
      minWidth: minWidth,
      maxWidth: maxWidth,
    );
    return textPainter.size;
  }

  TextSpan buildExpandText(BuildContext context, TextStyle commonStyle) {
    final linkColor = widget.linkColor ??
        widget.linkStyle?.color ??
        Theme.of(context).primaryColor;
    final linkTextStyle = effectiveTextStyle.merge(widget.linkStyle);
    final expandTextStr =
        widget.expandText ?? BkDevopsAppi18n.of(context).$t('expandText');
    final collapseTextStr =
        widget.collapseText ?? BkDevopsAppi18n.of(context).$t('collapseText');

    final linkText = _expanded ? collapseTextStr : expandTextStr;

    return widget.customExpandWidget ??
        TextSpan(
          children: [
            if (!_expanded)
              TextSpan(
                text: '\u2026 ',
                recognizer: _tapGestureRecognizer,
                style: linkTextStyle,
              ),
            TextSpan(
              style: commonStyle,
              children: <TextSpan>[
                if (_expanded)
                  TextSpan(
                    text: ' ',
                  ),
                TextSpan(
                  text: linkText,
                  style: linkTextStyle.copyWith(
                    color: linkColor,
                  ),
                  recognizer: _tapGestureRecognizer,
                ),
              ],
            ),
          ],
        );
  }

  @override
  Widget build(BuildContext context) {
    final DefaultTextStyle defaultTextStyle = DefaultTextStyle.of(context);
    effectiveTextStyle = widget.style;
    if (widget.style == null || widget.style.inherit) {
      effectiveTextStyle = defaultTextStyle.style.merge(widget.style);
    }

    final textSpan = TextSpan(
      children: [
        TextSpan(
          text: widget.text.trim(),
          style: effectiveTextStyle,
        ),
      ],
    );

    final expandTextSpan = buildExpandText(context, effectiveTextStyle);

    Widget result = LayoutBuilder(
      builder: (BuildContext context, BoxConstraints constraints) {
        assert(constraints.hasBoundedWidth);
        final double maxWidth = constraints.maxWidth;

        final textAlign =
            widget.textAlign ?? defaultTextStyle.textAlign ?? TextAlign.start;
        final textDirection =
            widget.textDirection ?? Directionality.of(context);
        final textScaleFactor =
            widget.textScaleFactor ?? MediaQuery.textScaleFactorOf(context);

        final linkSize = widget.expandWidgetSize ??
            measureText(
              context,
              constraints.minWidth,
              maxWidth,
              expandTextSpan,
              textScaleFactor,
              textAlign,
              textDirection,
            );

        final textSize = measureText(
          context,
          constraints.minWidth,
          maxWidth,
          textSpan,
          textScaleFactor,
          textAlign,
          textDirection,
        );

        TextSpan text = textSpan;
        if (textPainter.didExceedMaxLines) {
          final position = textPainter.getPositionForOffset(Offset(
            textSize.width - linkSize.width,
            textSize.height,
          ));
          final endOffset = textPainter.getOffsetBefore(position.offset) ?? 0;

          text = TextSpan(
            style: effectiveTextStyle,
            children: [
              TextSpan(
                text: _expanded
                    ? widget.text
                    : widget.text.substring(0, max(endOffset, 0)),
              ),
              (widget.showCollapse || !_expanded)
                  ? expandTextSpan
                  : TextSpan(text: ''),
            ],
          );
        } else if (widget.customExpandWidget != null) {
          text = TextSpan(
            children: <TextSpan>[
              textSpan,
              widget.customExpandWidget,
            ],
          );
        }

        return RichText(
          text: text,
          softWrap: true,
          textDirection: textDirection,
          textAlign: textAlign,
          textScaleFactor: textScaleFactor,
          overflow: TextOverflow.clip,
        );
      },
    );

    return result;
  }
}
