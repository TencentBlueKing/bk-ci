import 'package:bkci_app/providers/ProjectProvider.dart';
import 'package:bkci_app/utils/i18n.dart';
import 'package:bkci_app/widgets/Empty.dart';
import 'package:bkci_app/widgets/PFText.dart';
import 'package:bkci_app/widgets/ProjectLogo.dart';
import 'package:provider/provider.dart';
import 'package:flutter/material.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/main.dart';
import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:bkci_app/pages/SearchPipeline.dart';
import 'package:bkci_app/widgets/pipelineListTab/List.dart';
import 'package:bkci_app/widgets/pipelineListTab/SelectProject.dart';
import 'package:bkci_app/widgets/pipelineListTab/SelectView.dart';
import 'package:bkci_app/models/project.dart';

class PipelineScreen extends StatefulWidget {
  static const routePath = '/pipeline';

  @override
  _PipelineState createState() => _PipelineState();
}

class _PipelineState extends State<PipelineScreen> {
  showBottom() {
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
        return SelectProject();
      },
    );
  }

  showSelectView() {
    final projectProvider = Provider.of<ProjectInfoProvider>(
      context,
      listen: false,
    );
    Project project = projectProvider.currentProject;
    Map view = projectProvider.currentView;
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
        return SelectView(
            projectCode: project?.projectCode,
            currentView: view['id'] ?? 'myPipeline');
      },
    );
  }

  Scaffold buildNoProjectBox(String i18n(String text)) {
    return Scaffold(
      backgroundColor: '#F0F1F5'.color,
      appBar: AppBar(
        leading: Padding(
          child: Icon(
            BkIcons.placeholder,
            color: '#999999'.color,
          ),
          padding: EdgeInsets.only(
            left: 32.px,
          ),
        ),
        leadingWidth: 56.px,
        title: PFMediumText(
          i18n('noProject'),
          style: TextStyle(
            color: Colors.black,
            fontSize: 36.px,
          ),
        ),
      ),
      body: Empty(
        title: i18n('noAccessProject'),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final i18n = BkDevopsAppi18n.of(context).$t;
    return Consumer<ProjectInfoProvider>(
      builder: (BuildContext context, value, child) {
        if (value.loading) {
          return Container(
            color: Colors.white,
            child: Center(
              child: CircularProgressIndicator(
                backgroundColor: Theme.of(context).primaryColor,
              ),
            ),
          );
        }
        if (!value.hasProject) {
          return buildNoProjectBox(i18n);
        }
        return Scaffold(
          backgroundColor: Colors.white,
          body: SafeArea(
            child: Column(
              children: [
                AppBar(
                  shadowColor: Colors.transparent,
                  leadingWidth: 88.px,
                  titleSpacing: 0,
                  leading: GestureDetector(
                    onTap: showBottom,
                    child: Center(
                      child: ProjectLogo(
                        margin: EdgeInsets.only(left: 32.px),
                        logoUrl: value.currentProject.logoUrl,
                        borderRadius: 12.px,
                        isStreamProject: value.currentProject.isStreamProject,
                      ),
                    ),
                  ),
                  title: GestureDetector(
                    onTap: showBottom,
                    child: Container(
                      padding: EdgeInsets.symmetric(horizontal: 16.px),
                      child: Row(
                        children: [
                          PFMediumText(
                            value.currentProject.projectName ?? '',
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              color: Colors.black,
                              fontSize: 36.px,
                            ),
                          ),
                          Icon(
                            BkIcons.downFill,
                            size: 44.px,
                            color: Theme.of(context).hintColor,
                          ),
                        ],
                      ),
                    ),
                  ),
                  actions: [
                    Padding(
                      padding: EdgeInsets.only(right: 32.px),
                      child: IconButton(
                        icon: Icon(
                          BkIcons.search,
                          size: 44.px,
                          color: Colors.black,
                        ),
                        onPressed: () {
                          SearchPipelineArgument args = SearchPipelineArgument(
                            projectCode: value.currentProject.projectCode,
                          );
                          DevopsApp.navigatorKey.currentState.pushNamed(
                            SearchPipeline.routePath,
                            arguments: args,
                          );
                        },
                      ),
                    ),
                  ],
                ),
                Container(
                  padding: EdgeInsets.fromLTRB(32.px, 0.px, 32.px, 0.px),
                  height: 88.px,
                  width: 750.px,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    border: Border(
                      top: BorderSide(
                          width: .5, color: Theme.of(context).dividerColor),
                      bottom: BorderSide(
                          width: .5, color: Theme.of(context).dividerColor),
                    ),
                  ),
                  child: InkWell(
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.center,
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Container(
                          padding:
                              EdgeInsets.fromLTRB(24.px, 24.px, 24.px, 20.px),
                          decoration: BoxDecoration(
                            border: Border(
                              bottom: BorderSide(
                                  width: 4.px,
                                  color: Theme.of(context).primaryColor),
                            ),
                          ),
                          child: PFMediumText(
                            (value.currentView['name'] == 'myPipeline'
                                    ? i18n('myPipeline')
                                    : value.currentView['name']) ??
                                '',
                            style: TextStyle(
                              color: Theme.of(context).primaryColor,
                              fontSize: 30.px,
                            ),
                          ),
                        ),
                        Icon(BkIcons.down),
                      ],
                    ),
                    onTap: showSelectView,
                  ),
                ),
                Expanded(
                  child: PipelineList(
                    viewId: value.currentView['id'],
                    projectId: value.currentProject.projectCode,
                  ),
                )
              ],
            ),
          ),
        );
      },
    );
  }
}
