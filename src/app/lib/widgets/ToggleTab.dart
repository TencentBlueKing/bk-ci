import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class TabItem {
  final String id;
  final String label;

  TabItem({
    @required this.id,
    @required this.label,
  });
}

class ToggleTab extends StatelessWidget {
  final bool value;
  final List<TabItem> tabs;
  final Function(TabItem tab, int index) handleTap;

  ToggleTab({this.value, this.handleTap, this.tabs});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 686.px,
      height: 80.px,
      decoration: BoxDecoration(
        color: '#F0F1F5'.color,
        borderRadius: BorderRadius.circular(44.px),
      ),
      child: Stack(
        children: [
          AnimatedPositioned(
            duration: const Duration(milliseconds: 200),
            curve: Curves.bounceInOut,
            top: 8.px,
            left: value ? 8.px : 347.px,
            child: Container(
              alignment: Alignment.center,
              width: 331.px,
              height: 64.px,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(36.px),
                color: Colors.white,
              ),
            ),
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: tabs.map((tab) {
              int pos = tabs.indexOf(tab);
              return Expanded(
                flex: 1,
                child: InkWell(
                  onTap: () {
                    handleTap(tab, pos);
                  },
                  child: Container(
                    alignment: Alignment.center,
                    child: PFMediumText(
                      tab.label,
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }
}
