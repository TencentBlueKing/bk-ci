import 'dart:io';
import 'dart:convert';
import 'dart:isolate';
import 'dart:ui';

import 'package:bkci_app/utils/util.dart';
import 'package:bkci_app/models/Artifactory.dart';
import 'package:bkci_app/models/DownloadJobInfo.dart';
import 'package:bkci_app/models/Pkg.dart';
import 'package:bkci_app/models/downloadUrl.dart';
import 'package:bkci_app/utils/Storage.dart';
import 'package:bkci_app/utils/constants.dart';
import 'package:bkci_app/utils/request.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_downloader/flutter_downloader.dart';
import 'package:path_provider/path_provider.dart';

class DownloadJob {
  DownloadJobInfo jobInfo;
  DownloadTask task;
  int progress;
  DownloadTaskStatus status;
  String taskId;

  DownloadJob({
    this.jobInfo,
    this.task,
    this.progress,
    this.status,
    this.taskId,
  });
}

class DownloadProvider with ChangeNotifier, DiagnosticableTreeMixin {
  static const String INS_NAME = 'progress_port';
  ReceivePort _port = ReceivePort();
  List<DownloadJob> _downloadJobs = [];

  List<DownloadJob> get downloadJobs {
    return _downloadJobs;
  }

  Future<DownloadUrl> getDownloadUrl(String expId) async {
    final result = await ajax.post('$EXPERIENCE_API_PREFIX/$expId/downloadUrl');

    return DownloadUrl.fromJson(result.data);
  }

  Future getArtifactoryDownloadUrl(
      String projectId, String artifactoryType, String path) async {
    final result = await ajax.post(
        '$ARTIFACTORY_API_PREFIX/$projectId/$artifactoryType/externalUrl?path=${Uri.encodeComponent(path)}');
    return DownloadUrl.fromJson(result.data);
  }

  DownloadProvider() {
    getAllDownloadTasks();
    FlutterDownloader.registerCallback(downloadCallback);
    _bindBackgroundIsolate();
  }

  String parseFileName(String url) {
    final last = url.split('/').last;
    return last.split('?').first;
  }

  Future downloadExp(
    String bundleIdentifier,
    String expId,
    String name,
    String logoUrl,
    int createTime,
    int size,
  ) async {
    final downloadUrl = await getDownloadUrl(expId);

    await download(
      downloadUrl,
      bundleIdentifier,
      expId,
      name,
      logoUrl,
      createTime,
      size,
      DownloadJobType.Exp,
    );
  }

  Future downloadArtifact(
    Artifactory artifactory,
    String projectId,
  ) async {
    final downloadUrl = await getArtifactoryDownloadUrl(
      projectId,
      artifactory.artifactoryType,
      artifactory.fullPath,
    );

    await download(
      downloadUrl,
      artifactory.bundleIdentifier,
      artifactory.uniqueId,
      artifactory.name,
      artifactory.logoUrl,
      artifactory.modifiedTime,
      artifactory.size,
      DownloadJobType.Artifact,
    );
  }

  Future download(
    DownloadUrl downloadUrl,
    String bundleIdentifier,
    String expId,
    String name,
    String logoUrl,
    int createTime,
    int size,
    DownloadJobType jobType,
  ) async {
    final preResults = await getExternalStorageDirectory();
    Directory savedDir = preResults;
    int timeStamp = DateTime.now().millisecondsSinceEpoch;
    String fileName =
        timeStamp.toString() + '~' + parseFileName(downloadUrl.url);
    String destination = '${savedDir.path}/$fileName';

    final taskId = await FlutterDownloader.enqueue(
      url: downloadUrl.url,
      fileName: fileName,
      savedDir: savedDir.path,
      mimeType: 'application/vnd.android.package-archive',
      showNotification:
          false, // show download progress in status bar (for Android)
      openFileFromNotification:
          false, // click on notification to open downloaded file (for Android)
    );
    final job = DownloadJobInfo(
      bundleIdentifier: bundleIdentifier,
      id: taskId,
      url: downloadUrl.url,
      destination: destination,
      expId: expId,
      logoUrl: logoUrl,
      size: size,
      platform: downloadUrl.platform,
      name: name,
      createTime: timeStamp,
      jobType: jobType,
    );

    await Storage.setString(taskId, jsonEncode(job));
  }

  // Future<String> getObbDest(String bundleId) async {
  //   final Directory dir = await getExternalStorageDirectory();
  //   final Directory dest = Directory(
  //       dir.parent.parent.parent.parent.path + '/Android/obb/' + bundleId);
  //   if (!await dest.exists()) {
  //     await dest.create(recursive: true);
  //   }
  //   return dest.path;
  // }

  Future upgradeAll(List<Pkg> list) async {
    final result = await Future.wait(
      list.map(
        (Pkg e) => downloadExp(
          e.bundleIdentifier,
          e.experienceHashId,
          e.experienceName,
          e.logoUrl,
          e.createTime,
          e.size,
        ),
      ),
    );
    await getAllDownloadTasks();
    return result;
  }

  Future removeDownloadByTaskId(String taskId, bool removeFile) async {
    await Storage.storage.remove(taskId);
    _downloadJobs.removeWhere((element) => element.taskId == taskId);

    await FlutterDownloader.remove(
      taskId: taskId,
      shouldDeleteContent: removeFile,
    );

    notifyListeners();
  }

  Future getAllDownloadTasks() async {
    final tasks = await FlutterDownloader.loadTasks();

    final List<DownloadJob> result = [];
    for (final task in tasks) {
      final job = Storage.getString(task.taskId);

      DownloadJobInfo jobInfo;
      if (job != null) {
        jobInfo = DownloadJobInfo.fromJson(jsonDecode(job));

        result.add(DownloadJob(
          jobInfo: jobInfo,
          task: task,
          progress: task.progress,
          status: task.status,
          taskId: task.taskId,
        ));
      }
    }
    _downloadJobs = result;

    notifyListeners();
  }

  List<DownloadJobInfo> getTypedTasks(DownloadTaskStatus type) {
    final tasks = _downloadJobs
        .where((task) => task.status == type)
        .map((e) => e.jobInfo)
        .toList();
    tasks.sort((task1, task2) => task2.createTime.compareTo(task1.createTime));
    return tasks;
  }

  List<DownloadJobInfo> getDownloadingTasks() {
    final tasks = _downloadJobs
        .where((task) => [
              DownloadTaskStatus.running,
              DownloadTaskStatus.enqueued,
              DownloadTaskStatus.failed,
              DownloadTaskStatus.paused,
            ].contains(task.status))
        .map((e) => e.jobInfo)
        .toList();
    return tasks;
  }

  Future open(String taskId) async {
    if (taskId != null || taskId != '') {
      DownloadJob job = _downloadJobs.find<DownloadJob>(
        (DownloadJob job) => job.taskId == taskId,
      );
      bool isPkgExist = File(job.jobInfo.destination).existsSync();

      if (!isPkgExist) {
        toast('安装包已经被删除，请重新下载');
        removeDownloadByTaskId(taskId, true);
        return false;
      }
      final result = await FlutterDownloader.open(taskId: taskId);
      await Storage.setCurrentInstallingExpId(job.jobInfo.expId);
      return result;
    }
    return false;
  }

  Future pause(String taskId) async {
    if (taskId != null || taskId != '') {
      final result = await FlutterDownloader.pause(taskId: taskId);
      if (result) {
        toast('暂停成功');
        return result;
      }
    }
    toast('暂停失败');
    return false;
  }

  Future resume(String taskId) async {
    try {
      if (taskId != null || taskId != '') {
        final String newTaskId = await FlutterDownloader.resume(taskId: taskId);
        if (newTaskId == null) {
          throw ('启动失败');
        }
        updateTask(taskId, newTaskId);
        toast('启动成功');
        return newTaskId;
      }
      return false;
    } catch (e) {
      toast('启动任务失败，文件已丢失，请重新开始下载');
      await removeDownloadByTaskId(taskId, true);
      return false;
    }
  }

  Future retry(String taskId) async {
    if (taskId != null || taskId != '') {
      final String newTaskId = await FlutterDownloader.retry(taskId: taskId);
      updateTask(taskId, newTaskId);

      toast('任务重试成功');
      return newTaskId;
    }
    return false;
  }

  Future updateTask(taskId, newTaskId) async {
    DownloadJob task = _downloadJobs.find<DownloadJob>(
      (DownloadJob task) => task.taskId == taskId,
    );
    if (task != null && newTaskId != null) {
      task.taskId = newTaskId;
      task.jobInfo.id = newTaskId;

      await Storage.setString(newTaskId, jsonEncode(task.jobInfo));
      await Storage.storage.remove(taskId);
      await removeDownloadByTaskId(taskId, true);
    }
  }

  Future deleteAll(List<DownloadJobInfo> list,
      [bool isDeleteFile = false]) async {
    await Future.wait(
      list.map(
        (DownloadJobInfo job) {
          return removeDownloadByTaskId(job.id, isDeleteFile);
        },
      ),
    );
    toast('任务删除成功');
  }

  Future pauseAll([List<DownloadJob> list = const []]) async {
    if (list.length == 0) list = _downloadJobs;
    await Future.wait(
      list.map(
        (DownloadJob job) => FlutterDownloader.pause(taskId: job.taskId),
      ),
    );
    toast('任务暂停成功');
  }

  Future resumeAll(List<DownloadJob> list) async {
    await Future.wait(
      list.map(
        (DownloadJob job) => resume(job.taskId),
      ),
    );
    toast('任务恢复成功');
  }

  void _bindBackgroundIsolate() {
    bool isSuccess =
        IsolateNameServer.registerPortWithName(_port.sendPort, INS_NAME);
    if (!isSuccess) {
      _unbindBackgroundIsolate();
      _bindBackgroundIsolate();
      return;
    }

    _port.listen((dynamic data) {
      String id = data[0];
      DownloadTaskStatus status = data[1];
      int progress = data[2];
      if (_downloadJobs != null && _downloadJobs.isNotEmpty) {
        DownloadJob task = _downloadJobs.find<DownloadJob>(
          (DownloadJob task) => task.taskId == id,
        );
        if (task != null) {
          task.status = status;
          task.progress = progress;
          notifyListeners();

          if (status == DownloadTaskStatus.complete) {
            Future.delayed(Duration.zero, () {
              open(id);
            });
          }
        }
      }
    });
  }

  void _unbindBackgroundIsolate() {
    IsolateNameServer.removePortNameMapping(INS_NAME);
  }

  static void downloadCallback(
    String id,
    DownloadTaskStatus status,
    int progress,
  ) {
    print(
        'Background Isolate Callback: task ($id) is in status ($status) and process ($progress)');
    final SendPort send = IsolateNameServer.lookupPortByName(INS_NAME);
    send.send([id, status, progress]);
  }
}
