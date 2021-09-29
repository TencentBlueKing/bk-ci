import 'package:bkci_app/models/ShareArgs.dart';
import 'package:bkci_app/pages/App.dart';
import 'package:bkci_app/pages/AppChangelog.dart';
import 'package:bkci_app/pages/ArtifactoryDetail.dart';
import 'package:bkci_app/pages/DownloadRecords.dart';
import 'package:bkci_app/pages/ExecuteDetailPage.dart';
import 'package:bkci_app/pages/ExpQRCode.dart';
import 'package:bkci_app/pages/Feedback.dart';
import 'package:bkci_app/pages/GuidePage.dart';
import 'package:bkci_app/pages/ITLoginScreen.dart';
import 'package:bkci_app/pages/DetailScreen.dart';
import 'package:bkci_app/pages/ExpHistory.dart';
import 'package:bkci_app/pages/InstallationManagement.dart';
import 'package:bkci_app/pages/LoginScreen.dart';
import 'package:bkci_app/pages/MailLoginScreen.dart';
import 'package:bkci_app/pages/MoreApps.dart';
import 'package:bkci_app/pages/QRScanScreen.dart';
import 'package:bkci_app/pages/SearchScreen.dart';
import 'package:bkci_app/pages/SettingPage.dart';
import 'package:bkci_app/pages/ShareQRCode.dart';
import 'package:bkci_app/pages/SearchPipeline.dart';
import 'package:bkci_app/pages/BuildHistory.dart';
import 'package:bkci_app/pages/CreateExp.dart';
import '../pages/NotFound.dart';
import 'package:flutter/material.dart';

final RouteObserver<PageRoute> routeObserver = RouteObserver<PageRoute>();

class AppRoutes {
  static unknowRoute(RouteSettings setting) {
    return (context) => NotFoundPage(setting.name);
  }

  static onGenerateRoute(RouteSettings settings) {
    WidgetBuilder builder;

    switch (settings.name) {
      case GuidePage.routePath:
        builder = (BuildContext context) => GuidePage();
        break;
      case LoginScreen.routePath:
        builder = (BuildContext context) => LoginScreen();
        break;
      case ITLoginScreen.routePath:
        builder = (BuildContext context) => ITLoginScreen();
        break;
      case MailLoginScreen.routePath:
        builder = (BuildContext context) => MailLoginScreen();
        break;
      case BkDevopsApp.routePath:
        builder = (BuildContext context) => BkDevopsApp();
        break;
      case SearchScreen.routePath:
        final SearchScreenArgs searchArgs = settings.arguments;
        builder = (BuildContext context) => SearchScreen(
              experiencePublic:
                  searchArgs != null ? searchArgs.experiencePublic : true,
            );
        break;
      case SearchPipeline.routePath:
        final SearchPipelineArgument pipelineArgs = settings.arguments;
        builder = (BuildContext context) =>
            new SearchPipeline(projectCode: pipelineArgs.projectCode);
        break;
      case BuildHistory.routePath:
        final BuildHistoryArgument buidHistoryArgs = settings.arguments;
        builder = (BuildContext context) => BuildHistory(
              projectId: buidHistoryArgs.projectId,
              pipelineId: buidHistoryArgs.pipelineId,
              pipelineName: buidHistoryArgs.pipelineName,
              canTrigger: buidHistoryArgs.canTrigger,
            );
        break;
      case CreateExp.routePath:
        final CreateExpArgument createExpArgs = settings.arguments;
        builder = (BuildContext context) => CreateExp(
            projectId: createExpArgs.projectId,
            artifact: createExpArgs.artifact);
        break;
      case MoreApps.routePath:
        final MoreAppsArgument moreAppsArgs = settings.arguments;
        builder = (BuildContext context) => MoreApps(
              title: moreAppsArgs.title,
              type: moreAppsArgs.type,
            );
        break;
      case DetailScreen.routePath:
        final DetailScreenArgument detailArgs = settings.arguments;
        builder = (BuildContext context) => DetailScreen(
            expId: detailArgs.expId,
            fromHistory: detailArgs.fromHistory ?? false);
        break;
      case ExpHistory.routePath:
        final ExpHistoryArgument historyArgs = settings.arguments;
        builder = (BuildContext context) => ExpHistory(
              expArgs: historyArgs,
            );
        break;
      case ExpQRCode.routePath:
        final ShareArgs expQRArgs = settings.arguments;
        builder = (BuildContext context) => ExpQRCode(args: expQRArgs);
        break;
      case QRScanScreen.routePath:
        builder = (BuildContext context) => QRScanScreen();
        break;
      case InstallationManagement.routePath:
        final hasPendingUpgradePkg = settings.arguments;
        builder = (BuildContext context) => InstallationManagement(
              hasPendingUpgradePkg: hasPendingUpgradePkg ?? false,
            );
        break;
      case ShareQRCode.routePath:
        builder = (BuildContext context) => ShareQRCode();
        break;
      case FeedbackScreen.routePath:
        builder = (BuildContext context) => FeedbackScreen();
        break;
      case AppChangelog.routePath:
        builder = (BuildContext context) => AppChangelog();
        break;
      case ExecuteDetailPage.routePath:
        final ExecuteDetailPageArgs args = settings.arguments;
        builder = (BuildContext context) => ExecuteDetailPage(args: args);
        break;
      case ArtifactoryDetail.routePath:
        final ArtifactoryDetailArgs args = settings.arguments;
        builder = (BuildContext context) => ArtifactoryDetail(args: args);
        break;
      case DownloadRecords.routePath:
        builder = (BuildContext context) => DownloadRecords();
        break;
      case SettingPage.routePath:
        builder = (BuildContext context) => SettingPage();
        break;

      default:
        throw new Exception('Invalid route: ${settings.name}');
    }

    return new MaterialPageRoute(builder: builder, settings: settings);
  }
}
