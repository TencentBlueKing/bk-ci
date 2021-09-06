import 'package:bkci_app/models/ExecuteModel.dart';
import 'package:bkci_app/widgets/BuildLog.dart';
import 'package:flutter/material.dart';

class ExecuteLog extends StatelessWidget {
  final ExecuteModel args;

  ExecuteLog({
    this.args,
  });

  @override
  Widget build(BuildContext context) {
    return BuildLog(
      projectId: args.projectId,
      pipelineId: args.pipelineId,
      buildId: args.buildId,
    );
  }
}
