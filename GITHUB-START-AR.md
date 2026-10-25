# MacSwipe v0.4 — البناء على GitHub بدون Android Studio

هذه نسخة أندرويد مكتوبة بالكامل بلغة Java، بلا إضافة Kotlin، لتفادي أعطال ترجمة Kotlin في التشغيل السابق.

## الأسهل والأقل عرضة للأخطاء — رفع المشروع عبر Terminal مرة واحدة

1. نزّل ملف `MacSwipe-v0.4-GitHub-Java.zip` على الماك، وضعه داخل `~/Downloads`.
2. افتح Terminal والصق الأوامر التالية **دفعة واحدة**:

```bash
cd ~/Downloads
unzip -o MacSwipe-v0.4-GitHub-Java.zip
git clone https://github.com/hadihabib/MacSwipe.git MacSwipe-upload
rsync -av --exclude='.git' MacSwipe-v0.4-GitHub-Java/ MacSwipe-upload/
rm -f MacSwipe-upload/android/app/src/main/java/com/macswipe/remote/*.kt
cd MacSwipe-upload
git add -A
git commit -m "Replace Kotlin Android app with Java-only build"
git push origin main
```

إذا كان مجلد `MacSwipe-upload` موجوداً سابقاً، غيّر اسمه أو احذفه بعد التأكد من عدم وجود تعديلات تحتاجها. إذا كان فرع المستودع الرئيسي ليس `main` فاستبدل `main` باسمه. 

3. افتح GitHub > مستودع `MacSwipe` > `Actions` > `Build MacSwipe Android APK` > `Run workflow`.
4. عند النجاح، انزل لأسفل صفحة التشغيل > `Artifacts` > `MacSwipe-v0.4-debug-APK`.
5. فك ZIP الناتج ونزّل `app-debug.apk` على الموبايل وثبّته.

## بديل من المتصفح فقط

ارفع ملفات ZIP **بعد فك ضغطه** إلى المستودع مع المحافظة على المسارات. عند استبدال ملف باسم موجود استخدم التعديل/الرفع المناسب على GitHub. احذف ملفات `.kt` القديمة من `android/app/src/main/java/com/macswipe/remote/` (إن وُجدت). تأكد من ظهور المسارات:

- `.github/workflows/build-android-apk.yml`
- `android/build.gradle.kts`
- `android/app/build.gradle.kts`
- `android/app/src/main/java/com/macswipe/remote/MainActivity.java`
- `android/app/src/main/java/com/macswipe/remote/RemoteSocket.java`
- `android/app/src/main/java/com/macswipe/remote/TrackpadView.java`

## تشغيل مستقبل التحكم على الماك

شغّل `mac/start.command` بعد منح تطبيق Terminal صلاحية Accessibility من `System Settings > Privacy & Security > Accessibility`. سيعرض البرنامج IP و Token. أدخلهما في تطبيق الأندرويد على شبكة Wi-Fi نفسها.

استخدم شبكة موثوقة فقط، ولا تعمل Port Forward للمنفذ 8765 إلى الإنترنت.

## التحقق

اجتازت ملفات Java اختبار ترجمة صياغي محلي باستخدام بدائل مبسطة لـ Android APIs (`javac 17`). لم يُجرَ بناء Android SDK كاملاً، ولذلك نجاح `app-debug.apk` لم يُؤكَّد بعد على GitHub Actions.
