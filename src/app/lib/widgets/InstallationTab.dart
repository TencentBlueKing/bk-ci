import 'package:bkci_app/widgets/AppListItem.dart';
import 'package:bkci_app/widgets/DownloadPkgItem.dart';
import 'package:bkci_app/widgets/Empty.dart';
import 'package:bkci_app/widgets/SectionList.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class PkgItem {
  final String bundleIdentifier;
  final String id;
  final String taskId;
  final String leadingUrl;
  final String title;
  final String subTitle;
  final int createTime;
  final int size;
  final bool isInDownloadRecordsTab;
  final bool isInNeedUpgradeTab;
  final Function setSelectable;
  final Function handleItemSelected;
  final String appScheme;
  final String lastDownloadHashId;

  final bool expired;
  final bool selectable;
  final bool selected;

  PkgItem({
    this.bundleIdentifier,
    this.id,
    this.taskId,
    this.leadingUrl,
    this.title,
    this.subTitle,
    this.createTime,
    this.size,
    this.isInNeedUpgradeTab,
    this.isInDownloadRecordsTab,
    this.setSelectable,
    this.handleItemSelected,
    this.selectable = false,
    this.selected = false,
    this.lastDownloadHashId,
    this.appScheme,
    this.expired,
  });
}

class InstallationTab extends StatefulWidget {
  final List sections;
  final bool isSectionList;

  InstallationTab({
    this.sections,
    this.isSectionList,
  });

  @override
  _InstallationTabState createState() => _InstallationTabState();
}

class _InstallationTabState extends State<InstallationTab> {
  Widget _sectionBuilder(BuildContext context, sections) {
    return SectionList(
      sections: sections,
      itemBuilder: _itemBuilder,
    );
  }

  ListView _listviewBuilder(list) {
    return ListView.separated(
      itemBuilder: (context, index) => _itemBuilder(context, list[index]),
      separatorBuilder: (context, index) {
        return Divider(
          indent: 160.px,
        );
      },
      itemCount: list.length,
    );
  }

  Widget _itemBuilder(BuildContext context, item) {
    return GestureDetector(
      onLongPress: () {
        item.setSelectable(true);
      },
      child: item.selectable
          ? DownloadPkgItem(
              id: item.taskId,
              selected: item.selected,
              onChange: item.handleItemSelected,
              leadingUrl: item.leadingUrl,
              title: item.title,
              subTitle: item.subTitle,
              createTime: item.createTime,
              size: item.size,
            )
          : AppListItem(
              bundleIdentifier: item.bundleIdentifier,
              id: item.id,
              leadingUrl: item.leadingUrl,
              title: item.title,
              subTitle: item.subTitle,
              createTime: item.createTime,
              size: item.size,
              isInDownloadRecordsTab: item.isInDownloadRecordsTab ?? false,
              isInNeedUpgradeTab: item.isInNeedUpgradeTab ?? false,
              expired: item.expired,
              appScheme: item.appScheme,
              lastDownloadHashId: item.lastDownloadHashId,
            ),
    );
  }

  Widget _listBuilder(list) {
    return Container(
      padding: EdgeInsets.only(top: 24.px),
      child: _listviewBuilder(list),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (widget.sections.length == 0) {
      return Empty();
    }
    return widget.isSectionList
        ? _sectionBuilder(context, widget.sections)
        : _listBuilder(widget.sections);
  }
}
