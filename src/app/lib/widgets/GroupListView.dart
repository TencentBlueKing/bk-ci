import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class GroupListView<T> extends StatelessWidget {
  final List list;
  final Function(BuildContext context, T item) itemBuilder;
  final Function(T item) groupBy;
  final Function(BuildContext context, dynamic param, T item)
      groupHeaderBuilder;

  GroupListView({
    this.list,
    this.itemBuilder,
    this.groupBy,
    this.groupHeaderBuilder,
  });

  // List formatList(BuildContext context, list) {
  //   new Map.fromIterable(
  //     list,
  //     key: groupBy,
  //     value: (value) {
  //       return list.where((item) => item.date == value.date).toList();
  //     },
  //   ).forEach((key, value) {
  //     sectionResult.add(key);
  //     sectionResult.addAll(value);
  //   });
  //   return result;
  // }

  @override
  Widget build(BuildContext context) {
    // final list = formatSections(context, sections);
    return ListView.separated(
      itemBuilder: (context, index) {
        final item = list[index];
        return item is Widget ? item : itemBuilder(context, item);
      },
      separatorBuilder: (context, index) {
        final nextItem = list[index + 1];
        return nextItem is Widget
            ? Container(
                padding: EdgeInsets.only(top: 15.px),
                child: Divider(
                  height: 1.px,
                ),
              )
            : SizedBox(
                height: 0,
              );
      },
      itemCount: list.length,
    );
  }
}
