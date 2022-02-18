import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class CupertinoBottomPopup extends StatefulWidget {
  final Widget child;
  final Widget footWidget;
  final double height;
  final double width;
  final bool show;
  final bool quickClose;
  final Function onDismiss;

  CupertinoBottomPopup({
    Key key,
    this.show,
    this.child,
    this.footWidget,
    this.width,
    this.height,
    this.quickClose,
    this.onDismiss,
  }) : super(key: key);

  @override
  _CupertinoBottomPopupState createState() => _CupertinoBottomPopupState();
}

class _CupertinoBottomPopupState extends State<CupertinoBottomPopup>
    with TickerProviderStateMixin {
  AnimationController controller;
  Tween<Offset> tween;

  @override
  void initState() {
    super.initState();
    controller = new AnimationController(
        duration: Duration(milliseconds: 200), vsync: this);
    tween = Tween<Offset>(
      begin: Offset(0.0, 1.0),
      end: Offset(0.0, 0.0),
    );
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (widget.show) {
      controller.forward();
    } else {
      controller.reverse();
    }
  }

  Future _onDismiss() async {
    await controller.reverse();
    widget.onDismiss();
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  Widget bgBuilder() {
    return Opacity(
      opacity: 0.5,
      child: Container(
        width: SizeFit.deviceWidth,
        height: SizeFit.deviceHeight,
        color: Colors.black,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: () async {
        await _onDismiss();

        return true;
      },
      child: Material(
        color: Colors.transparent,
        child: Stack(
          children: [
            widget.quickClose
                ? GestureDetector(
                    onTap: _onDismiss,
                    child: bgBuilder(),
                  )
                : bgBuilder(),
            Positioned(
              bottom: 0.px,
              left: 0.px,
              child: SafeArea(
                child: SlideTransition(
                  position: tween.animate(controller),
                  child: Container(
                    margin: EdgeInsets.all(32.px),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(32.px),
                    ),
                    width: widget.width,
                    height: widget.height,
                    child: Column(
                      children: [
                        Expanded(
                          child: widget.child,
                        ),
                        widget.footWidget ??
                            GestureDetector(
                              onTap: _onDismiss,
                              child: Container(
                                decoration: BoxDecoration(
                                  color: Colors.white,
                                  borderRadius: BorderRadius.only(
                                    bottomLeft: Radius.circular(32.px),
                                    bottomRight: Radius.circular(32.px),
                                  ),
                                ),
                                padding: EdgeInsets.only(bottom: 40.px),
                                alignment: Alignment.center,
                                child: PFMediumText(
                                  BkDevopsAppi18n.of(context).$t('cancel'),
                                  style: TextStyle(
                                    fontSize: 32.px,
                                    color: '#3A84FF'.color,
                                    height: 48.px / 32.px,
                                  ),
                                ),
                              ),
                            ),
                      ],
                    ),
                  ),
                ),
              ),
            )
          ],
        ),
      ),
    );
  }
}
