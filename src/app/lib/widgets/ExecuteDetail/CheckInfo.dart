import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/widgets/Form/FormList.dart';

class CheckInfo extends StatefulWidget {
  final String action;
  final Map payload;
  final String prefix;
  final Function submit;

  CheckInfo({
    @required this.action,
    @required this.payload,
    this.prefix,
    this.submit,
  });

  @override
  _CheckInfoState createState() => _CheckInfoState();
}

class _CheckInfoState extends State<CheckInfo> {
  bool hasInit = false;
  Map paramValue = new Map();
  List paramList = [];
  List syncItems = [];
  List checkParams = [];
  String checkDesc = '';
  String restTime = '';

  @override
  void initState() {
    super.initState();
    widget.action == '_handleAtomCheck'
        ? getAtomCheckParams()
        : getStageCheckParams();
  }

  getStageCheckParams() {
    Map resMap = Map();
    List resList = [
      {
        'label': '审核结果',
        'id': 'cancel',
        'defaultValue': 'false',
        'widgetType': 'select',
        'options': [
          {'key': 'false', 'value': '继续执行'},
          {'key': 'true', 'value': '取消执行，立即标记为Stage成功状态'},
        ]
      },
    ];

    List params = [];
    try {
      params = widget.payload['stageControlOption']['reviewParams'] ?? [];
    } catch (_) {
      print('get stage params error');
    }
    params.forEach((item) {
      Map param = {
        'label': item['key'],
        'id': item['key'],
        'defaultValue': item['value'],
        'widgetType': 'input'
      };
      resList.add(param);
      checkParams.add(item['key']);
    });

    resList.forEach((item) {
      resMap[item['id']] = item['defaultValue'];
    });

    setState(() {
      paramList = resList;
      paramValue = resMap;
      final reviewTime = getReviewTime();
      checkDesc = widget.payload['stageControlOption']['reviewDesc'] ?? '';
      restTime = '请在 $reviewTime 前完成审核操作，逾期将自动标记流水线为Stage成功状态';
      hasInit = true;
    });
  }

  String getReviewTime() {
    try {
      final stage = widget.payload;
      const hour2Ms = 60 * 60 * 1000;

      int time = stage['startEpoch'] +
          stage['stageControlOption']['timeout'] * hour2Ms;
      return time.yMdhm;
    } catch (e) {
      print(e);
      return '--';
    }
  }

  Future getAtomCheckParams() async {
    syncItems = ['status'];
    final result = await ajax
        .get('${widget.prefix}/${widget.payload["elementId"]}/toReview');

    Map resMap = Map();
    List resList = [
      {
        'label': '审核结果',
        'id': 'status',
        'defaultValue': 'PROCESS',
        'widgetType': 'select',
        'options': [
          {'key': 'PROCESS', 'value': '同意，继续执行流水线'},
          {'key': 'ABORT', 'value': '驳回，该步骤判定为失败'},
        ]
      },
      {
        'label': '审核意见',
        'id': 'suggest',
        'defaultValue': '',
        'widgetType': 'input',
        'maxLines': 3,
      },
    ];

    final params = result.data['params'];
    checkDesc = result.data['desc'];
    params.forEach((item) {
      Map param = {
        ...item,
        'label': item['key'],
        'id': item['key'],
        'defaultValue': item['value'],
        'widgetType': WIDGET_TYPE_MAP[item['valueType']]
      };
      resList.add(param);
      checkParams.add(item['key']);
    });

    resList.forEach((item) {
      resMap[item['id']] = item['defaultValue'];
    });

    setState(() {
      paramList = resList;
      paramValue = resMap;
      hasInit = true;
    });
  }

  handleSync(name, value) {
    setState(() {
      if (checkParams.length > 0) {
        paramList.forEach((item) {
          if (checkParams.indexOf(item['id']) >= 0) {
            item['hidden'] = (value == 'PROCESS' ? false : true);
          }
        });
      }
    });
  }

  Future _atomCheckSubmit(Map value) async {
    Map data = {
      'status': value['status'],
      'suggest': value['suggest'],
      'params': []
    };
    if (checkParams.length > 0) {
      checkParams.forEach((param) {
        // 接口新改动，params参数格式变化
        data['params'].add({'key': param, 'value': value[param]});
      });
    }
    await ajax.post('${widget.prefix}/${widget.payload["elementId"]}/review',
        data: data);
  }

  Future _stageCheckSubmit(Map value) async {
    Map data = {'reviewParams': []};
    if (checkParams.length > 0) {
      checkParams.forEach((param) {
        data['reviewParams'].add({'key': param, 'value': value[param]});
      });
    }
    final url =
        '${widget.prefix}/stages/${widget.payload["id"]}/manualStart?cancel=${value["cancel"]}';
    await ajax.post(url, data: data);
  }

  Future handleSubmit(Map value) async {
    try {
      if (widget.action == '_handleAtomCheck') {
        await _atomCheckSubmit(value);
      } else {
        await _stageCheckSubmit(value);
      }
      Navigator.pop(context);
      widget.submit();
    } catch (e) {
      print(e);
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
          padding: EdgeInsets.fromLTRB(
            32.px,
            25.px,
            32.px,
            20.px,
          ),
          child: !hasInit
              ? Center(
                  child: CircularProgressIndicator(
                    backgroundColor: Theme.of(context).primaryColor,
                  ),
                )
              : Column(
                  children: [
                    Center(
                      child: Container(
                        margin: EdgeInsets.symmetric(
                          vertical: 12.px,
                        ),
                        width: 72.px,
                        height: 8.px,
                        decoration: BoxDecoration(
                            color: Theme.of(context).hintColor,
                            borderRadius: BorderRadius.circular(
                              8.px,
                            )),
                      ),
                    ),
                    Expanded(
                      child: SingleChildScrollView(
                        keyboardDismissBehavior:
                            ScrollViewKeyboardDismissBehavior.onDrag,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Offstage(
                              offstage: restTime == '',
                              child: Container(
                                margin:
                                    EdgeInsets.only(top: 60.px, bottom: 0.px),
                                // height: 60.px,
                                child: PFText(
                                  '审核倒计时：$restTime',
                                  style: TextStyle(
                                    color: Colors.black,
                                    fontSize: 28.px,
                                  ),
                                ),
                              ),
                            ),
                            Offstage(
                              offstage: false,
                              child: Container(
                                margin:
                                    EdgeInsets.only(top: 60.px, bottom: 40.px),
                                child: PFText(
                                  '审核说明：$checkDesc',
                                  style: TextStyle(
                                    color: Colors.black,
                                    fontSize: 28.px,
                                  ),
                                ),
                              ),
                            ),
                            FormList(
                              paramList: paramList,
                              paramValue: paramValue,
                              buttonText: '确定',
                              submit: handleSubmit,
                              syncItems: syncItems,
                              handleSync: handleSync,
                            )
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
        ),
      ),
    );
  }
}
