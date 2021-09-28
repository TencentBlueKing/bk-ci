import 'package:bkci_app/models/ExecuteModel.dart';
import 'package:bkci_app/providers/PollProvider.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/BkTab.dart';
import 'package:bkci_app/widgets/ExecuteDetail/ArtifactoryTab.dart';
import 'package:bkci_app/widgets/ExecuteDetail/ExecuteChoreography.dart';
import 'package:bkci_app/widgets/ExecuteDetail/ExecuteLog.dart';
import 'package:bkci_app/widgets/ExecuteDetail/ExecuteSummary.dart';
import 'package:bkci_app/widgets/ExecuteDetail/MaterialLogTab.dart';
import 'package:bkci_app/widgets/ExpandAppBar.dart';
import 'package:bkci_app/widgets/Loading.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:bkci_app/utils/util.dart';

class ExecuteContent extends StatelessWidget {
  final int initialIndex;
  final String urlPrefix;

  ExecuteContent({
    this.urlPrefix,
    this.initialIndex,
  });

  List _getMenuItems(String status) {
    List items = [];
    if (['RUNNING', 'QUEUE', 'STAGE_SUCCESS'].indexOf(status) < 0) {
      items.add('runAgain');
    } else if (status != 'STAGE_SUCCESS') {
      items.add('stopRunning');
    }
    return items;
  }

  Future hanleMenuClick(item, provider) async {
    if (item == 'runAgain') {
      await ajax.post('$urlPrefix/retry');
      await provider.fetchData();
      provider.startPolling();
    } else if (item == 'stopRunning') {
      await ajax.delete(urlPrefix);
    }
    return;
  }

  Column buildContent(
    BuildContext context,
    ExecuteModel execDetail,
    PollProvider<ExecuteModel> provider,
  ) {
    final menuItems = _getMenuItems(execDetail.status);
    return Column(
      children: [
        Container(
          color: Colors.white,
          child: SafeArea(
            bottom: false,
            child: ExpandAppBar(
              title: execDetail.pipelineName ?? '',
              actions: menuItems.isNotEmpty
                  ? Container(
                      padding: EdgeInsets.all(5.px),
                      child: PopupMenuButton(
                        offset: Offset(0, -40.px),
                        padding: EdgeInsets.all(0),
                        itemBuilder: (BuildContext context) {
                          return menuItems
                              .map(
                                (e) => PopupMenuItem(
                                  value: e,
                                  child: SizedBox(
                                    width: 244.px,
                                    child: PFText(
                                      BkDevopsAppi18n.of(context).$t(e) ?? e,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: TextStyle(fontSize: 32.px),
                                    ),
                                  ),
                                ),
                              )
                              .toList();
                        },
                        onSelected: (item) {
                          BkLoading.of(context)
                              .during(hanleMenuClick(item, provider));
                        },
                        child: Icon(
                          BkIcons.more,
                          size: 44.px,
                          color: Colors.black,
                        ),
                      ),
                    )
                  : null,
            ),
          ),
        ),
        Expanded(
          child: Column(
            children: [
              ExecuteSummary(
                execDetail: execDetail,
              ),
              Expanded(
                child: BkTab(
                  initialIndex: initialIndex ?? 0,
                  tabs: [
                    BkTabItem(
                      tabLabel: 'artifactories',
                      tabView: ArtifactoryTab(
                        args: execDetail,
                      ),
                    ),
                    BkTabItem(
                      tabLabel: 'log',
                      tabView: ExecuteLog(
                        args: execDetail,
                      ),
                    ),
                    BkTabItem(
                      tabLabel: 'choreography',
                      tabView: ExecuteChoreography(
                        args: execDetail,
                      ),
                    ),
                    BkTabItem(
                      tabLabel: 'record',
                      tabView: MaterialLogTab(
                        args: execDetail,
                      ),
                    ),
                  ],
                  keepAlive: true,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final PollDataModel<ExecuteModel> provider =
        Provider.of<PollDataModel<ExecuteModel>>(context);
    final ExecuteModel execDetail = provider.value;

    print('rebuild execDetail provider: ${execDetail?.toJson()}');

    return Scaffold(
      body: execDetail == null
          ? Center(
              child: CircularProgressIndicator(
                backgroundColor: Theme.of(context).primaryColor,
              ),
            )
          : buildContent(
              context,
              execDetail,
              provider.provider,
            ),
    );
  }
}
