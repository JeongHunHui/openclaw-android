# 훈희봇 Android App

개인용 Android 앱. 알림바에서 음성/텍스트로 Slack 메시지를 바로 전송합니다.

## 기능
- 🎤 알림바 음성 버튼 → Android STT → Slack 전송
- ✏️ 알림바 텍스트 버튼 → 입력 창 → Slack 전송
- 🔧 앱 실행 → Slack Bot Token / Channel ID 설정

## 빌드
```bash
./gradlew assembleDebug
```
APK 위치: `app/build/outputs/apk/debug/app-debug.apk`

## 설정
앱 실행 후 첫 화면에서:
1. Slack Bot Token 입력 (xoxb-...)
2. Slack Channel ID 입력 (C...)
3. 저장 → 서비스 시작

## 권한
- 마이크 (음성 인식)
- 인터넷 (Slack API)
- 포그라운드 서비스 (알림 유지)
- 알림 (Android 13+)
