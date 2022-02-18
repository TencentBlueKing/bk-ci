import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/ProjectLogo.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/models/project.dart';

class ProjectItem extends StatelessWidget {
  final Project projectItem;
  final Function onTap;
  final bool selected;

  ProjectItem({this.projectItem, this.onTap, this.selected = false});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 87.px,
      child: ListTile(
        contentPadding: EdgeInsets.symmetric(horizontal: 0.px),
        leading: ProjectLogo(
          showBorder: false,
          logoUrl: projectItem.logoUrl,
          isStreamProject: projectItem.isStreamProject,
        ),
        title: PFText(
          projectItem.projectName ?? '',
          overflow: TextOverflow.ellipsis,
          maxLines: 1,
          style: TextStyle(
            fontSize: 28.px,
          ),
        ),
        trailing: Offstage(
          offstage: !selected,
          child: Container(
            width: 80.px,
            child: Center(
                child: Icon(
              BkIcons.checkSmall,
              color: Theme.of(context).primaryColor,
            )),
          ),
        ),
        onTap: () {
          onTap(projectItem.toJson());
        },
      ),
    );
  }
}
