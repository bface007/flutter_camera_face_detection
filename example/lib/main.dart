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
  String _gender;
  String _ageRange;
  StreamSubscription _subscription;

  @override
  void initState() {
    super.initState();
    _cameraFaceDetection = CameraFaceDetection();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      initPlatformState();
    });
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  void initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await CameraFaceDetection.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    try {
      final _stream = await _cameraFaceDetection.startDetection(
        // detectGender: false,
        detectAgeRange: false,
      );

      if (_stream != null) {
        _subscription = _stream.listen((faces) {
          print("Faces ${faces.length}");

          if (faces.isNotEmpty) {
            setState(() {
              _smilingProb = faces.first.smilingProbability;
              _gender = faces.first.gender;
              _ageRange = faces.first.ageRange;
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

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  void dispose() {
    _cameraFaceDetection.dispose();
    _subscription?.cancel();
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
              if (_gender != null) Text("Gender: $_gender"),
              if (_ageRange != null) Text("Gender: $_ageRange"),
            ],
          ),
        ),
      ),
    );
  }
}
