import 'package:bkci_app/widgets/AuthImage.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class AppIcon extends StatelessWidget {
  const AppIcon({
    Key key,
    this.borderRadius,
    this.margin,
    this.width,
    this.height,
    this.fit,
    this.showBorder = true,
    this.isAssetsImage = false,
    @required this.url,
  }) : super(key: key);

  final String url;
  final EdgeInsetsGeometry margin;
  final double width;
  final double height;
  final double borderRadius;
  final BoxFit fit;
  final bool showBorder;
  final bool isAssetsImage;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: margin,
      width: width ?? 100.px,
      height: height ?? 100.px,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(borderRadius ?? 20.px),
        border: showBorder
            ? Border.all(
                width: 1.px,
                color: Theme.of(context).dividerColor,
              )
            : null,
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(borderRadius ?? 20.px),
        child: isAssetsImage
            ? Image.asset(
                url,
                fit: fit,
              )
            : AuthImage(
                url: url,
                fit: fit,
              ),
      ),
    );
  }
}
