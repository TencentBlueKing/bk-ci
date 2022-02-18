import 'package:flutter/material.dart';

class PFText extends StatelessWidget {
  final String text;
  final TextStyle style;
  final TextAlign textAlign;
  final TextOverflow overflow;
  final int maxLines;
  final bool softWrap;

  final String fontWeight;
  PFText(
    this.text, {
    this.style,
    this.fontWeight,
    this.maxLines,
    this.overflow,
    this.textAlign,
    this.softWrap = true,
  });

  @override
  Widget build(BuildContext context) {
    final String fontFamilySuffix = fontWeight != null ? '-$fontWeight' : '';
    final TextStyle fontStyle = TextStyle(
      fontFamily: 'PingFang$fontFamilySuffix',
      color: Theme.of(context).secondaryHeaderColor,
    );
    return Text(
      text,
      maxLines: maxLines,
      overflow: overflow,
      textAlign: textAlign,
      softWrap: softWrap,
      style: fontStyle.merge(
        style,
      ),
    );
  }
}

class PFMediumText extends StatelessWidget {
  final String text;
  final TextStyle style;
  final int maxLines;
  final TextAlign textAlign;
  final TextOverflow overflow;
  final bool softWrap;

  PFMediumText(
    this.text, {
    this.style,
    this.maxLines,
    this.overflow,
    this.textAlign,
    this.softWrap = true,
  });

  @override
  Widget build(BuildContext context) {
    final TextStyle fontStyle = TextStyle(
      fontFamily: 'PingFang-medium',
      color: Theme.of(context).secondaryHeaderColor,
    );
    return Text(
      text,
      maxLines: maxLines,
      overflow: overflow,
      textAlign: textAlign,
      softWrap: softWrap,
      style: fontStyle.merge(style),
    );
  }
}

class PFBoldText extends StatelessWidget {
  final String text;
  final TextStyle style;
  final int maxLines;
  final TextAlign textAlign;
  final TextOverflow overflow;
  final bool softWrap;
  PFBoldText(
    this.text, {
    this.style,
    this.maxLines,
    this.overflow,
    this.textAlign,
    this.softWrap = true,
  });

  @override
  Widget build(BuildContext context) {
    final TextStyle fontStyle = TextStyle(
      fontFamily: 'PingFang-bold',
      color: Theme.of(context).secondaryHeaderColor,
    );
    return Text(
      text,
      maxLines: maxLines,
      overflow: overflow,
      textAlign: textAlign,
      softWrap: softWrap,
      style: fontStyle.merge(style),
    );
  }
}
