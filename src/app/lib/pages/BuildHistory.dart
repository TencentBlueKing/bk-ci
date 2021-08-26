import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/ExpandAppBar.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/pipelineListTab/BuildList.dart';
import 'package:bkci_app/widgets/pipelineListTab/FilterBuildDrawer.dart';
import 'package:bkci_app/widgets/pipelineListTab/ParamInfo.dart';

class BuildHistoryArgument {
  final String projectId;
  final String pipelineId;
  final String pipelineName;
  final bool canTrigger;

  BuildHistoryArgument({
    this.projectId,
    this.pipelineId,
    this.pipelineName,
    this.canTrigger,
  });
}

class BuildHistory extends StatefulWidget {
  static const routePath = '/buildHistory';
  final String projectId;
  final String pipelineId;
  final String pipelineName;
  final bool canTrigger;

  BuildHistory({
    this.projectId,
    this.pipelineId,
    this.pipelineName,
    this.canTrigger,
  });

  @override
  _BuildHistoryState createState() => _BuildHistoryState();
}

class _BuildHistoryState extends State<BuildHistory> {
  final GlobalKey<ScaffoldState> _buildScaffoldKey =
      new GlobalKey<ScaffoldState>();

  String queryStr = '';
  Map<String, dynamic> queryConditon = {
    'buildMsg': '',
    'status': [],
    'materialUrl': [],
    'materialBranch': []
  };

  _handleQueryStr() {
    String newQueryStr = '';
    this.queryConditon.forEach((key, value) => {
          if (key == 'buildMsg')
            {if (value != '') newQueryStr += '&$key=$value'}
          else
            {
              if (value.length > 0)
                {
                  value.forEach((item) {
                    newQueryStr += '&$key=$item';
                  })
                }
            }
        });

    this.setState(() {
      this.queryStr = newQueryStr;
    });
  }

  filterChange(result) {
    Navigator.pop(context);
    this.queryConditon = result;
    this._handleQueryStr();
  }

  handleTrigger() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      enableDrag: true,
      backgroundColor: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(32.px),
          topRight: Radius.circular(32.px),
        ),
      ),
      builder: (BuildContext context) {
        return ParamInfo(
          projectId: widget.projectId,
          pipelineId: widget.pipelineId,
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: _buildScaffoldKey,
      endDrawer: Container(
        width: 648.px,
        child: Drawer(
          child: SafeArea(
            bottom: false,
            child: FilterBuild(
              projectId: widget.projectId,
              pipelineId: widget.pipelineId,
              queryCondition: queryConditon,
              handleFilter: filterChange,
            ),
          ),
        ),
      ),
      body: Column(
        children: [
          Container(
            color: Colors.white,
            child: SafeArea(
              bottom: false,
              child: ExpandAppBar(
                title: widget.pipelineName ?? '',
                actions: GestureDetector(
                  onTap: () {
                    _buildScaffoldKey.currentState.openEndDrawer();
                  },
                  child: Container(
                    padding: EdgeInsets.all(5.px),
                    child: Icon(
                      BkIcons.filter,
                      size: 44.px,
                      color: Colors.black,
                    ),
                  ),
                ),
              ),
            ),
          ),
          Expanded(
            child: Stack(
              children: [
                Container(
                  child: Column(
                    children: [
                      Expanded(
                        child: Container(
                          margin:
                              EdgeInsets.fromLTRB(32.px, 20.px, 32.px, 120.px),
                          child: BuildList(
                            key: ValueKey(queryStr),
                            projectId: widget.projectId,
                            pipelineId: widget.pipelineId,
                            pipelineName: widget.pipelineName,
                            url:
                                '/process/api/app/pipelineBuild/${widget.projectId}/${widget.pipelineId}/history/new',
                            queryStr: queryStr,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                Positioned(
                  bottom: 32.px,
                  left: 32.px,
                  child: Container(
                    height: 88.px,
                    width: 686.px,
                    child: ElevatedButton(
                      onPressed: widget.canTrigger ? handleTrigger : null,
                      child: PFText(
                        BkDevopsAppi18n.of(context).$t(
                            widget.canTrigger ? 'triggerPipeline' : 'noManual'),
                        style: TextStyle(fontSize: 30.px, color: Colors.white),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
