# 보안 규칙

## 로그
- [필수] `Timber` / `Log.d` 등 로그에 **토큰, PII(개인식별정보)를 출력하지 않는다.**

```kotlin
// DON'T
Timber.d("사용자 정보: email=$userEmail, deviceId=$deviceId")

// DO — 식별 불가능한 정보만 로깅
Timber.d("앱 사용 데이터 로드 완료: count=${usageList.size}")
```
