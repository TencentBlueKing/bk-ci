import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/BkTab.dart';
import 'package:bkci_app/widgets/HomeTabs/CatagoryTab.dart';
import 'package:bkci_app/widgets/HomeTabs/RecommendTab.dart';
import 'package:bkci_app/widgets/KeepAliveWrap.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/widgets/TopBar.dart';
import 'package:bkci_app/utils/util.dart';

class HomeScreen extends StatefulWidget {
  static const String routePath = '/home';
  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  static List<BkTabItem> tabs = [
    BkTabItem(
      tabLabel: 'recommendTabLabel',
      tabView: RecommendTab(),
    ),
    BkTabItem(
      tabLabel: CATEGORY_MAP[BkCategory.GAME],
      tabView: CatagoryTab(
        catagory: BkCategory.GAME,
      ),
    ),
    BkTabItem(
      tabLabel: CATEGORY_MAP[BkCategory.TOOL],
      tabView: CatagoryTab(
        catagory: BkCategory.TOOL,
      ),
    ),
    BkTabItem(
      tabLabel: CATEGORY_MAP[BkCategory.LIFE],
      tabView: CatagoryTab(
        catagory: BkCategory.LIFE,
      ),
    ),
    BkTabItem(
      tabLabel: CATEGORY_MAP[BkCategory.SOCIAL],
      tabView: CatagoryTab(
        catagory: BkCategory.SOCIAL,
      ),
    )
  ];

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Scaffold(
        body: Stack(
          children: [
            Container(
              height: 696.px,
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment(0.0, -0.4),
                  end: Alignment(0.0, 1),
                  colors: ['3023AE'.color, Colors.white],
                ),
              ),
            ),
            SafeArea(
              child: Column(
                children: [
                  Padding(
                    padding: EdgeInsets.fromLTRB(32.px, 16.px, 32.px, 16.px),
                    child: TopBar(),
                  ),
                  DefaultTabController(
                    length: tabs.length,
                    child: Expanded(
                      child: Column(
                        children: [
                          Padding(
                            padding: EdgeInsets.fromLTRB(0, 0, 0, 7.0),
                            child: TabBar(
                              indicatorSize: TabBarIndicatorSize.label,
                              indicator: UnderlineTabIndicator(
                                borderSide: BorderSide(
                                  width: 4.0.px,
                                  color: Colors.white,
                                ),
                                insets: EdgeInsets.only(
                                  left: 12.px,
                                  right: 12.px,
                                  bottom: -10.px,
                                ),
                              ),
                              indicatorColor: Colors.white,
                              labelPadding: EdgeInsets.only(
                                top: 12.0,
                              ),
                              tabs: [
                                for (final tab in tabs)
                                  PFMediumText(
                                    BkDevopsAppi18n.of(context).$t(
                                      tab.tabLabel,
                                    ),
                                    style: TextStyle(
                                      fontSize: 32.px,
                                      color: Colors.white,
                                    ),
                                  ),
                              ],
                            ),
                          ),
                          Expanded(
                            child: TabBarView(
                              physics: NeverScrollableScrollPhysics(),
                              children: [
                                for (final tab in tabs)
                                  KeepAliveWrap(
                                    child: tab.tabView,
                                  ),
                              ],
                            ),
                          ),
                        ],
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
