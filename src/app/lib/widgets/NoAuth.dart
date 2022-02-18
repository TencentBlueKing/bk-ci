import 'package:bkci_app/pages/App.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';

class NoAuth extends StatelessWidget {
  final String title;
  final String desc;

  NoAuth({
    this.title,
    this.desc,
  });

  PFText buildContentText(String text) {
    return PFText(
      text,
      style: TextStyle(
        fontSize: 32.px,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      padding: EdgeInsets.symmetric(
        horizontal: 32.px,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Container(
            width: 240.px,
            height: 200.px,
            margin: EdgeInsets.only(
              top: 244.px,
            ),
            padding: EdgeInsets.only(
              bottom: 26.px,
            ),
            child: Image(
              image: AssetImage('assets/images/lock.png'),
              fit: BoxFit.contain,
            ),
          ),
          buildContentText(title),
          Container(
            margin: EdgeInsets.only(bottom: 150.px),
            child: buildContentText(
              desc ?? BkDevopsAppi18n.of(context).$t('applyPermTips'),
            ),
          ),
          Padding(
            padding: EdgeInsets.symmetric(horizontal: 32.px),
            child: OutlinedButton(
              style: OutlinedButton.styleFrom(
                minimumSize: Size(
                  double.infinity,
                  88.px,
                ),
              ),
              onPressed: () {
                Navigator.of(context).pushNamedAndRemoveUntil(
                  BkDevopsApp.routePath,
                  (route) => false,
                );
              },
              child: PFText(
                BkDevopsAppi18n.of(context).$t('backHome'),
                style: TextStyle(
                  fontSize: 30.px,
                  color: Theme.of(context).primaryColor,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
