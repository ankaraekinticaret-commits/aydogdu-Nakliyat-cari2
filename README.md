# Aydoğdu Cari — Android Uygulaması

Telefona kurulabilir gerçek Android APK. Siteyle otomatik çift yönlü senkron çalışır.

## Kurulum Adımları

### 1. URL ve Anahtarı Ayarla
`app/src/main/res/values/strings.xml` dosyasında:
```
https://aydogdunakliyat.com/?aydogdu_cari=mobil#key=API_ANAHTARINIZ
```
API anahtarı: **wp-admin → Cari & Fiş → Senkron & Mobil**

### 2. APK Yap (GitHub Actions — Ücretsiz)
1. Bu klasörü GitHub'a yükle (yeni repo)
2. **Actions** sekmesi → **APK Derle** → **Run workflow**
3. İş bitince **Artifacts** → `AydogduCari-APK` indir
4. İçindeki `app-debug.apk` dosyasını telefona kur

### 3. Telefona Kur
- `.apk` dosyasını telefona gönder (WhatsApp/e-posta/USB)
- Dosyaya dokun → Kur
- "Bilinmeyen kaynak" uyarısı çıkarsa → İzin ver

## Özellikler
- Tam ekran uygulama, status bar lacivert
- Her 2 saniyede otomatik senkron
- Fiş yazdırma (telefon yazıcısına)
- WhatsApp / telefon linkleri native açılır
- Çevrimdışı çalışır (önceki veri görünür)
- Geri tuşu çalışır
