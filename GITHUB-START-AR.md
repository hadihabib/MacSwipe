# MacSwipe | إنشاء APK عن طريق GitHub فقط

هذه النسخة جاهزة للرفع على GitHub وتشغيل GitHub Actions لإنشاء ملف APK دون تثبيت Android Studio.

## 1. رفع المشروع

1. افتح https://github.com/new وأنشئ Repository باسم `MacSwipe`، ويفضل Private للتجربة.
2. فك ضغط الملف `MacSwipe-v0.2-GitHub.zip` على الماك.
3. ادخل إلى مجلد `MacSwipe-GitHub-v0.2` بعد فك الضغط.
4. افتح المستودع في GitHub، ثم **Add file > Upload files**.
5. اسحب **محتويات المجلد** إلى صفحة الرفع، وليس ملف ZIP نفسه. تأكد أن المجلد `.github` مرفوع كذلك؛ يمكن إظهاره في Finder عبر `Command + Shift + .`.
6. بعد الرفع، تأكد من وجود المسارين على المستوى الأول في المستودع:
   - `.github/workflows/build-android-apk.yml`
   - `android/app/build.gradle.kts`
7. اختر **Commit changes**.

**تنبيه:** رفع ملف ZIP كما هو إلى GitHub لا يفك الضغط ولا ينتج APK. يجب رفع محتوياته.

## 2. البناء والتنزيل

1. افتح تبويب **Actions** في GitHub.
2. افتح **Build MacSwipe Android APK**.
3. اختر **Run workflow** ثم **Run workflow** مرة ثانية.
4. افتح التشغيل الذي يظهر باللون الأخضر عند نجاح البناء.
5. من قسم **Artifacts** نزّل **MacSwipe-v0.2-debug-APK**.
6. فك ضغط Artifact على الموبايل أو الماك وستجد `app-debug.apk`.
7. انقل APK إلى هاتف الأندرويد وافتحه للتثبيت. قد يطلب السماح بتثبيت التطبيقات من هذا المصدر.

## 3. تفعيل الماك

1. من المشروع شغّل `mac/start.command` على الماك. يتطلب أدوات Apple Command Line Tools (`xcode-select --install`) أول مرة إذا لم تكن مثبّتة.
2. امنح **Terminal** صلاحية **Accessibility** من `System Settings > Privacy & Security > Accessibility`.
3. قد تحتاج لإيقاف البرنامج وإعادة تشغيله بعد منح الصلاحية.
4. سيعرض الطرفية عنوان IP ورمز اقتران. افتح MacSwipe على الأندرويد وأدخلهما.
5. استخدم شبكة Wi-Fi موثوقة مشتركة للجهازين. لا تفتح المنفذ 8765 على الإنترنت.

## ملاحظات

- البرنامج على الماك مستلم مستقل؛ لا يتطلب Android Studio.
- `app-debug.apk` نسخة **Debug** مناسبة للاختبار الشخصي، وليست إصداراً موقعاً للنشر على Google Play.
- GitHub Actions يتطلب السماح بتشغيل Actions في إعدادات المستودع.
- الكود الحالي نسخة أولية؛ لم يُختبر هنا بالبناء على GitHub أو بتشغيله فعلياً على هاتف وماك.
- GitHub Actions يبني من الملفات في جذر المستودع؛ لا تتركها داخل مجلد إضافي.
- ملفات الاقتران السرية التي ينشئها الماك مستثناة من Git بواسطة `.gitignore`.
