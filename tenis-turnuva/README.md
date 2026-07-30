# Tenis Turnuva Yönetimi (DeFi Ligi benzeri)

Kapsam: tenis kulüpleri için turnuva yönetimi (Faz 1) + defi/meydan okuma
ligi (Faz 2) + saha rezervasyonu (Faz 3). Web + mobil, ortak bir backend.

## Klasör Yapısı

```
tenis-turnuva/
├── apps/
│   ├── api/          Backend (Prisma schema burada — NestJS/Express kurulacak)
│   └── web/           Next.js web uygulaması (henüz boş — bir sonraki adım)
├── packages/
│   └── core/           Framework'ten bağımsız iş mantığı (bracket motoru)
│       └── src/bracket.ts   Tek eleme turnuva üretimi + otomatik ilerleme
```

## Şu Ana Kadar Hazır Olan

- **`packages/core/src/bracket.ts`** — tek eleme turnuva bracket motoru:
  - `generateBracket(participants, seedingType)` — bye'ları doğru dağıtarak
    tam bracket üretir, bye'lı oyuncuları otomatik bir sonraki tura ilerletir.
  - `advanceWinner(matches, round, position, winnerId)` — bir maç bitince
    kazananı bir sonraki maça otomatik yerleştirir.
  - `swapParticipants(...)` — manuel bracket düzenleme (sürükle-bırak
    arayüzünün arkasında kullanılacak).
  - 7 kişilik bir turnuva ile duman testi (smoke test) yapıldı, bye ve
    ilerleme mantığı doğrulandı.
- **`apps/api/prisma/schema.prisma`** — veritabanı modeli: Club, Player,
  Tournament, Participant, Match (bracket düğümleri).

## Senin Yapman Gerekenler

1. **Ortam kurulumu (bir kereye mahsus):**
   - [Node.js](https://nodejs.org) (LTS) ve `pnpm` kur: `npm install -g pnpm`
   - Bir PostgreSQL veritabanı edin — en kolayı: [Supabase](https://supabase.com)
     veya [Railway](https://railway.app) üzerinde ücretsiz bir proje açmak
     (ikisi de bootstrap için uygun, kredi kartı gerektirmeden başlanabilir)
   - GitHub'da bu proje için boş bir repo aç, bu klasörü push'la

2. **Hesaplar (ihtiyaç oldukça açılabilir, hemen şart değil):**
   - Vercel (web'i deploy etmek için) — GitHub ile giriş yeterli
   - OneSignal (push bildirimleri için) — Faz 2'de gerekecek

3. **Yerelde çalıştırmak için** (ben burada npm install çalıştıramıyorum,
   ağ erişimim kapalı — bunu sen kendi makinende yapmalısın):
   ```bash
   pnpm install
   cd apps/api
   cp .env.example .env   # DATABASE_URL'i kendi Postgres bağlantı adresinle değiştir
   npx prisma migrate dev --name init
   ```

## Benim Yapacaklarım (senin onayınla, adım adım)

- `apps/api` içine gerçek bir backend iskeleti (Express veya NestJS +
  Prisma client + turnuva CRUD endpoint'leri) kurmak
- `apps/web` içine Next.js uygulamasını kurup ilk ekranı (turnuva oluşturma
  formu + bracket görselleştirme) inşa etmek
- Bracket'ı görsel olarak çizen bir React bileşeni yazmak
- Sen yerelde `pnpm install` çalıştırıp bana sonucu bildirdikçe, üzerine
  koda devam etmek

## Notlar

- Bracket motoru framework'ten bağımsız yazıldı (`packages/core`) — hem web
  hem mobil tarafı aynı mantığı kullanabilsin diye çoğaltma yapılmadı.
- `next_match_id` ilerleme mantığı DB'de bir trigger olarak değil, uygulama
  katmanında (`advanceWinner`) yürütülüyor — bu hem test etmesi hem
  debug etmesi çok daha kolay bir yaklaşım.
