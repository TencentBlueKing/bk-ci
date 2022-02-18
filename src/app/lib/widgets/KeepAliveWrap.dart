import 'package:flutter/material.dart';

class KeepAliveWrap extends StatefulWidget {
  final Widget child;

  KeepAliveWrap({
    @required this.child,
  });
  @override
  _KeepAliveWrapState createState() => _KeepAliveWrapState();
}

class _KeepAliveWrapState extends State<KeepAliveWrap>
    with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;

  @override
  Widget build(BuildContext context) {
    super.build(context);
    return widget.child;
  }
}
