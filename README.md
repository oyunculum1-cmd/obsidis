# Obsidis

Ekranın üstünde sabit duran, canlı gözlü bir "ada" üzerinden soru sorup,
CapCut gibi bir uygulamanın gerçek düğmelerini turuncu ışıkla işaret ederek
adım adım rehberlik eden Android asistanı.

## Bu sürümde neler değişti

- **Ada artık sabit ve sürüklenemiyor** — ekranın üstünde, saat ile pil
  arasında bir hap şeklinde duruyor (Dynamic Island mantığı). Sol yarısı
  göz, sağ yarısı klavye ikonu.
- **Göz canlı** — rastgele aralıklarla kendiliğinden göz kırpıyor.
- **Klavyeye basınca** adanın hemen altında bir soru kutusu açılıyor,
  klavye otomatik çıkıyor.
- **Uygulamadan çıkınca ada kaybolmuyor.** Bu bir "foreground service" —
  CapCut'a geçsen, Obsidis'i kapatsan, hatta son uygulamalardan silsen bile
  ada ekranda kalır. Yalnızca ana ekrandaki "Adayı durdur" düğmesine tekrar
  basınca kapanır. Uygulamayı tekrar açtığında düğme, adanın gerçekten açık
  mı kapalı mı olduğunu doğru gösterir.
- **Yeni açılış animasyonu:** karanlıkta bir kıvılcım doğar, sarı bir ışığa
  şişer, ekran beyaza patlar, sonra rozet şeklindeki logo (siyah kare
  içinde beyaz A, çıtası göz) yukarıdan iner. İnince göz açılır, iki kez
  kırpar, sağa sola bakar, sonra ana ekrana geçilir.
- **Ana ekrandaki logo da göz kırpıyor** (durgun halde, ara sıra).

## Dürüst olmam gereken kısım (değişmedi)

- Yapay zeka kısmı için ücretsiz bir servise (önerim: console.groq.com,
  kredi kartsız, günlük cömert kota) kayıt olup bir API anahtarı alman ve
  Ayarlar'a yapıştırman gerekiyor. Hiçbir ücretli bağımlılık kodun içinde
  yok.
- CapCut'ın yazısız/sade ikonlu bazı düğmelerinde erişilebilirlik etiketi
  zayıf olabilir, o adımlar ilk denemede bulunamayabilir — gerçek cihazda
  test edip zamanla iyileştireceğimiz bir kısım.

## GitHub'a yükleme ve APK indirme (adım adım)

Bunu daha önce UniRemote AI ile yaptığın yöntemin aynısı. Özetle:

**1) Yeni bir GitHub deposu aç**
`github.com/new` → isim ver (ör. `obsidis`) → Public → boş bırak → Create.

**2) Termux ile projeyi yükle**
```
cd ~/storage/downloads
unzip Obsidis.zip
cd obsidis
git init
git add .
git commit -m "ilk yukleme"
git branch -M main
git remote add origin https://KULLANICI_ADIN:TOKEN@github.com/KULLANICI_ADIN/obsidis.git
git push -u origin main
```
(`KULLANICI_ADIN` ve `TOKEN` kısımlarını kendi bilgilerinle değiştir — token'ı
daha önce oluşturduğun GitHub ayarlarından tekrar üretebilir ya da eskisini
kullanabilirsin, süresi dolmadıysa.)

**3) Otomatik derleme dosyasını ekle**
GitHub'da şu adrese git:
```
github.com/KULLANICI_ADIN/obsidis/new/main?filename=.github/workflows/build.yml
```
İçine şunu yapıştır:
```yaml
name: Build APK
on: [push, workflow_dispatch]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - name: Accept SDK licenses
        run: yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses || true
      - uses: gradle/actions/setup-gradle@v4
      - name: Build debug APK
        run: gradle assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: obsidis-debug-apk
          path: app/build/outputs/apk/debug/*.apk
```
Commit changes.

**4) Derlemeyi izle, indir**
```
github.com/KULLANICI_ADIN/obsidis/actions
```
Yeşil tik olunca, o çalışmanın içindeki **Artifacts** bölümünden
`obsidis-debug-apk` dosyasını indir, zip'i aç, `.apk`'ya dokun, kur.

## Kurulumdan sonra

1. Uygulamayı aç, açılış animasyonunu izle.
2. "Ekran üstünde gösterme izni" ve "Erişilebilirlik servisi" düğmelerine
   sırayla bas, açılan ayarlarda Obsidis'i etkinleştir.
3. Groq'tan aldığın anahtarı "Yapay zeka anahtarı" kutusuna yapıştır.
4. "Adayı başlat" — ada ekranın üstünde belirir. CapCut'ı aç, klavye
   ikonuna dokun, sormaya başla.
