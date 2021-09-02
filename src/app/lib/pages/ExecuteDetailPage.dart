import 'dart:async';

import 'package:bkci_app/models/ExecuteModel.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/widgets/ExecuteDetail/ExecuteContent.dart';
import 'package:bkci_app/widgets/PeriodicSyncBuilder.dart';
import 'package:flutter/material.dart';

class ExecuteDetailPageArgs {
  final String projectId;
  final String pipelineId;
  final String buildId;
  final int initialIndex;
  final ExecuteModel initModel;

  ExecuteDetailPageArgs({
    this.projectId,
    this.pipelineId,
    this.buildId,
    this.initialIndex,
    this.initModel,
  });
}

class ExecuteDetailPage extends StatelessWidget {
  static const String routePath = '/executeDetail';
  final ExecuteDetailPageArgs args;
  final String urlPrefix;

  ExecuteDetailPage({
    Key key,
    this.args,
  })  : urlPrefix =
            '/process/api/app/pipelineBuild/${args.projectId}/${args.pipelineId}/${args.buildId}',
        super(key: key);

  Future<ExecuteModel> getExecuteDetail() async {
    final response = await ajax.get('$urlPrefix/detail');
    final result = ExecuteModel.fromJson(response.data);

    return result;
  }

  bool _shouldContinueTask(detail, _) {
    return detail != null && !detail.isDone;
  }

  @override
  Widget build(BuildContext context) {
    return PeriodicSyncBuilder<ExecuteModel>(
      future: getExecuteDetail,
      duration: Duration(seconds: 5),
      initialData: args.initModel,
      shouldContinueTask: _shouldContinueTask,
      child: ExecuteContent(
        initialIndex: args.initialIndex,
        urlPrefix: urlPrefix,
      ),
    );
  }
}
