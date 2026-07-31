# Tenis Turnuva Yönetimi (DeFi Ligi benzeri)

Kapsam: tenis kulüpleri için turnuva yönetimi (Faz 1) + defi/meydan okuma
ligi (Faz 2) + saha rezervasyonu (Faz 3). Backend ortak; web (Next.js,
henüz iskelet) ve mobil (Android Studio, Kotlin + Jetpack Compose) aynı
API'yi kullanır. **Tam kullanıcı sistemi var**: e-posta/şifre ile giriş,
iki rol (Yönetici / Oyuncu), JWT tabanlı yetkilendirme.

## Klasör Yapısı

```
tenis-turnuva/
├── apps/
│   ├── api/        Backend — Express + Prisma + PostgreSQL + JWT auth
│   └── web/         Next.js web uygulaması (henüz boş)
├── android/          Kotlin + Jetpack Compose uygulaması
│   └── app/src/main/java/com/tenisturnuva/app/
│       ├── data/session/    Oturum yönetimi (giriş bilgisini saklar)
│       ├── data/            Modeller, Retrofit servisi, repository
│       ├── navigation/      NavGraph — splash → login/register → ana akış
│       └── ui/screens/      Login, Register, Kulüpler, Turnuvalar, Kurulum, Bracket
└── packages/
    └── core/         Bracket motoru (framework'ten bağımsız)
```

## Rol Sistemi

- **Yönetici (ADMIN):** kulüp oluşturur (otomatik sahibi olur), turnuva
  açar, katılımcı ekler, bracket üretir, maç sonucu girer. Sadece **kendi**
  kulübü üzerinde işlem yapabilir (backend'de sahiplik kontrolü var).
- **Oyuncu (PLAYER):** tüm kulüpleri ve turnuvaları görüntüler, bracket'ı
  izler. Ekleme/düzenleme/sonuç girme butonları ona görünmez.

Üstteki bar her ekranda **hangi rolle giriş yaptığını** (rozet) gösterir —
"admin mi kullanıcı mı belli değil" sorunu buradan çözüldü.

## ÖNEMLİ — Bu Güncelleme Sonrası Yapman Gerekenler

Veritabanı şemasına `User` tablosu eklendi ve `Club` artık bir sahibe
(`ownerId`) bağlı — bu değişiklik, daha önce oluşturduğun test verileriyle
(örn. "Esrarengiz" kulübü) uyumsuz. En temiz çözüm, geliştirme
veritabanını sıfırlamak (sadece test verisi kaybolur):

```bash
cd apps/api
npx prisma migrate reset
```

Onay isterse `y` yaz. Bu, veritabanını temizleyip tüm migration'ları
(User/Club/Player/Tournament/Match dahil) baştan uygular.

`.env` dosyana ayrıca şunu ekle (JWT imzalamak için):
```
JWT_SECRET="bunu-uzun-rastgele-bir-metinle-degistir"
```

Sonra bağımlılıkları güncelle (yeni paketler eklendi: bcryptjs,
jsonwebtoken):
```bash
pnpm install
```

Backend'i başlat:
```bash
pnpm dev
```

## Android Tarafında Değişenler

- Yeni ekranlar: **Giriş** ve **Kayıt Ol** (uygulama artık bunlarla açılıyor)
- Oturum bilgisi cihazda saklanıyor (DataStore) — uygulamayı kapatıp
  açtığında tekrar giriş yapman gerekmez
- Her ekranda üstte rol rozeti + çıkış yap butonu
- Yeni renk teması: canlı yeşil (kort) + turuncu (enerji vurgusu), büyük
  yuvarlak butonlar, ikonlu kartlı listeler
- `app/build.gradle.kts`'e yeni bağımlılıklar eklendi (DataStore, Material
  ikonlar, Gson) — Android Studio'da tekrar **Sync Project with Gradle
  Files** yapman gerekecek (internet ister)

### Test için ilk hesabı oluşturma

Uygulamayı açtığında **Kayıt Ol**'a bas, "Yönetici" rolünü seç, bir kulüp
oluştur. Oyuncu tarafını denemek istersen ikinci bir hesabı "Oyuncu"
rolüyle kaydet (farklı bir e-posta ile) — o hesapla giriş yapınca sadece
görüntüleme yapabildiğini göreceksin.

## Notlar

- Bracket motoru (`packages/core`) framework'ten bağımsız.
- `Match.player1Id` / `player2Id` alanları Participant.id tutar (Player.id
  değil).
- Backend'de yetkilendirme: `requireAuth` (geçerli JWT şart) ve
  `requireAdmin` (rol ADMIN olmalı) middleware'leri + turnuva/kulüp
  sahiplik kontrolü (`club.ownerId === req.user.userId`).
- Bu bir MVP kimlik doğrulama sistemi — şifre sıfırlama, e-posta
  doğrulama, refresh token gibi production-seviyesi özellikler kapsam
  dışı bırakıldı. İstersen sıradaki adım olarak ekleyebiliriz.
- Gerçek bir uygulama ikonu yok — Android Studio'da **File → New → Image
  Asset** ile eklenebilir.
