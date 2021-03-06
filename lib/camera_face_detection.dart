import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:rxdart/rxdart.dart';

class CameraFaceDetection {
  static const MethodChannel _channel =
      const MethodChannel('camera_face_detection');

  static const EventChannel _eventChannel =
      const EventChannel("camera_face_detection_stream");

  StreamController<List<DetectedFace>> _streamController;

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  Future<Stream<List<DetectedFace>>> startDetection({bool detectGender = true, bool detectAgeRange = true}) async {
    final result = await _channel.invokeMethod("startDetection", {
      "detectGender": detectGender,
      "detectAgeRange": detectAgeRange,
    });

    if (result) {
      _streamController = _streamController ??= StreamController();

      _eventChannel
          .receiveBroadcastStream()
          .throttleTime(const Duration(seconds: 1))
          .listen((event) {
        if (event is List) {
          _streamController
              .add(event.map((e) => DetectedFace.fromMap(e)).toList());
        }
      });

      return _streamController.stream;
    }
    return null;
  }

  Future<void> stopDetection() async {
    await _channel.invokeMethod("stopDetection");
  }

  Future<bool> hasPermissions() {
    return _channel.invokeMethod("hasPermissions");
  }

  Future<bool> isDetecting() {
    return _channel.invokeMethod("isDetecting");
  }

  void dispose() async {
    await stopDetection();
    _streamController?.close();
  }
}

class DetectedFace {
  final double smilingProbability;
  final double leftEyeOpenProbability;
  final double rightEyeOpenProbability;
  final double headEulerAngleX;
  final double headEulerAngleY;
  final double headEulerAngleZ;
  final int trackingId;
  final String gender;
  final String ageRange;

  DetectedFace({
    @required this.smilingProbability,
    @required this.leftEyeOpenProbability,
    @required this.rightEyeOpenProbability,
    @required this.headEulerAngleX,
    @required this.headEulerAngleY,
    @required this.headEulerAngleZ,
    @required this.trackingId,
    @required this.gender,
    @required this.ageRange,
  });

  factory DetectedFace.fromMap(Map data) => DetectedFace(
        smilingProbability: data['smilingProbability'],
        leftEyeOpenProbability: data['leftEyeOpenProbability'],
        rightEyeOpenProbability: data['rightEyeOpenProbability'],
        headEulerAngleX: data['headEulerAngleX'],
        headEulerAngleY: data['headEulerAngleY'],
        headEulerAngleZ: data['headEulerAngleZ'],
        trackingId: data['trackingId'],
        gender: data['gender'],
        ageRange: data['ageRange']
      );
}
