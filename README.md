![DexCeed VPN](artwork/dexceed/logo.png)

# DexCeed VPN для Android

Брендированная сборка [qThroneMobile](https://github.com/ttrroyy/qThroneMobile) на основе v3.3.0. Поддерживает импорт персональной ссылки подписки. Android-пакет `online.dexceed.vpn` устанавливается рядом с оригиналом. Обновления берутся из [релизов DexCeed VPN](https://github.com/dexceed/DexCeed-VPN/releases).

[Исходное описание и авторы](README.upstream.md) · [GPLv3](LICENSE). Лицензии компонентов транспорта включены в приложение.

## Сборка на Linux

Для версий 1.0.x используйте JDK 17, Go 1.27.0, Android SDK 35, Build Tools 35.0.0 и Android NDK. В чистой рабочей копии выполните `buildScript/dexceed_mobile_inputs.sh /путь/к/новой/временной/папке`, затем `./gradlew app:assembleOssRelease`. Скрипт проверяет SHA-256 официального универсального APK v3.3.0, берёт из него готовые нативные библиотеки и связанные ресурсы, а Java-привязки генерирует из закреплённых исходников ядра. Kotlin/Java-код приложения компилируется Gradle. При смене upstream-версии обновите и проверьте оба закреплённых значения.

Для подписанного выпуска задайте переменные подписи из buildSrc (SIGNING_KEYSTORE_B64, SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS), UPDATE_SIGNER_SHA256 и номер сборки THRONE_BUILD. Храните ключ вне репозитория. Проверьте APK и сформируйте метаданные встроенного обновления командой `python3 buildScript/release_assets.py app/build/outputs/apk/oss/release dist stable`.
