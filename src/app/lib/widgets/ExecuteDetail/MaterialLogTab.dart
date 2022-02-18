import 'package:bkci_app/models/CommitRecord.dart';
import 'package:bkci_app/models/ExecuteModel.dart';
import 'package:bkci_app/models/MaterialItem.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/InfinityList.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class MaterialLogTab extends StatelessWidget {
  final ExecuteModel args;

  MaterialLogTab({
    this.args,
  });

  Future fetchMaterialLog(int page, int pageSize) async {
    final response = await ajax.get(
        '/repository/api/app/repositories/projects/${args.projectId}/pipelines/${args.pipelineId}/builds/${args.buildId}/commit/get/record');
    final List<MaterialItem> result = [];
    response.data.forEach((ele) {
      result.add(MaterialItem.fromJson(ele));
    });

    return [
      result,
      false,
    ];
  }

  Future refreshMaterialLog(int pageSize) async {
    return fetchMaterialLog(1, pageSize);
  }

  Widget infoRow(BuildContext context, String i18nKey, String value) {
    final rowTextstyle = TextStyle(
      fontSize: 22.px,
      height: 32.px / 22.px,
    );
    return Row(
      children: [
        Container(
          alignment: Alignment.centerLeft,
          width: 138.px,
          height: 32.px,
          child: PFText(
            '${BkDevopsAppi18n.of(context).$t(i18nKey)}：',
            style: rowTextstyle,
          ),
        ),
        Expanded(
          child: PFText(
            value ?? '--',
            maxLines: 1,
            style: rowTextstyle,
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }

  Widget _commitItem(
      BuildContext context, CommitRecord item, int index, int last) {
    return Stack(
      children: [
        Container(
          padding: EdgeInsets.only(bottom: 16.px),
          child: Container(
            decoration: BoxDecoration(
              color: '#F5F6FA'.color,
              borderRadius: BorderRadius.circular(8.px),
            ),
            padding: EdgeInsets.fromLTRB(64.px, 24.px, 24.px, 24.px),
            alignment: Alignment.centerLeft,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text.rich(
                  TextSpan(
                    text: 'Commit：',
                    style: TextStyle(
                      color: '#979BA5'.color,
                      fontFamily: 'PingFang-Medium',
                    ),
                    children: [
                      TextSpan(
                        text: item.comment,
                        style: TextStyle(
                          color: Colors.black,
                          fontFamily: 'PingFang-Medium',
                        ),
                      ),
                    ],
                  ),
                  textAlign: TextAlign.left,
                  maxLines: 5,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: 28.px,
                  ),
                ),
                Container(
                  margin: EdgeInsets.only(top: 8.px),
                  child: Column(
                    children: [
                      infoRow(context, 'operater', item?.committer),
                      infoRow(context, 'time', item?.commitTime?.yMdhms),
                      infoRow(
                        context,
                        'commit',
                        item?.commit?.substring(0, 8),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
        Positioned(
          left: 20.px,
          child: ConstrainedBox(
            constraints: BoxConstraints(
              maxHeight: 252.px,
            ),
            child: Column(
              children: [
                Container(
                  height: 32.px,
                  width: 4.px,
                  color: index != 0
                      ? Theme.of(context).hintColor
                      : Colors.transparent,
                ),
                Container(
                  width: 24.px,
                  height: 24.px,
                  padding: EdgeInsets.all(7.px),
                  decoration: BoxDecoration(
                    color: Colors.black,
                    shape: BoxShape.circle,
                  ),
                  child: Container(
                    decoration: BoxDecoration(
                      color: Colors.white,
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
                if (index != last)
                  Expanded(
                    child: Container(
                      width: 4.px,
                      color: Theme.of(context).hintColor,
                    ),
                  ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _itemBuilder(BuildContext context, MaterialItem item) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          margin: EdgeInsets.only(bottom: 24.px),
          child: PFMediumText(
            item.name,
            style: TextStyle(
              color: Colors.black,
              fontSize: 30.px,
              height: 44.px / 30.px,
            ),
          ),
        ),
        for (final value in item.records)
          _commitItem(
            context,
            value,
            item.records.indexOf(value),
            item.records.length - 1,
          ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(32.px),
      child: InfinityList(
        itemBuilder: (item) => _itemBuilder(context, item),
        onFetchData: fetchMaterialLog,
        onRefresh: refreshMaterialLog,
        dividerBuilder: (
          BuildContext context,
          int index,
          item,
          nextItem,
        ) =>
            Divider(
          height: 58.px,
        ),
      ),
    );
  }
}
