import 'package:flutter/material.dart';

class ExpandableText extends StatefulWidget {
  final String text;
  final int maxLines;
  final TextStyle style;
  final bool expand;

  ExpandableText(
      {Key key, this.text, this.maxLines, this.style, this.expand = false})
      : super(key: key);

  @override
  _ExpandableTextState createState() => _ExpandableTextState();
}

class _ExpandableTextState extends State<ExpandableText> {
  bool expand;

  @override
  void initState() {
    super.initState();
    expand = widget.expand || false;
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (context, size) {
      final span = TextSpan(text: widget.text ?? '', style: widget.style);
      final tp = TextPainter(
          text: span,
          maxLines: widget.maxLines,
          textDirection: TextDirection.ltr);
      tp.layout(maxWidth: size.maxWidth);

      if (tp.didExceedMaxLines) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            GestureDetector(
              onTap: () {
                setState(() {
                  expand = !expand;
                });
              },
              child: expand
                  ? Text(
                      widget.text ?? '',
                      style: widget.style,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    )
                  : Text(
                      widget.text ?? '',
                      maxLines: widget.maxLines,
                      overflow: TextOverflow.ellipsis,
                      style: widget.style,
                    ),
            ),
          ],
        );
      } else {
        return Text(widget.text ?? '', style: widget.style);
      }
    });
  }
}
