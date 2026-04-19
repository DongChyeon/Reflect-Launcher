# 리소스 생명주기 규칙

> Android 리소스는 사용 후 반드시 해제한다. 해제하지 않으면 메모리 누수 및 배터리 낭비로 이어진다.

## BroadcastReceiver

- [필수] `registerReceiver()`로 동적 등록한 Receiver는 반드시 `unregisterReceiver()`로 해제한다.
- [필수] 해제 시점: 등록한 생명주기의 대응 메서드 (`onStart`↔`onStop`, `onCreate`↔`onDestroy`)

```kotlin
// DO
override fun onStart() {
    super.onStart()
    registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_ON))
}

override fun onStop() {
    super.onStop()
    unregisterReceiver(receiver)
}

// DON'T — unregisterReceiver() 미호출 → 메모리 누수 및 중복 이벤트 수신
override fun onStart() {
    super.onStart()
    registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_ON))
    // onStop()에 unregisterReceiver() 없음
}
```

## Coroutine / Flow

- [필수] `CoroutineScope`를 직접 생성한 경우 더 이상 필요하지 않을 때 `cancel()`을 호출한다.
- UI 레이어의 Flow 수집은 `repeatOnLifecycle`로 생명주기에 맞게 자동 관리한다. (`state-management-rules.md` 참고)

## 해제 시점 요약

| 리소스 | 등록 시점 | 해제 시점 |
|---|---|---|
| `BroadcastReceiver` | `onStart()` | `onStop()` |
| 커스텀 `CoroutineScope` | 생성 시점 | 더 이상 불필요할 때 |
