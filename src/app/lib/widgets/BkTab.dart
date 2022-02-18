import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/KeepAliveWrap.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class BkTabItem {
  final String tabLabel;
  final Widget tabView;

  BkTabItem({
    @required this.tabLabel,
    @required this.tabView,
  });
}

class BkTab extends StatelessWidget {
  final List<BkTabItem> tabs;
  final int initialIndex;
  final bool keepAlive;
  final TabBarIndicatorSize indicatorSize;
  final UnderlineTabIndicator indicator;
  final Color indicatorColor;
  final Color labelColor;
  final Color unselectedLabelColor;
  final TextStyle labelStyle;

  BkTab({
    this.tabs,
    this.indicatorSize = TabBarIndicatorSize.label,
    this.indicator,
    this.indicatorColor,
    this.labelStyle,
    this.labelColor,
    this.unselectedLabelColor,
    this.initialIndex = 0,
    this.keepAlive = false,
  });

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      initialIndex: initialIndex,
      length: tabs.length,
      child: Column(
        children: [
          Container(
            height: 88.px,
            color: Colors.white,
            child: TabBar(
              labelPadding: EdgeInsets.zero,
              indicatorSize: indicatorSize,
              labelColor: labelColor ?? Theme.of(context).primaryColor,
              unselectedLabelColor: unselectedLabelColor ??
                  Theme.of(context).secondaryHeaderColor,
              indicator: indicator ??
                  UnderlineTabIndicator(
                    borderSide: BorderSide(
                      width: 4.0.px,
                      color: Theme.of(context).primaryColor,
                    ),
                    insets: EdgeInsets.only(
                      left: 40.px,
                      right: 40.px,
                    ),
                  ),
              labelStyle: labelStyle ??
                  TextStyle(
                    fontSize: 30.px,
                    height: 44.px / 30.px,
                    fontFamily: 'PingFang-medium',
                  ),
              tabs: [
                for (final tab in tabs)
                  PFText(
                    BkDevopsAppi18n.of(context).$t(
                      tab.tabLabel,
                    ),
                  ),
              ],
            ),
          ),
          Divider(
            height: 0.5,
          ),
          Expanded(
            child: TabBarView(
              physics: NeverScrollableScrollPhysics(),
              children: [
                for (final tab in tabs)
                  Container(
                    color: Colors.white,
                    child: keepAlive
                        ? KeepAliveWrap(
                            child: tab.tabView,
                          )
                        : tab.tabView,
                  )
              ],
            ),
          ),
        ],
      ),
    );
  }
}
