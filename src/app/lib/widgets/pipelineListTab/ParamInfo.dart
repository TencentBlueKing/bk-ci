import 'package:bkci_app/pages/ExecuteDetailPage.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/widgets/Form/FormList.dart';

class ParamInfo extends StatefulWidget {
  final String projectId;
  final String pipelineId;

  ParamInfo({
    @required this.projectId,
    @required this.pipelineId,
  });

  @override
  _ParamInfoState createState() => _ParamInfoState();
}

class _ParamInfoState extends State<ParamInfo> {
  bool hasInit = false;
  List paramList = [];
  Map paramValue = new Map();
  bool loading = false;

  List versionParams = [
    'BK_CI_FIX_VERSION',
    'BK_CI_MAJOR_VERSION',
    'BK_CI_MINOR_VERSION'
  ];

  @override
  void initState() {
    super.initState();
    this.getBuildParams();
  }

  Future getBuildParams() async {
    final result = await ajax.get(
      '/process/api/app/pipelineBuild/${widget.projectId}/${widget.pipelineId}/manualStartupInfo',
    );

    List resList = result.data['properties'] ?? [];
    Map resMap = {};

    resList.removeWhere((item) => (item['required'] == false));
    resList.forEach((item) {
      item['label'] = item['label'] ?? item['id'];
      item['widgetType'] = WIDGET_TYPE_MAP[item['type']];
      item['required'] = item['type'] == 'SVN_TAG' || item['type'] == 'GIT_REF';
      resMap[item['id']] = item['defaultValue'];
    });

    setState(() {
      hasInit = true;
      paramList = resList
          .where((item) => versionParams.indexOf(item['id']) == -1)
          .toList();
      paramValue = resMap;
    });
  }

  Future handleSubmit(Map data) async {
    if (loading) return;
    try {
      setState(() {
        loading = true;
      });
      final result = await ajax.post(
        '/process/api/app/pipelineBuild/${widget.projectId}/${widget.pipelineId}',
        data: data,
      );

      Navigator.pop(context);
      ExecuteDetailPageArgs args = ExecuteDetailPageArgs(
        projectId: widget.projectId,
        pipelineId: widget.pipelineId,
        buildId: result.data['id'],
        initialIndex: 2,
      );
      Navigator.of(context).pushNamed(
        ExecuteDetailPage.routePath,
        arguments: args,
      );
    } catch (e) {
      print(e);
      toast(BkDevopsAppi18n.of(context).$t('triggerPipelineErrorTips'));
    } finally {
      if (mounted) {
        setState(() {
          loading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: GestureDetector(
        behavior: HitTestBehavior.translucent,
        onTap: () {
          FocusScope.of(context).requestFocus(FocusNode());
        },
        child: Container(
          height: SizeFit.deviceHeight * 0.85,
          padding: EdgeInsets.fromLTRB(32.px, 25.px, 32.px, 20.px),
          child: Container(
            child: !hasInit
                ? Center(
                    child: CircularProgressIndicator(
                      backgroundColor: Theme.of(context).primaryColor,
                    ),
                  )
                : Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Center(
                        child: Container(
                          width: 72.px,
                          height: 8.px,
                          color: Theme.of(context).hintColor,
                        ),
                      ),
                      Container(
                        padding: EdgeInsets.only(top: 40.px),
                        child: PFMediumText(
                          BkDevopsAppi18n.of(context).$t('parameters'),
                          style: TextStyle(
                            color: Colors.black,
                            fontSize: 28.px,
                          ),
                        ),
                      ),
                      Expanded(
                        child: SingleChildScrollView(
                          child: FormList(
                            paramList: paramList,
                            paramValue: paramValue,
                            buttonText: BkDevopsAppi18n.of(context)
                                .$t('executePipeline'),
                            submiting: loading,
                            submit: handleSubmit,
                          ),
                        ),
                      ),
                    ],
                  ),
          ),
        ),
      ),
    );
  }
}
