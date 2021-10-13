import 'dart:convert';
import 'dart:io';

import 'package:bkci_app/models/ExecuteModel.dart';
import 'package:bkci_app/providers/PollProvider.dart';
import 'package:bkci_app/providers/UserProvider.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/widgets/ExecuteDetail/CheckInfo.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:webview_flutter/webview_flutter.dart';

class ExecuteChoreography extends StatefulWidget {
  final ExecuteModel args;

  ExecuteChoreography({
    this.args,
  });

  @override
  _ExecuteChoreographyState createState() => _ExecuteChoreographyState();
}

class _ExecuteChoreographyState extends State<ExecuteChoreography> {
  JavascriptChannel _javascriptChannel;
  WebViewController _controller;
  bool _loading = false;
  String prefix = '';

  @override
  void initState() {
    super.initState();
    prefix =
        '/process/api/app/pipelineBuild/${widget.args.projectId}/${widget.args.pipelineId}/${widget.args.buildId}';
    if (Platform.isAndroid) WebView.platform = SurfaceAndroidWebView();
    _javascriptChannel = getJavascriptChannel();
    _loading = true;
  }

  @override
  void didUpdateWidget(ExecuteChoreography oldWidget) {
    super.didUpdateWidget(oldWidget);

    updateChoreography(widget.args);
  }

  JavascriptChannel getJavascriptChannel() {
    return JavascriptChannel(
      name: 'BkDevOps',
      onMessageReceived: _handleJsMessage,
    );
  }

  Future getExecuteDetail() async {
    final response = await ajax.get(
        '/process/api/app/pipelineBuild/${widget.args.projectId}/${widget.args.pipelineId}/${widget.args.buildId}/detail');
    final result = ExecuteModel.fromJson(response.data);
    updateChoreography(result);
  }

  updateChoreography(ExecuteModel detail) {
    if (_controller != null) {
      _controller.evaluateJavascript(_getInitScriptString(detail));
    }
    if (mounted && detail != null) {
      setState(() {
        _loading = false;
      });
    }
  }

  _handleWebviewCreated(WebViewController controller) {
    _controller = controller;
  }

  _getInitScriptString(ExecuteModel result) {
    final userInfo = json.encode({
      'username': Provider.of<User>(context, listen: false).user?.englishName,
    });
    final stages = json.encode(result?.model?.stages ?? []);

    return '''
      (function () {
        try {
          render({
            stages: $stages,
            userInfo: $userInfo,
            reviewQualityAtom: function (payload) {
                BkDevOps.postMessage(JSON.stringify({
                    action: "_reviewQualityAtom",
                    payload
                }))
            },
            handleAtomCheck: function (payload) {
                BkDevOps.postMessage(JSON.stringify({
                    action: "_handleAtomCheck",
                    payload
                }))
            },
            startNextStage: function (payload) {
                BkDevOps.postMessage(JSON.stringify({
                    action: "_handleStageReview",
                    payload
                }))
            },
            retryPipeline: function (taskId) {
                BkDevOps.postMessage(JSON.stringify({
                    action: "_retryPipeline",
                    payload: {
                        taskId
                    }
                }))
            }
        })
        } catch(e) {
          console.log(e);
          BkDevOps.postMessage(e.toString());
        }
    })()
    ''';
  }

  _handleWebviewStarted(String url) {
    print('_handleWebviewStarted, $url');
  }

  _handlePageFinish(String url) {
    updateChoreography(widget.args);
  }

  _handleJsMessage(JavascriptMessage message) {
    print('JavascriptMessage: $message, ${message.message}');
    final msg = json.decode(message.message);
    switch (msg['action']) {
      case "_reviewQualityAtom":
        _reviewQualityAtom(msg['payload']);
        break;
      case "_handleAtomCheck":
        _handleAtomCheck(msg['payload']);
        break;
      case "_handleStageReview":
        _handleStageReview(msg['payload']);
        break;
      case "_retryPipeline":
        _retryPipeline(msg['payload']);
        break;
    }
  }

  Future _reviewQualityAtom(payload) async {
    print('_reviewQualityAtom, $payload');
    await ajax.post(
        '$prefix/${payload["atomId"]}/qualityGateReview/${payload["action"]}');
    updateDetail();
  }

  Future _handleAtomCheck(payload) async {
    print('_handleAtomCheck, $payload');
    this.showCheckParams('_handleAtomCheck', payload);
  }

  Future _handleStageReview(payload) async {
    print('_handleStageReview, $payload');
    this.showCheckParams('_handleStageReview', payload);
  }

  Future _retryPipeline(payload) async {
    await ajax.post('$prefix/retry?taskId=${payload["taskId"]}');
    toast('流水线重试成功');
    updateDetail();
  }

  updateDetail() async {
    final PollDataModel<ExecuteModel> provider =
        Provider.of<PollDataModel<ExecuteModel>>(
      context,
      listen: false,
    );

    await provider.provider.fetchData();
    provider.provider.startPolling();
  }

  void showCheckParams(action, payload) {
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
        return Container(
          height: SizeFit.deviceHeight * 0.85,
          padding:
              EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
          child: CheckInfo(
            action: action,
            payload: payload,
            prefix: action == '_handleStageReview'
                ? '/process/api/app/pipelineBuild/projects/${widget.args.projectId}/pipelines/${widget.args.pipelineId}/builds/${widget.args.buildId}'
                : prefix,
            submit: updateDetail,
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        WebView(
          initialUrl: CHOREOGRAPHY_RESOURCE_URL,
          onWebViewCreated: _handleWebviewCreated,
          onPageStarted: _handleWebviewStarted,
          onPageFinished: _handlePageFinish,
          onWebResourceError: (error) {
            print('error, $error');
          },
          javascriptMode: JavascriptMode.unrestricted,
          javascriptChannels: <JavascriptChannel>[_javascriptChannel].toSet(),
        ),
        _loading
            ? Center(
                child: CircularProgressIndicator(
                  backgroundColor: Theme.of(context).primaryColor,
                ),
              )
            : SizedBox(),
      ],
    );
  }
}
