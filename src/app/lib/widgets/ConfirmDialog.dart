import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class ConfirmDialog extends StatelessWidget {
  final String contentMsg;
  final String confirmLabel;
  final Color confirmLabelColor;
  final String cancelLabel;
  final Color cancelLabelColor;
  final Function confirm;
  final double height;

  ConfirmDialog({
    this.confirm,
    this.contentMsg,
    this.confirmLabel,
    this.cancelLabel,
    this.cancelLabelColor,
    this.confirmLabelColor,
    this.height,
  });

  Future _confirm(BuildContext context) async {
    await confirm();
    _dismiss(context);
  }

  _dismiss(BuildContext context) {
    Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(32.px),
      ),
      child: Container(
        height: height,
        child: Column(
          children: [
            Expanded(
              child: Container(
                padding: EdgeInsets.only(
                  top: 50.px,
                  bottom: 40.px,
                  left: 66.px,
                  right: 66.px,
                ),
                child: PFText(
                  contentMsg,
                  style: TextStyle(
                    fontSize: 32.px,
                    color: Colors.black,
                  ),
                ),
              ),
            ),
            Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.only(
                  bottomLeft: Radius.circular(32.px),
                  bottomRight: Radius.circular(32.px),
                ),
              ),
              height: 80.px,
              padding: EdgeInsets.all(18.px),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Expanded(
                    flex: 1,
                    child: GestureDetector(
                      onTap: () {
                        _dismiss(context);
                      },
                      child: Container(
                        decoration: BoxDecoration(
                          color: Colors.white,
                          border: Border(
                            right: BorderSide(
                              width: 2.px,
                              color: Theme.of(context).dividerColor,
                            ),
                          ),
                        ),
                        child: PFMediumText(
                          cancelLabel ??
                              BkDevopsAppi18n.of(context).$t('cancel'),
                          style: TextStyle(
                            fontSize: 32.px,
                            color: cancelLabelColor ??
                                Theme.of(context).primaryColor,
                          ),
                        ),
                        alignment: Alignment.center,
                      ),
                    ),
                  ),
                  Expanded(
                    flex: 1,
                    child: GestureDetector(
                      onTap: () {
                        _confirm(context);
                      },
                      child: Container(
                        color: Colors.white,
                        child: PFMediumText(
                          confirmLabel ??
                              BkDevopsAppi18n.of(context).$t('confirm'),
                          style: TextStyle(
                            fontSize: 32.px,
                            color: confirmLabelColor ??
                                Theme.of(context).primaryColor,
                          ),
                        ),
                        alignment: Alignment.center,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
