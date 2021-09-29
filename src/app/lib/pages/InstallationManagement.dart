import 'package:bkci_app/widgets/ConfirmDialog.dart';
import 'package:bkci_app/providers/DownloadProvider.dart';
import 'package:bkci_app/providers/PkgProvider.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/BkAppBar.dart';
// import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/DownloadRecordsTab.dart';
import 'package:bkci_app/widgets/InstallationTab.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/SectionList.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:flutter_downloader/flutter_downloader.dart';
import 'package:provider/provider.dart';

class InstallationManagement extends StatefulWidget {
  static const String routePath = '/installManage';
  final bool hasPendingUpgradePkg;

  InstallationManagement({this.hasPendingUpgradePkg = false});

  @override
  _InstallationManagementState createState() => _InstallationManagementState();
}

class _InstallationManagementState extends State<InstallationManagement>
    with WidgetsBindingObserver {
  List tabs = [
    // 'pendingUpgrade',
    'downloadRecords',
    'downloadManagement',
  ];
  bool _selectable = false;
  List<String> selectItems = [];

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);

    updateProvider();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    super.didChangeAppLifecycleState(state);

    switch (state) {
      case AppLifecycleState.resumed:
        updateProvider();
        break;
      case AppLifecycleState.inactive:
      case AppLifecycleState.paused:
      case AppLifecycleState.detached:
        break;
    }
  }

  updateProvider() {
    // final pkgProvider = Provider.of<PkgProvider>(context, listen: false);
    final downloadProvider =
        Provider.of<DownloadProvider>(context, listen: false);
    downloadProvider.getAllDownloadTasks();
    // pkgProvider.checkInstallPkgUpdate();
  }

  void _toggleSelectable([selectable = false]) {
    setState(() {
      _selectable = selectable;
      if (!selectable) {
        selectItems = [];
      }
    });
  }

  void _handleItemSelected(String id) {
    setState(() {
      if (selectItems.contains(id)) {
        selectItems.removeWhere((element) => element == id);
      } else {
        selectItems = [
          id,
          ...selectItems,
        ];
      }
    });
  }

  void clearCompleteList(DownloadProvider provider, List completeList) {
    showDialog(
      context: context,
      builder: (context) => ConfirmDialog(
        height: 226.px,
        contentMsg: BkDevopsAppi18n.of(context).$t('patchDeleteTips'),
        confirmLabel: BkDevopsAppi18n.of(context).$t('delete'),
        confirmLabelColor: '#EA3636'.color,
        confirm: () {
          provider.deleteAll(completeList, true);
        },
      ),
    );
  }

  String _getSubtitle(e) {
    final int time = e.createTime;
    final int size = e.size;
    return '${time.yyMMdd}    ${size.mb}';
  }

  static Widget _actionBuilder(
    BuildContext context,
    String i18nLabel,
    Function onTap,
  ) {
    return GestureDetector(
      onTap: onTap,
      child: Text(
        BkDevopsAppi18n.of(context).$t(i18nLabel),
        style: TextStyle(
          fontSize: 26.px,
          color: Theme.of(context).primaryColor,
        ),
      ),
    );
  }

  static Widget _headerBuilder(
      BuildContext context, String i18nLabel, int length,
      [Widget action]) {
    return Container(
      alignment: Alignment.centerLeft,
      margin: EdgeInsets.fromLTRB(32.px, 38.px, 32.px, 15.px),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          PFMediumText(
            '${BkDevopsAppi18n.of(context).$t(i18nLabel)}（$length）',
            style: TextStyle(
              fontSize: 32.px,
              height: 48.px / 32.px,
              color: Colors.black,
            ),
          ),
          action ??
              SizedBox(
                height: 0,
              ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final i18n = BkDevopsAppi18n.of(context).$t;
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: BkAppBar(
        title: i18n('installationManagement'),
        // actions: [Icon(BkIcons.more)],
        shadowColor: Colors.transparent,
      ),
      body: DefaultTabController(
        // initialIndex: widget.hasPendingUpgradePkg ? 0 : 2,
        initialIndex: 1,
        length: tabs.length,
        child: Column(
          children: [
            Container(
              height: 88.px,
              alignment: Alignment.centerLeft,
              padding: EdgeInsets.only(left: 16.px),
              color: Colors.white,
              child: TabBar(
                isScrollable: true,
                indicatorSize: TabBarIndicatorSize.label,
                labelPadding: EdgeInsets.symmetric(horizontal: 16.px),
                labelColor: Theme.of(context).primaryColor,
                unselectedLabelColor: Theme.of(context).secondaryHeaderColor,
                indicator: UnderlineTabIndicator(
                  borderSide: BorderSide(
                    width: 4.0.px,
                    color: Theme.of(context).primaryColor,
                  ),
                  insets: EdgeInsets.only(
                    left: 40.px,
                    right: 40.px,
                    bottom: -22.px,
                  ),
                ),
                labelStyle: TextStyle(
                  fontSize: 30.px,
                  fontFamily: 'PingFang-medium',
                ),
                tabs: [
                  for (final tab in tabs) Text(i18n(tab)),
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
                  // buildPendingUpgradeTab(),
                  DownloadRecordsTab(),
                  buildDownloadTaskTab()
                ],
              ),
            ),
            if (_selectable)
              Container(
                height: 106.px,
                decoration: BoxDecoration(
                  border: Border(
                    top: BorderSide(
                      color: '#DCDEE5'.color,
                    ),
                  ),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Expanded(
                      child: InkWell(
                        onTap: _toggleSelectable,
                        child: Container(
                          color: Colors.white,
                          child: Center(
                            child: PFText(
                              BkDevopsAppi18n.of(context).$t('cancel'),
                            ),
                          ),
                        ),
                      ),
                    ),
                    Container(
                      height: 64.px,
                      width: 1.px,
                      decoration: BoxDecoration(
                        color: '#DCDEE5'.color,
                      ),
                    ),
                    Expanded(
                      child: InkWell(
                        onTap: () {
                          showDialog(
                            context: context,
                            builder: (context) => ConfirmDialog(
                              height: 226.px,
                              contentMsg: BkDevopsAppi18n.of(context)
                                  .$t('deleteSelectedJobTips'),
                              confirmLabel:
                                  BkDevopsAppi18n.of(context).$t('delete'),
                              confirmLabelColor: '#EA3636'.color,
                              confirm: () async {
                                await Future.wait(selectItems.map(
                                  (e) => Provider.of<DownloadProvider>(context,
                                          listen: false)
                                      .removeDownloadByTaskId(e, true),
                                ));
                                _toggleSelectable();
                              },
                            ),
                          );
                        },
                        child: Container(
                          color: Colors.white,
                          child: Center(
                            child: PFText(
                              '${BkDevopsAppi18n.of(context).$t('delete')}（${selectItems.length}）',
                              style: TextStyle(color: '#EA3636'.color),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              )
          ],
        ),
      ),
    );
  }

  Selector<PkgProvider, List<SectionData>> buildPendingUpgradeTab() {
    return Selector<PkgProvider, List<SectionData>>(
      selector: (context, provider) {
        final pendingUpgradePkgs = provider.pendingUpgradePkgs ?? [];
        if (pendingUpgradePkgs.length == 0) return [];
        return [
          SectionData(
            header: _headerBuilder(
              context,
              'pendingUpgrade',
              pendingUpgradePkgs.length,
              _actionBuilder(context, 'upgradeAll', () {
                Provider.of<DownloadProvider>(context, listen: false)
                    .upgradeAll(provider.pendingUpgradePkgs);
              }),
            ),
            list: pendingUpgradePkgs
                .map((e) => PkgItem(
                      bundleIdentifier: e.bundleIdentifier,
                      id: e.experienceHashId,
                      leadingUrl: e.logoUrl,
                      title: e.experienceName,
                      subTitle: _getSubtitle(e),
                      createTime: e.createTime,
                      expired: false,
                      size: e.size,
                      isInNeedUpgradeTab: true,
                    ))
                .toList(),
          )
        ];
      },
      builder: (context, value, child) {
        return InstallationTab(
          sections: value ?? [],
          isSectionList: true,
        );
      },
    );
  }

  Selector<DownloadProvider, List<SectionData>> buildDownloadTaskTab() {
    return Selector<DownloadProvider, List<SectionData>>(
      selector: (context, provider) {
        final downloadingList = provider.getDownloadingTasks();
        final completeList =
            provider.getTypedTasks(DownloadTaskStatus.complete);

        if (downloadingList.length == 0 && completeList.length == 0) return [];
        return [
          SectionData(
            header: _headerBuilder(
              context,
              'downloadManagement',
              downloadingList.length,
              downloadingList.length > 0 && _selectable
                  ? _actionBuilder(
                      context,
                      'selectAll',
                      () {
                        setState(() {
                          selectItems = [
                            ...{
                              ...downloadingList.map((e) => e.id),
                              ...selectItems
                            }
                          ];
                        });
                      },
                    )
                  : null,
            ),
            list: downloadingList
                .map((e) => PkgItem(
                      bundleIdentifier: e.bundleIdentifier,
                      id: e.expId,
                      taskId: e.id,
                      leadingUrl: e.logoUrl,
                      title: e.name,
                      subTitle: _getSubtitle(e),
                      createTime: e.createTime,
                      size: e.size,
                      lastDownloadHashId: '',
                      expired: false,
                      setSelectable: _toggleSelectable,
                      selectable: _selectable,
                      selected: selectItems.contains(e.id),
                      handleItemSelected: _handleItemSelected,
                    ))
                .toList(),
          ),
          SectionData(
            header: _headerBuilder(
              context,
              'pkgManagement',
              completeList.length,
              completeList.length > 0
                  ? _actionBuilder(
                      context,
                      _selectable ? 'selectAll' : 'deleteAll',
                      () {
                        if (_selectable) {
                          setState(() {
                            selectItems = [
                              ...{
                                ...completeList.map((e) => e.id),
                                ...selectItems
                              }
                            ];
                          });
                        } else {
                          clearCompleteList(provider, completeList);
                        }
                      },
                    )
                  : null,
            ),
            list: completeList.map((e) {
              return PkgItem(
                bundleIdentifier: e.bundleIdentifier,
                id: e.expId,
                taskId: e.id,
                leadingUrl: e.logoUrl,
                title: e.name,
                subTitle: '${e.createTime.yMdhms}    ${e.size.mb}',
                createTime: e.createTime,
                size: e.size,
                setSelectable: _toggleSelectable,
                selectable: _selectable,
                lastDownloadHashId: '',
                expired: false,
                selected: selectItems.contains(e.id),
                handleItemSelected: _handleItemSelected,
              );
            }).toList(),
          ),
        ];
      },
      builder: (context, value, child) {
        return InstallationTab(
          sections: value,
          isSectionList: true,
        );
      },
    );
  }
}
