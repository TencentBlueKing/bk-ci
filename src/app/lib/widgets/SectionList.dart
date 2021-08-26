import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';

class SectionData {
  final Widget header;
  final List list;
  SectionData({
    this.header,
    this.list,
  });
}

class SectionList<T> extends StatelessWidget {
  final List sections;
  final Function(BuildContext context, T item) itemBuilder;

  SectionList({this.sections, this.itemBuilder});

  List formatSections(BuildContext context, sections) {
    final List result = [];
    sections.forEach((SectionData section) {
      result.add(section.header);
      result.addAll(section.list);
    });
    return result;
  }

  @override
  Widget build(BuildContext context) {
    final list = formatSections(context, sections);
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
