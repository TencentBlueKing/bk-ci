import 'package:flutter/material.dart';

class CircleIconButton extends StatelessWidget {
  final double width;
  final double height;
  final double iconSize;
  final IconData icon;
  final Function onPressed;
  final String toolTips;

  CircleIconButton(
      {Key key,
      this.width,
      this.height,
      this.iconSize,
      this.icon,
      this.onPressed,
      this.toolTips})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: width,
      height: height ?? width,
      decoration: const ShapeDecoration(
        color: Colors.white10,
        shape: CircleBorder(),
      ),
      child: IconButton(
          padding: EdgeInsets.all(0),
          iconSize: iconSize,
          color: Colors.white,
          icon: Icon(icon),
          tooltip: toolTips ?? null,
          onPressed: this.onPressed),
    );
  }
}
