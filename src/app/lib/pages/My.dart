import 'dart:io';

import 'package:bkci_app/pages/AppChangelog.dart';
import 'package:bkci_app/pages/Feedback.dart';
import 'package:bkci_app/pages/InstallationManagement.dart';
import 'package:bkci_app/pages/SettingPage.dart';
import 'package:bkci_app/pages/ShareQRCode.dart';
import 'package:bkci_app/providers/CheckUpdateProvider.dart';
import 'package:bkci_app/providers/UserProvider.dart';
import 'package:bkci_app/utils/LoginUtil.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/AuthImage.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/pages/DownloadRecords.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

import 'package:provider/provider.dart';

class MyScreen extends StatelessWidget {
  static const routePath = '/my';

  final double itemheight = 90.px;
  final List<List<Map<String, dynamic>>> entrys = [
    [
      // {
      //   'label': 'ApprovalCenter',
      //   'pageRoute': '',
      // },
      Platform.isIOS
          ? {
              'label': 'downloadRecords',
              'pageRoute': DownloadRecords.routePath,
              'childBuilder': _childBuilder,
            }
          : {
              'label': 'InstallManage',
              'pageRoute': InstallationManagement.routePath,
              'childBuilder': _childBuilder,
              // 'indicator': Selector<PkgProvider, int>(
              //   selector: (BuildContext context, provider) =>
              //       provider.pendingUpgradePkgs != null
              //           ? provider.pendingUpgradePkgs.length
              //           : 0,
              //   builder: (BuildContext context, value, _) => value > 0
              //       ? _badgeBuilder('$value', 36.px, 36.px)
              //       : SizedBox(),
              // ),
            },
    ],
    [
      ...Storage.loginType == LOGIN_TYPE.INTERNAL
          ? [
              {
                'label': 'feedback',
                'pageRoute': FeedbackScreen.routePath,
                'childBuilder': _childBuilder,
              }
            ]
          : [],
      {
        'label': 'shareApp',
        'pageRoute': ShareQRCode.routePath,
        'childBuilder': _childBuilder,
      }
    ],
    [
      {
        'label': 'checkUpdate',
        'subLabel': PFText(
          '（${Storage.appVersion}）',
          style: TextStyle(
            color: '#979BA5'.color,
            fontSize: 24.px,
          ),
        ),
        'action': _checkUpdate,
        'childBuilder': _childBuilder,
        'indicator': Selector<CheckUpdateProvider, String>(
          selector: (BuildContext context, provider) =>
              provider.needUpgrade ? 'New' : null,
          builder: (BuildContext context, value, _) =>
              _badgeBuilder(value, 68.px, 36.px),
        ),
      },
      ...Storage.loginType == LOGIN_TYPE.INTERNAL
          ? [
              {
                'label': 'about',
                'pageRoute': AppChangelog.routePath,
                'childBuilder': _childBuilder,
              }
            ]
          : [],
    ],
    [
      {
        'label': 'setting',
        'pageRoute': SettingPage.routePath,
        'childBuilder': _childBuilder,
      },
    ],
  ];

  static _checkUpdate(BuildContext context) {
    Provider.of<CheckUpdateProvider>(context, listen: false).checkUpdate();
  }

  Future<void> _logout() async {
    LoginUtil.logout();
  }

  _handleItemTap(BuildContext context, item) {
    if (item['pageRoute'] != null && item['pageRoute'] != '') {
      Navigator.of(context).pushNamed(item['pageRoute']);
    } else if (item['action'] != null && (item['action'] is Function)) {
      item['action'](context);
    }
  }

  Widget _itemBuilder(BuildContext context, item) {
    return GestureDetector(
      onTap: () {
        _handleItemTap(context, item);
      },
      child: Container(
        color: Colors.white,
        height: itemheight,
        padding: EdgeInsets.only(left: 32.px, right: 32.px),
        child: item['childBuilder'](context, item),
      ),
    );
  }

  Widget _groupBuilder(BuildContext context, group) {
    return Container(
      margin: EdgeInsets.only(
        bottom: 16.px,
      ),
      height: group.length * itemheight,
      color: Colors.white,
      child: ListView.separated(
        physics: const NeverScrollableScrollPhysics(),
        padding: EdgeInsets.all(0),
        itemBuilder: (BuildContext context, int index) => _itemBuilder(
          context,
          group[index],
        ),
        cacheExtent: itemheight,
        separatorBuilder: (BuildContext context, int index) => Divider(
          height: .5,
          indent: 32.px,
        ),
        itemCount: group.length,
      ),
    );
  }

  static Widget _childBuilder(BuildContext context, item) {
    final labelChildren = <Widget>[
      Text(
        BkDevopsAppi18n.of(context).$t(item['label']),
        style: TextStyle(
          fontSize: 28.px,
          color: Colors.black,
        ),
      ),
    ];
    if (item['subLabel'] is Widget) {
      labelChildren.add(item['subLabel']);
    }
    final List<Widget> children = [
      Expanded(
        child: Row(
          children: labelChildren,
        ),
      ),
      Icon(
        BkIcons.right,
      ),
    ];

    if (item['indicator'] != null) {
      children.insert(1, item['indicator']);
    }
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: children,
    );
  }

  static Widget _badgeBuilder(String label, double width, double height) {
    if (label == null) {
      return SizedBox();
    }
    return Container(
      width: width,
      height: height,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: '#EA3636'.color,
        borderRadius: BorderRadius.circular(32.px),
      ),
      child: PFMediumText(
        label,
        style: TextStyle(
          color: Colors.white,
          fontSize: 22.px,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            height: 640.px,
            width: SizeFit.deviceWidth,
            decoration: BoxDecoration(
              color: Colors.white,
              image: DecorationImage(
                fit: BoxFit.cover,
                image: AssetImage('assets/images/my_bg.png'),
              ),
            ),
            child: Consumer<User>(
              builder: (BuildContext context, User user, child) {
                if (user.user == null) {
                  return Center(
                    child: CircularProgressIndicator(
                      backgroundColor: Theme.of(context).primaryColor,
                    ),
                  );
                }
                return Column(
                  children: [
                    Container(
                      margin: EdgeInsets.only(top: 268.px, bottom: 22.px),
                      width: 200.px,
                      height: 200.px,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(100.px),
                        border: Border.all(
                          color: Colors.white,
                          width: 8.px,
                        ),
                      ),
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(100.px),
                        child: AuthImage(
                          url: user.user.avatars,
                          fit: BoxFit.cover,
                          needAuth: true,
                        ),
                      ),
                    ),
                    PFMediumText(
                      user.user.englishName ?? '--',
                      style: TextStyle(
                        fontSize: 36.px,
                        height: 54.px / 36.px,
                      ),
                    ),
                    PFText(
                      user.user.email ?? '--',
                      style: TextStyle(
                        fontSize: 28.px,
                        height: 40.px / 28.px,
                        color: '#979BA5'.color,
                      ),
                    ),
                  ],
                );
              },
            ),
          ),
          Container(
            margin: EdgeInsets.only(top: 16.px),
            child: Column(
              children: [
                for (final group in entrys)
                  _groupBuilder(
                    context,
                    group,
                  )
              ],
            ),
          ),
          GestureDetector(
            onTap: _logout,
            child: Container(
              color: Colors.white,
              margin: EdgeInsets.only(top: 16.px),
              height: 88.px,
              alignment: Alignment.center,
              child: Text(
                BkDevopsAppi18n.of(context).$t('logout'),
                style: TextStyle(
                  color: Colors.black,
                  fontSize: 28.px,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
