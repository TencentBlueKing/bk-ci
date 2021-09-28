import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/Loading.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/widgets/BkAppbar.dart';
import 'package:bkci_app/widgets/Form/FormList.dart';
import 'package:bkci_app/utils/request.dart';
import 'dart:convert';

class CreateExpArgument {
  final String projectId;
  final Map artifact;

  CreateExpArgument({
    this.projectId,
    this.artifact,
  });
}

class CreateExp extends StatefulWidget {
  static const routePath = '/createExp';
  final String projectId;
  final Map artifact;

  CreateExp({this.projectId, this.artifact});

  @override
  _CreateExpState createState() => _CreateExpState();
}

class _CreateExpState extends State<CreateExp> {
  List paramList = [
    {
      'label': BkDevopsAppi18n.translate('appName'),
      'id': 'experienceName',
      'defaultValue': '',
      'widgetType': 'input',
      'placeholder': BkDevopsAppi18n.translate('appNamePlaceholder'),
      'desc': BkDevopsAppi18n.translate('appNameDesc'),
    },
    {
      'label': BkDevopsAppi18n.translate('vertionTitle'),
      'id': 'versionTitle',
      'defaultValue': '',
      'widgetType': 'input',
      'required': true,
      'placeholder': BkDevopsAppi18n.translate('versionTitlePlaceholder'),
      'desc': BkDevopsAppi18n.translate('versionTitleDesc'),
    },
    {
      'label': BkDevopsAppi18n.translate('versionDesc'),
      'id': 'remark',
      'defaultValue': '',
      'widgetType': 'input',
      'maxLines': 3,
      'placeholder': BkDevopsAppi18n.translate('versionDescPlaceholder'),
    },
    {
      'label': BkDevopsAppi18n.translate('productType'),
      'id': 'categoryId',
      'defaultValue': '1',
      'widgetType': 'select',
      'options': CATEGORY_MAP.entries
          .map((entry) => {
                'key': '${entry.key}',
                'value': BkDevopsAppi18n.translate(entry.value),
              })
          .toList(),
    },
    {
      'label': BkDevopsAppi18n.translate('expireDate'),
      'id': 'expireDate',
      'defaultValue': 0,
      'widgetType': 'datePicker',
      'placeholder': BkDevopsAppi18n.translate('expireDatePlaceholder'),
      'required': true,
    },
    {
      'label': BkDevopsAppi18n.translate('pm'),
      'id': 'productOwner',
      'defaultValue': '',
      'widgetType': 'input',
      'required': true,
      'placeholder': BkDevopsAppi18n.translate('pmPlaceholder'),
      'desc': BkDevopsAppi18n.translate('pmDesc'),
    },
    {
      'label': BkDevopsAppi18n.translate('experienceGroup'),
      'id': 'experienceGroups',
      'defaultValue': [],
      'widgetType': 'multiple',
      'required': true,
      'placeholder': BkDevopsAppi18n.translate('experienceGroupPlaceholder'),
      'isArray': true,
      'options': [],
    },
    {
      'label': BkDevopsAppi18n.translate('innerStaff'),
      'id': 'innerUsers',
      'defaultValue': '',
      'widgetType': 'input',
      'placeholder': BkDevopsAppi18n.translate('pmPlaceholder'),
      'desc': BkDevopsAppi18n.translate('innerStaffDesc'),
    },
    {
      'label': BkDevopsAppi18n.translate('externalStaff'),
      'id': 'outerUsers',
      'defaultValue': [],
      'widgetType': 'multiple',
      'placeholder': BkDevopsAppi18n.translate('externalStaffPlaceholder'),
      'isArray': true,
      'options': [],
    },
    {
      'label': BkDevopsAppi18n.translate('notifyType'),
      'id': 'notifyTypes',
      'defaultValue': [],
      'widgetType': 'multiple',
      'placeholder': BkDevopsAppi18n.translate('notifyTypePlaceholder'),
      'isArray': true,
      'options': [
        {'key': 'RTX', 'value': 'wework'},
        {'key': 'EMAIL', 'value': 'email'},
      ]
    },
    {
      'label': BkDevopsAppi18n.translate('weworkGroupId'),
      'id': 'wechatGroups',
      'defaultValue': '',
      'widgetType': 'input',
      'placeholder': BkDevopsAppi18n.translate('weworkGroupIdPlaceholder'),
      'desc': BkDevopsAppi18n.translate('weworkGroupIdDesc'),
    }
  ];
  Map paramValue = {};
  bool hasInit = false;

  Future handleSubmit(Map data) async {
    // 深拷贝要提交的数据map
    Map value = json.decode(json.encode(data));
    // change type of innerUser and productOwner
    value['productOwner'] = value['productOwner'].split(',');
    value['innerUsers'] = value['innerUsers'].split(',');
    value['expireDate'] =
        value['expireDate'] > 0 ? (value['expireDate'] / 1000) : 0;

    try {
      await BkLoading.of(context)
          .during(ajax.post(
        '/experience/api/app/experiences/${widget.projectId}',
        data: value,
      ))
          .whenComplete(() {
        toast(BkDevopsAppi18n.of(context).$t('createExpSucc'));
      });

      Navigator.pop(context);
    } catch (e) {
      print(e);
      toast(BkDevopsAppi18n.of(context).$t('createExpFail'));
    }
  }

  _getExpGroups(result) {
    List tmpList = result['records'] ?? [];
    List groupOptions = [];
    tmpList.forEach((e) {
      final group = {'key': e['groupHashId'], 'value': e['name']};
      groupOptions.add(group);
    });
    final expGroupItem =
        paramList.where((item) => item['id'] == 'experienceGroups').toList();
    expGroupItem[0]['options'] = groupOptions;
  }

  _getOutUsers(result) {
    List tmpList = result ?? [];
    List outUserOptions = [];
    tmpList.forEach((e) {
      final user = {'key': e['username'], 'value': e['username']};
      outUserOptions.add(user);
    });
    final outUserItem =
        paramList.where((item) => item['id'] == 'outerUsers').toList();
    outUserItem[0]['options'] = outUserOptions;
  }

  _getDefaultData(lastParamsRes) {
    // 表单默认值赋值
    paramList.forEach((item) {
      paramValue[item['id']] = item['defaultValue'];
    });
    // 若有上一次参数记录，回填参数
    if (lastParamsRes['exist'] == true &&
        lastParamsRes['experienceCreate'] is Map) {
      Map lastParams = lastParamsRes['experienceCreate'];

      if (lastParams['productOwner'] is List) {
        lastParams['productOwner'] = lastParams['productOwner'].join(',');
      }
      if (lastParams['innerUsers'] is List) {
        lastParams['innerUsers'] = lastParams['innerUsers'].join(',');
      }
      if (lastParams['categoryId'] is int) {
        lastParams['categoryId'] = lastParams['categoryId'].toString();
      }

      // 结束日期、标题、描述字段留空
      lastParams['expireDate'] = 0;
      lastParams['versionTitle'] = '';
      lastParams['remark'] = '';

      paramValue.addAll(lastParams);
    }
    // 增加隐藏参数字段
    final artifactMap = {
      'name': widget.artifact['name'],
      'path': widget.artifact['path'],
      'artifactoryType': widget.artifact['artifactoryType']
    };
    paramValue.addAll(artifactMap);
  }

  Future initData() async {
    final apis = [
      '/experience/api/app/experiences/lastParams?projectId=${widget.projectId}&name=${widget.artifact["name"]}&bundleIdentifier=${widget.artifact["bundleIdentifier"]}',
      '/experience/api/app/experience/group/${widget.projectId}/list?returnPublic=true',
      '/experience/api/app/experiences/outer/list?projectId=${widget.projectId}'
    ];
    try {
      final List responses = await Future.wait(
        apis.map(
          (String e) => ajax.get(e),
        ),
      );

      _getDefaultData(responses[0].data);
      _getExpGroups(responses[1].data);
      _getOutUsers(responses[2].data);

      setState(() {
        hasInit = true;
      });
    } catch (err) {
      print(err);
    }
  }

  @override
  void initState() {
    super.initState();
    initData();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: BkAppBar(
        shadowColor: Colors.transparent,
        title: widget.artifact['name'] ?? '转体验',
      ),
      body: !hasInit
          ? Center(
              child: CircularProgressIndicator(
                backgroundColor: Theme.of(context).primaryColor,
              ),
            )
          : SingleChildScrollView(
              keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
              child: GestureDetector(
                behavior: HitTestBehavior.translucent,
                onTap: () {
                  FocusScope.of(context).requestFocus(FocusNode());
                },
                child: Container(
                  margin: EdgeInsets.only(top: 16.px),
                  padding: EdgeInsets.only(left: 32.px, right: 32.px),
                  color: Colors.white,
                  child: FormList(
                    paramList: paramList,
                    paramValue: paramValue,
                    buttonText: BkDevopsAppi18n.of(context).$t('createExp'),
                    submit: handleSubmit,
                  ),
                ),
              ),
            ),
    );
  }
}
