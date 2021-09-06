import 'package:bkci_app/widgets/BkIcons.dart';
import 'package:flutter/material.dart';

class NotFoundPage extends StatelessWidget {
  final String route;

  NotFoundPage(this.route);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          leading: Icon(BkIcons.left),
          title: Text('NotFound'),
        ),
        body: Center(
          child: Text('page $route 404'),
        ));
  }
}
