import 'package:bkci_app/widgets/AppIcon.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class ProjectLogo extends StatelessWidget {
  ProjectLogo({
    Key key,
    this.logoUrl,
    this.borderRadius,
    this.margin,
    this.showBorder = false,
    this.isStreamProject = false,
  }) : super(key: key);

  final String logoUrl;
  final bool isStreamProject;
  final double borderRadius;
  final bool showBorder;
  final EdgeInsetsGeometry margin;

  @override
  Widget build(BuildContext context) {
    return AppIcon(
      width: 56.px,
      height: 56.px,
      margin: margin,
      showBorder: showBorder,
      borderRadius: borderRadius,
      isAssetsImage: isStreamProject,
      url: isStreamProject ? 'assets/images/stream.png' : logoUrl,
      fit: BoxFit.contain,
    );
  }
}
