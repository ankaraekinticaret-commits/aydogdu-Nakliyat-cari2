package com.aydogdu.cari;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.*;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView web;
    private ProgressBar progress;
    private DrawerLayout drawer;
    private LinearLayout drawerMenu;
    private LinearLayout bottomNav;
    private TextView baslikYazi;
    private TextView kullaniciAdiYazi;
    private TextView kullaniciRolYazi;
    private String aktifSekme = "panel";

    // Oturum zaman aşımı: arka planda bu süreden uzun kalırsa otomatik çıkış
    private static final long OTURUM_ZAMAN_ASIMI_MS = 10 * 60 * 1000; // 10 dakika
    private long arkaplanaGecisZamani = 0;

    // Sekme adı -> { ikon, etiket, gec() parametresi }
    private final String[][] TUM_SEKMELER = {
        {"panel",    "🏠", "Panel"},
        {"fis",      "📄", "Fişler"},
        {"cari",     "💰", "Cari"},
        {"musteri",  "👥", "Müşteriler"},
        {"sofor",    "🚛", "Şoförler"},
        {"rapor",    "📊", "Raporlar"},
        {"kullanici","👤", "Kullanıcılar"},
        {"ayar",     "⚙️", "Ayarlar"},
        {"yedek",    "💾", "Yedek"},
        {"senkron",  "🔄", "Senkron"},
    };
    // Alt navigasyonda gösterilecek 4 ana sekme + "Daha Fazla"
    private final String[][] ALT_SEKMELER = {
        {"panel",   "🏠", "Panel"},
        {"fis",     "📄", "Fişler"},
        {"cari",    "💰", "Cari"},
        {"musteri", "👥", "Müşteri"},
    };

    @SuppressLint({"SetJavaScriptEnabled","AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.parseColor("#0d3c75"));
        }

        int mavi = Color.parseColor("#1a56a0");
        int maviKoyu = Color.parseColor("#0d3c75");
        int beyaz = Color.WHITE;
        int aktif = Color.parseColor("#ffffff");
        int pasif = Color.parseColor("#9fc1e8");

        // ====== KÖK: DrawerLayout ======
        drawer = new DrawerLayout(this);

        // ---- İçerik sütunu (üst bar + webview + alt nav) ----
        LinearLayout icerikKolon = new LinearLayout(this);
        icerikKolon.setOrientation(LinearLayout.VERTICAL);

        // Üst bar (native)
        LinearLayout ustBar = new LinearLayout(this);
        ustBar.setOrientation(LinearLayout.HORIZONTAL);
        ustBar.setGravity(Gravity.CENTER_VERTICAL);
        ustBar.setBackgroundColor(maviKoyu);
        ustBar.setPadding(dp(16), dp(14), dp(16), dp(14));

        ImageView logoIkon = new ImageView(this);
        logoIkon.setImageResource(R.mipmap.ic_launcher);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        logoLp.rightMargin = dp(10);
        ustBar.addView(logoIkon, logoLp);

        baslikYazi = new TextView(this);
        baslikYazi.setText("Panel");
        baslikYazi.setTextColor(beyaz);
        baslikYazi.setTextSize(18);
        baslikYazi.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams baslikLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ustBar.addView(baslikYazi, baslikLp);

        Button menuBtn = new Button(this);
        menuBtn.setText("☰");
        menuBtn.setTextColor(beyaz);
        menuBtn.setTextSize(20);
        menuBtn.setBackgroundColor(Color.TRANSPARENT);
        menuBtn.setPadding(dp(8), 0, dp(8), 0);
        menuBtn.setOnClickListener(v -> drawer.openDrawer(Gravity.END));
        ustBar.addView(menuBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        icerikKolon.addView(ustBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        // İlerleme çubuğu
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        icerikKolon.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        // WebView
        web = new WebView(this);
        icerikKolon.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Alt navigasyon (native)
        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setBackgroundColor(beyaz);
        bottomNav.setElevation(dp(8));
        bottomNav.setPadding(0, dp(4), 0, dp(4));
        for (String[] sek : ALT_SEKMELER) addAltSekme(bottomNav, sek[0], sek[1], sek[2]);
        addAltSekmeMenu(bottomNav);
        icerikKolon.addView(bottomNav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        drawer.addView(icerikKolon, new DrawerLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ---- Sağdan açılan drawer menü ----
        ScrollView drawerScroll = new ScrollView(this);
        drawerScroll.setBackgroundColor(maviKoyu);
        LinearLayout drawerKok = new LinearLayout(this);
        drawerKok.setOrientation(LinearLayout.VERTICAL);

        // Kullanıcı bilgi kartı (üstte)
        LinearLayout kullaniciKart = new LinearLayout(this);
        kullaniciKart.setOrientation(LinearLayout.VERTICAL);
        kullaniciKart.setBackgroundColor(Color.parseColor("#0a2d57"));
        kullaniciKart.setPadding(dp(20), dp(28), dp(20), dp(18));

        TextView kullaniciIkon = new TextView(this);
        kullaniciIkon.setText("👤");
        kullaniciIkon.setTextSize(32);
        kullaniciKart.addView(kullaniciIkon);

        kullaniciAdiYazi = new TextView(this);
        kullaniciAdiYazi.setText("—");
        kullaniciAdiYazi.setTextColor(beyaz);
        kullaniciAdiYazi.setTextSize(17);
        kullaniciAdiYazi.setTypeface(null, android.graphics.Typeface.BOLD);
        kullaniciAdiYazi.setPadding(0, dp(8), 0, dp(2));
        kullaniciKart.addView(kullaniciAdiYazi);

        kullaniciRolYazi = new TextView(this);
        kullaniciRolYazi.setText("Giriş bekleniyor...");
        kullaniciRolYazi.setTextColor(pasif);
        kullaniciRolYazi.setTextSize(13);
        kullaniciKart.addView(kullaniciRolYazi);

        drawerKok.addView(kullaniciKart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        drawerMenu = new LinearLayout(this);
        drawerMenu.setOrientation(LinearLayout.VERTICAL);
        drawerMenu.setPadding(0, dp(14), 0, dp(8));

        TextView drawerBaslik = new TextView(this);
        drawerBaslik.setText("MENÜ");
        drawerBaslik.setTextColor(pasif);
        drawerBaslik.setTextSize(12);
        drawerBaslik.setTypeface(null, android.graphics.Typeface.BOLD);
        drawerBaslik.setPadding(dp(20), dp(4), dp(20), dp(10));
        drawerMenu.addView(drawerBaslik);

        for (String[] sek : TUM_SEKMELER) addDrawerSekme(sek[0], sek[1], sek[2]);

        drawerKok.addView(drawerMenu);

        // Çıkış butonu (en altta)
        LinearLayout cikisKutu = new LinearLayout(this);
        cikisKutu.setOrientation(LinearLayout.HORIZONTAL);
        cikisKutu.setGravity(Gravity.CENTER_VERTICAL);
        cikisKutu.setPadding(dp(20), dp(16), dp(20), dp(16));
        cikisKutu.setClickable(true);
        cikisKutu.setBackgroundResource(android.R.drawable.list_selector_background);

        TextView cikisIkon = new TextView(this);
        cikisIkon.setText("🚪");
        cikisIkon.setTextSize(18);
        LinearLayout.LayoutParams cikisIkonLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cikisIkonLp.rightMargin = dp(14);
        cikisKutu.addView(cikisIkon, cikisIkonLp);

        TextView cikisYaziV = new TextView(this);
        cikisYaziV.setText("Çıkış Yap");
        cikisYaziV.setTextColor(Color.parseColor("#ff8a80"));
        cikisYaziV.setTextSize(15.5f);
        cikisYaziV.setTypeface(null, android.graphics.Typeface.BOLD);
        cikisKutu.addView(cikisYaziV);

        cikisKutu.setOnClickListener(v -> {
            drawer.closeDrawer(Gravity.END);
            if (web != null) web.evaluateJavascript("try{ cikisYap(); }catch(e){}", null);
        });
        drawerKok.addView(cikisKutu, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        drawerScroll.addView(drawerKok);
        DrawerLayout.LayoutParams drawerLp = new DrawerLayout.LayoutParams(
            dp(270), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END);
        drawer.addView(drawerScroll, drawerLp);

        // Drawer her açıldığında kullanıcı bilgisini sayfadan oku
        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override public void onDrawerOpened(View v) { kullaniciBilgisiOku(); }
        });

        setContentView(drawer);

        // ====== WebView Ayarları ======
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setUseWideViewPort(false);
        s.setLoadWithOverviewMode(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) {
                progress.setProgress(p);
                progress.setVisibility(p == 100 ? View.GONE : View.VISIBLE);
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) {
                progress.setVisibility(View.GONE);
                // Sitenin kendi üst menüsünü ve alt tab bar'ını gizle — native olanları kullanıyoruz
                String gizleCSS =
                    "(function(){" +
                    "var st=document.createElement('style');" +
                    "st.innerHTML='.ustbar{display:none!important}.tab-bar{display:none!important}" +
                    "main{padding-top:14px!important}body{padding-bottom:0!important}" +
                    "#giris .ustbar{display:none!important}';" +
                    "document.head.appendChild(st);" +
                    "window.print=function(){try{AndroidBridge.yazdir();}catch(e){}};" +
                    "})();";
                v.evaluateJavascript(gizleCSS, null);
                kullaniciBilgisiOku();
                kullaniciTakibiBaslat();
            }

            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                String u = req.getUrl().toString();
                if (u.startsWith("tel:") || u.startsWith("mailto:") ||
                    u.startsWith("whatsapp:") || u.contains("wa.me")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); }
                    catch (Exception ignored) {}
                    return true;
                }
                return false;
            }
        });

        web.setDownloadListener((url, ua, cd, mt, cl) -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
            catch (Exception ignored) {}
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(getString(R.string.app_url));
    }

    // ---- Native alt navigasyon öğesi ----
    private void addAltSekme(LinearLayout parent, String sekme, String ikon, String etiket) {
        LinearLayout kutu = new LinearLayout(this);
        kutu.setOrientation(LinearLayout.VERTICAL);
        kutu.setGravity(Gravity.CENTER);
        kutu.setTag(sekme);
        kutu.setClickable(true);

        TextView ikonV = new TextView(this);
        ikonV.setText(ikon);
        ikonV.setTextSize(20);
        ikonV.setGravity(Gravity.CENTER);
        kutu.addView(ikonV);

        TextView etiketV = new TextView(this);
        etiketV.setText(etiket);
        etiketV.setTextSize(10.5f);
        etiketV.setGravity(Gravity.CENTER);
        etiketV.setPadding(0, dp(2), 0, 0);
        kutu.addView(etiketV);

        kutu.setOnClickListener(v -> sekmeyeGit(sekme));
        parent.addView(kutu, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sekmeBoyaAyarla(kutu, ikonV, etiketV, sekme.equals(aktifSekme));
    }

    private void addAltSekmeMenu(LinearLayout parent) {
        LinearLayout kutu = new LinearLayout(this);
        kutu.setOrientation(LinearLayout.VERTICAL);
        kutu.setGravity(Gravity.CENTER);

        TextView ikonV = new TextView(this);
        ikonV.setText("☰");
        ikonV.setTextSize(20);
        ikonV.setGravity(Gravity.CENTER);
        ikonV.setTextColor(Color.parseColor("#50575e"));
        kutu.addView(ikonV);

        TextView etiketV = new TextView(this);
        etiketV.setText("Menü");
        etiketV.setTextSize(10.5f);
        etiketV.setGravity(Gravity.CENTER);
        etiketV.setTextColor(Color.parseColor("#50575e"));
        etiketV.setPadding(0, dp(2), 0, 0);
        kutu.addView(etiketV);

        kutu.setOnClickListener(v -> drawer.openDrawer(Gravity.END));
        parent.addView(kutu, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    private void sekmeBoyaAyarla(LinearLayout kutu, TextView ikonV, TextView etiketV, boolean aktifMi) {
        int renk = aktifMi ? Color.parseColor("#1a56a0") : Color.parseColor("#8a95a1");
        ikonV.setTextColor(renk);
        etiketV.setTextColor(renk);
    }

    // ---- Drawer (sağ menü) öğesi ----
    private void addDrawerSekme(String sekme, String ikon, String etiket) {
        LinearLayout satir = new LinearLayout(this);
        satir.setOrientation(LinearLayout.HORIZONTAL);
        satir.setGravity(Gravity.CENTER_VERTICAL);
        satir.setPadding(dp(20), dp(15), dp(20), dp(15));
        satir.setClickable(true);
        satir.setBackgroundResource(android.R.drawable.list_selector_background);

        TextView ikonV = new TextView(this);
        ikonV.setText(ikon);
        ikonV.setTextSize(18);
        LinearLayout.LayoutParams ikonLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ikonLp.rightMargin = dp(14);
        satir.addView(ikonV, ikonLp);

        TextView etiketV = new TextView(this);
        etiketV.setText(etiket);
        etiketV.setTextColor(Color.parseColor("#dce8f7"));
        etiketV.setTextSize(15.5f);
        etiketV.setTypeface(null, android.graphics.Typeface.BOLD);
        satir.addView(etiketV);

        satir.setOnClickListener(v -> {
            sekmeyeGit(sekme);
            drawer.closeDrawer(Gravity.END);
        });
        drawerMenu.addView(satir, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    // ---- Sayfa içindeki gec() fonksiyonunu native'den tetikle ----
    private void sekmeyeGit(String sekme) {
        aktifSekme = sekme;
        if (web != null) {
            web.evaluateJavascript("try{ gec('" + sekme + "'); }catch(e){}", null);
        }
        for (String[] s : TUM_SEKMELER) {
            if (s[0].equals(sekme)) { baslikYazi.setText(s[2]); break; }
        }
        // Alt navigasyon vurgusunu güncelle
        for (int i = 0; i < bottomNav.getChildCount(); i++) {
            View ch = bottomNav.getChildAt(i);
            if (ch instanceof LinearLayout && ch.getTag() != null) {
                LinearLayout kutu = (LinearLayout) ch;
                TextView ikonV = (TextView) kutu.getChildAt(0);
                TextView etiketV = (TextView) kutu.getChildAt(1);
                sekmeBoyaAyarla(kutu, ikonV, etiketV, kutu.getTag().equals(sekme));
            }
        }
    }

    private final Handler takipHandler = new Handler(Looper.getMainLooper());
    private final Runnable takipDongusu = new Runnable() {
        @Override public void run() {
            kullaniciBilgisiOku();
            takipHandler.postDelayed(this, 4000); // 4 saniyede bir kontrol (giriş/çıkış anında yansısın)
        }
    };
    private void kullaniciTakibiBaslat() {
        takipHandler.removeCallbacks(takipDongusu);
        takipHandler.postDelayed(takipDongusu, 1000);
    }

    private void kullaniciBilgisiOku() {
        if (web == null) return;
        String js =
            "(function(){try{" +
            "return JSON.stringify({k: (window.aktifKullanici||''), r: (window.aktifRol||'')});" +
            "}catch(e){return '{}';}})();";
        web.evaluateJavascript(js, sonuc -> {
            try {
                if (sonuc == null || sonuc.equals("null")) return;
                // evaluateJavascript JSON-escaped string döndürür, dış tırnakları temizle
                String temiz = sonuc;
                if (temiz.startsWith("\"") && temiz.endsWith("\"")) {
                    temiz = temiz.substring(1, temiz.length() - 1).replace("\\\"", "\"");
                }
                String kad = ayikla(temiz, "k");
                String rol = ayikla(temiz, "r");
                runOnUiThread(() -> {
                    if (kad != null && !kad.isEmpty()) {
                        kullaniciAdiYazi.setText(kad);
                        kullaniciRolYazi.setText("yonetici".equals(rol) ? "Yönetici" : "Kullanıcı");
                    } else {
                        kullaniciAdiYazi.setText("—");
                        kullaniciRolYazi.setText("Giriş yapılmadı");
                    }
                });
            } catch (Exception ignored) {}
        });
    }

    // Basit JSON alan ayıklayıcı: {"k":"cevo","r":"yonetici"} -> "cevo"
    private String ayikla(String json, String alan) {
        try {
            String anahtar = "\"" + alan + "\":\"";
            int i = json.indexOf(anahtar);
            if (i < 0) return "";
            int bas = i + anahtar.length();
            int son = json.indexOf("\"", bas);
            if (son < 0) return "";
            return json.substring(bas, son);
        } catch (Exception e) { return ""; }
    }

    private int dp(int deger) {
        float yogunluk = getResources().getDisplayMetrics().density;
        return Math.round(deger * yogunluk);
    }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); if (web != null) web.saveState(out); }

    @Override public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen(Gravity.END)) { drawer.closeDrawer(Gravity.END); return; }
        if (web != null && web.canGoBack()) { web.goBack(); }
        else { super.onBackPressed(); }
    }

    @Override protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
        arkaplanaGecisZamani = System.currentTimeMillis();
        takipHandler.removeCallbacks(takipDongusu);
    }

    @Override protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
        // Arka planda zaman aşımı süresinden uzun kaldıysa otomatik çıkış yap
        if (arkaplanaGecisZamani > 0) {
            long gecenSure = System.currentTimeMillis() - arkaplanaGecisZamani;
            if (gecenSure >= OTURUM_ZAMAN_ASIMI_MS && web != null) {
                web.evaluateJavascript("try{ if(typeof cikisYap==='function') cikisYap(); }catch(e){}", null);
            }
        }
        kullaniciTakibiBaslat();
    }

    @Override protected void onDestroy() {
        takipHandler.removeCallbacks(takipDongusu);
        if (web != null) { web.stopLoading(); web.destroy(); }
        super.onDestroy();
    }

    private class Bridge {
        @JavascriptInterface public void yazdir() {
            runOnUiThread(() -> {
                try {
                    PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                    pm.print("Aydoğdu Fiş", web.createPrintDocumentAdapter("Fiş"),
                        new PrintAttributes.Builder().build());
                } catch (Exception ignored) {}
            });
        }
    }
}
