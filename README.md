# 훈희봇 Android App

개인용 Android 앱. 알림바/앱에서 음성·텍스트로 Slack 메시지를 바로 전송.

APK 다운로드: https://github.com/JeongHunHUI/openclaw-android/releases/latest

---

## 기능

### 현재 구현
- 🎤 음성 녹음 → Android STT → Slack 전송
  - 연속 녹음 모드: 종료 버튼 누를 때까지 계속 듣기
  - 버튼 상태: 기본="🎤 지금 바로 음성 전송" / 녹음중="👂 듣는 중... (탭하면 중지)"
- ✏️ 알림바 텍스트 버튼 → 입력창 → Slack 전송
- 🔔 포그라운드 서비스 (알림바 상태 표시 + 액션 버튼)
- 🔧 설정 화면: Slack Bot Token / Channel ID 입력

### 알림바 버튼
- 🎤 음성: 음성 인식 시작/연속 녹음
- ✏️ 텍스트: 인라인 텍스트 입력 (RemoteInput)

---

## 구조

```
app/src/main/java/com/hunhui/bot/
  MainActivity.kt            # 메인 화면 (음성 버튼, 설정 버튼)
  BotService.kt              # 포그라운드 서비스 (STT, 알림 관리)
  SetupActivity.kt           # 초기 설정 화면
  TextInputActivity.kt       # 텍스트 입력 팝업
  SlackMessenger.kt          # Slack API 메시지 전송
  NotificationActionReceiver.kt # 알림 액션 처리
  Prefs.kt                   # SharedPreferences 헬퍼
```

---

## 빌드 & 배포

### 자동 빌드
main 브랜치에 push → GitHub Actions 자동 빌드 → Slack #openclaw에 APK 링크 전송

### 수동 빌드
```bash
./gradlew assembleRelease
```
APK 위치: `app/build/outputs/apk/release/`

### GitHub Secrets 필요
| Secret | 설명 |
|--------|------|
| `KEYSTORE_BASE64` | 릴리즈 서명 키스토어 (base64) |
| `KEYSTORE_STORE_PASS` | 키스토어 비밀번호 |
| `KEYSTORE_KEY_ALIAS` | 키 별칭 |
| `KEYSTORE_KEY_PASS` | 키 비밀번호 |
| `SLACK_BOT_TOKEN` | Slack Bot Token (빌드 완료 알림용) |

---

## 설정

앱 실행 후 설정 화면:
1. Slack Bot Token 입력 (`xoxb-...`)
2. Slack Channel ID 입력 (`C...`)
3. 저장 → 서비스 시작

---

## 권한
- `RECORD_AUDIO` (음성 인식)
- `INTERNET` (Slack API)
- `FOREGROUND_SERVICE` (알림 유지)
- `POST_NOTIFICATIONS` (Android 13+)

---

## AI 개발 노트 (OpenClaw)

이 앱 = "훈이 앱". hunhui-playground와 별개.

### 구현 예정 / 아이디어
- [ ] 투두리스트 화면 (Google Sheets 연동)
- [ ] 작업 현황 화면 (대시보드)
- [ ] Slack 메시지 히스토리 보기

### 주의사항
- 연속 녹음 로직: `BotService.kt`의 `onResults`에서 `isListening`이면 재시작
- STT 오류 중 `ERROR_NO_MATCH`, `ERROR_SPEECH_TIMEOUT`은 재시도 가능 (retriable)
- 슬랙 알림 채널: `C0AJLQUM2KA` (#openclaw)
