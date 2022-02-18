import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';

class AuthImage extends StatelessWidget {
  final String url;
  final double width;
  final double height;
  final BoxFit fit;
  final bool needAuth;

  AuthImage({
    this.url,
    this.width,
    this.height,
    this.fit = BoxFit.fill,
    this.needAuth = false,
  });

  Widget _errorBuilder(context, _, __) {
    return Image.asset('assets/images/bkdevops_logo.png');
  }

  @override
  Widget build(BuildContext context) {
    return CachedNetworkImage(
      imageUrl: url,
      height: height,
      width: width,
      fit: fit,
      httpHeaders: needAuth
          ? {
              CKEY_HEAD_FIELD: Storage.cKey,
            }
          : null,
      placeholder: (context, url) => Center(child: CircularProgressIndicator()),
      errorWidget: _errorBuilder,
    );
  }
}
