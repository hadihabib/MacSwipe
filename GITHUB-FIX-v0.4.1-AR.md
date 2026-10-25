# تصحيح خطأ Java في MacSwipe v0.4.1

الخطأ: `TrackpadView.java:72: cancelLongPress() cannot override cancelLongPress() in View`

الإصلاح: تغيير اسم الدالة الخاصة بالتطبيق من `cancelLongPress()` إلى `cancelPendingLongPress()` وتحديث جميع الاستدعاءات في `TrackpadView.java`، لتجنّب التعارض مع الدالة العامة الموجودة في `android.view.View`.

## تحديث ملف واحد مباشرة على GitHub

1. انتقل في المستودع إلى `android/app/src/main/java/com/macswipe/remote`.
2. استبدل محتوى ملف `TrackpadView.java` بنسخة الملف المصحح في المجلد نفسه.
3. اعمل Commit changes ثم شغّل Build MacSwipe Android APK من Actions.

النسخة المصححة لم تُختبر ببناء APK كامل على GitHub.
