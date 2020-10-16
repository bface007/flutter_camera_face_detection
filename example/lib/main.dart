import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:camera_face_detection/camera_face_detection.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  List<Future> configs = [
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.landscapeLeft,
    ]),
  ];

  await Future.wait(configs);

  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  int _detectedFacesCount = 0;
  double _smilingProb = 0;
  CameraFaceDetection _cameraFaceDetection;

  @override
  void initState() {
    super.initState();
    _cameraFaceDetection = CameraFaceDetection();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await CameraFaceDetection.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    try {
      final _stream = await _cameraFaceDetection.startDetection();

      if (_stream != null) {
        _stream.listen((faces) {
          print("Faces ${faces.length}");

          if (faces.isNotEmpty) {
            setState(() {
              _smilingProb = faces.first.smilingProbability;
            });
          }

          setState(() {
            _detectedFacesCount = faces.length;
          });
        });
      } else {
        print("Stream is null");
      }
    } catch (ex) {
      print(ex);
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  void dispose() {
    _cameraFaceDetection.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: [
              Text('Running on: $_platformVersion\n'),
              Text("Detected faces: $_detectedFacesCount"),
              Text("smiling prob: $_smilingProb"),
            ],
          ),
        ),
      ),
    );
  }
}
