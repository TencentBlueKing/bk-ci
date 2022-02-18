import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:flutter/cupertino.dart';
import 'package:bkci_app/utils/request.dart';

class FilterBuild extends StatefulWidget {
  final String projectId;
  final String pipelineId;
  final Function handleFilter;
  final Map<String, dynamic> queryCondition;

  FilterBuild({
    this.projectId,
    this.pipelineId,
    this.queryCondition,
    this.handleFilter,
  });

  _FilterBuildState createState() => _FilterBuildState();
}

class _FilterBuildState extends State<FilterBuild> {
  final TextEditingController _controller = new TextEditingController();

  Map<String, dynamic> result = {
    'buildMsg': '',
    'status': [],
    'materialBranch': [],
    'materialUrl': []
  };

  List renderList = [
    {
      'key': 'status',
      'labelText': 'status',
      'data': [
        {'id': 'SUCCEED', 'value': 'successStatus'},
        {'id': 'FAILED', 'value': 'failStatus'},
        {'id': 'RUNNING', 'value': 'runningStatus'},
        {'id': 'CANCELED', 'value': 'cancelStatus'},
        {'id': 'REVIEWING', 'value': 'reviewStatus'},
        {'id': 'REVIEW_ABORT', 'value': 'abortStatus'},
        {'id': 'REVIEW_PROCESSED', 'value': 'passStatus'},
        {'id': 'PREPARE_ENV', 'value': 'queuedStatus'},
        {'id': 'STAGE_SUCCESS', 'value': 'stageSuccessStatus'}
      ],
      'isOpen': true,
      'itemWidth': 180,
      'align': 'center'
    },
    {
      'key': 'materialBranch',
      'labelText': 'branch',
      'data': [],
      'isOpen': false,
      'itemWidth': 580,
      'align': 'left'
    },
    {
      'key': 'materialUrl',
      'labelText': 'codelib',
      'data': [],
      'isOpen': false,
      'itemWidth': 580,
      'align': 'left'
    }
  ];

  Widget labelWidget(String text) {
    return PFMediumText(
      BkDevopsAppi18n.of(context).$t(text),
      style: TextStyle(
        fontSize: 28.px,
      ),
    );
  }

  Widget flexWidget(
      String key, List data, List selected, int width, String align) {
    final itemList = data.map(
      (item) {
        String id, value;
        if (key == 'status') {
          id = item['id'];
          value = item['value'];
        } else {
          id = item;
          value = item;
        }
        bool inList = selected.indexOf(id) >= 0;
        final theme = Theme.of(context);
        Color bgColor =
            inList ? '#E1ECFF'.color : theme.scaffoldBackgroundColor;
        Color borderColor =
            inList ? theme.primaryColor : theme.scaffoldBackgroundColor;
        Color textColor =
            inList ? theme.primaryColor : theme.secondaryHeaderColor;

        return InkWell(
          child: Container(
            width: width.px,
            padding: EdgeInsets.symmetric(vertical: 12.px, horizontal: 15.px),
            alignment:
                align == 'left' ? Alignment.centerLeft : Alignment.center,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(6.px),
              color: bgColor,
              border: Border.all(
                color: borderColor,
              ),
            ),
            child: PFText(
              BkDevopsAppi18n.of(context).$t(value),
              maxLines: 5,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 26.px,
                color: textColor,
              ),
            ),
          ),
          onTap: () {
            setState(() {
              if (result[key].indexOf(id) >= 0) {
                result[key].remove(id);
              } else {
                result[key].add(id);
              }
            });
          },
        );
      },
    ).toList();
    return Wrap(spacing: 20.px, runSpacing: 20.px, children: itemList);
  }

  Future fetchCondition() async {
    final apis = ['branchName', 'repo'];

    try {
      final List responses = await Future.wait(
        apis.map(
          (String e) => ajax.get(
              '/process/api/app/pipeline/${widget.projectId}/${widget.pipelineId}/historyCondition/$e'),
        ),
      );
      setState(() {
        renderList[1]['data'] = responses[0].data ?? [];
        renderList[2]['data'] = responses[1].data ?? [];
      });
    } catch (err) {
      print(err);
    }
  }

  @override
  void initState() {
    super.initState();
    this.fetchCondition();

    result = widget.queryCondition;
    _controller.text = result['buildMsg'];
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        SingleChildScrollView(
          child: Container(
            margin: EdgeInsets.fromLTRB(32.px, 20.px, 32.px, 120.px),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: EdgeInsets.only(bottom: 25.px),
                  child: labelWidget(
                    BkDevopsAppi18n.of(context).$t('buildMsg'),
                  ),
                ),
                Container(
                  height: 68.px,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(6.px),
                    border: Border.all(
                      color: Theme.of(context).hintColor,
                    ),
                  ),
                  child: TextField(
                    controller: _controller,
                    textAlignVertical: TextAlignVertical.center,
                    onChanged: (val) {
                      result['buildMsg'] = val;
                    },
                    style: TextStyle(
                      color: Theme.of(context).secondaryHeaderColor,
                      fontSize: 28.px,
                    ),
                    decoration: InputDecoration(
                      isDense: true,
                      contentPadding: EdgeInsets.only(
                        left: 32.px,
                      ),
                      suffixIcon: IconButton(
                        splashColor: Colors.transparent,
                        padding: EdgeInsets.zero,
                        onPressed: () {
                          _controller.text = '';
                          result['buildMsg'] = '';
                        },
                        icon: Icon(
                          BkIcons.closeFill,
                          color: Theme.of(context).hintColor,
                          size: 36.px,
                        ),
                      ),
                      hintText: BkDevopsAppi18n.of(context).$t('keyword'),
                      hintStyle: TextStyle(
                        color: Theme.of(context).hintColor,
                        fontSize: 28.px,
                      ),
                      focusedBorder: InputBorder.none,
                      border: InputBorder.none,
                      enabledBorder: InputBorder.none,
                      errorBorder: InputBorder.none,
                      disabledBorder: InputBorder.none,
                    ),
                  ),
                ),
                for (final item in renderList)
                  Container(
                    margin: EdgeInsets.only(top: 40.px),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                          padding: EdgeInsets.only(bottom: 25.px),
                          child: InkWell(
                              child: Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  labelWidget(item['labelText']),
                                  Icon(
                                    item['isOpen'] == false
                                        ? BkIcons.down
                                        : BkIcons.up,
                                  ),
                                ],
                              ),
                              onTap: () {
                                setState(() {
                                  item['isOpen'] = !item['isOpen'];
                                });
                              }),
                        ),
                        Offstage(
                          offstage: item['isOpen'] == false,
                          child: flexWidget(
                            item['key'],
                            item['data'],
                            result[item['key']],
                            item['itemWidth'],
                            item['align'],
                          ),
                        )
                      ],
                    ),
                  ),
              ],
            ),
          ),
        ),
        Positioned(
          bottom: 0.px,
          child: Container(
            child: Row(
              children: [
                Container(
                  width: 322.px,
                  height: 88.px,
                  child: ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      primary: Colors.white,
                      shape: BeveledRectangleBorder(),
                    ),
                    onPressed: () {
                      setState(() {
                        this.result = {
                          'buildMsg': '',
                          'status': [],
                          'materialBranch': [],
                          'materialUrl': []
                        };
                      });
                      _controller.clear();
                    },
                    child: PFText(
                      BkDevopsAppi18n.of(context).$t('reset'),
                      style: TextStyle(
                        fontSize: 32.px,
                      ),
                    ),
                  ),
                ),
                Container(
                  width: 322.px,
                  height: 88.px,
                  child: ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      shape: BeveledRectangleBorder(),
                    ),
                    onPressed: () => {widget.handleFilter(this.result)},
                    child: PFText(
                      BkDevopsAppi18n.of(context).$t('confirm'),
                      style: TextStyle(
                        fontSize: 32.px,
                        color: Colors.white,
                      ),
                    ),
                  ),
                )
              ],
            ),
          ),
        )
      ],
    );
  }
}
