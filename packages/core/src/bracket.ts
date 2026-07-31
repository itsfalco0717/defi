/**
 * Tek eleme (single elimination) turnuva bracket motoru.
 *
 * Sorumluluklari:
 *  1. Katilimci listesinden ve seed sirasindan bir bracket (round + position
 *     izgarasi) uretmek - bye'lari dogru yerlere dagitarak.
 *  2. Bir macin kazananini bir sonraki tura otomatik ilerletmek.
 *
 * Bu dosya framework/DB'den bagimsizdir (saf TypeScript) - hem apps/api
 * (Prisma ile kalicilastirma) hem apps/web / mobil (onizleme, test) tarafindan
 * kullanilabilir.
 */

export type SeedingType = "random" | "ranked" | "manual";

export interface ParticipantInput {
  id: string; // Participant.id (playerId degil - ayni oyuncu farkli turnuvalarda farkli katilimci olabilir)
  playerName: string;
  rating?: number; // ranked seeding icin
  seed?: number; // manual seeding icin (1 = en guclu)
}

export interface BracketMatch {
  round: number; // 1 = ilk tur
  position: number; // o turdaki sira (0-indexli)
  player1Id: string | null; // null = BYE ya da henuz belli degil
  player2Id: string | null;
  winnerId: string | null;
  status: "PENDING" | "SCHEDULED" | "COMPLETED";
  nextMatchRound: number | null; // kazananin gidecegi maç: round
  nextMatchPosition: number | null; // ve o rounddaki position
}

export interface GeneratedBracket {
  drawSize: number;
  totalRounds: number;
  matches: BracketMatch[];
}

/** drawSize (2'nin kuvveti) icin standart turnuva seed sirasi.
 *  Ornek: size=8 -> [1,8,5,4,3,6,7,2]
 *  Bu siralama sayesinde 1 ve 2 numarali seedler ancak finalde karsilasir. */
function standardSeedOrder(size: number): number[] {
  let seeds = [1];
  while (seeds.length < size) {
    const n = seeds.length * 2;
    const next: number[] = [];
    for (const s of seeds) {
      next.push(s);
      next.push(n + 1 - s);
    }
    seeds = next;
  }
  return seeds;
}

function nextPowerOfTwo(n: number): number {
  let p = 1;
  while (p < n) p *= 2;
  return p;
}

/** Katilimcilari seedingType'a gore siralar (1. index = en guclu / 1 numarali seed). */
function orderParticipants(
  participants: ParticipantInput[],
  seedingType: SeedingType
): ParticipantInput[] {
  if (seedingType === "manual") {
    return [...participants].sort((a, b) => (a.seed ?? 999) - (b.seed ?? 999));
  }
  if (seedingType === "ranked") {
    return [...participants].sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0));
  }
  // random
  const arr = [...participants];
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

/**
 * Tam bracket'i uretir: ilk turdan finale kadar tum maclari, bye'lari
 * otomatik ilerletilmis halde (bye alan oyuncu round 1'i COMPLETED olarak
 * gecer ve winnerId direkt atanir).
 */
export function generateBracket(
  participants: ParticipantInput[],
  seedingType: SeedingType = "random"
): GeneratedBracket {
  if (participants.length < 2) {
    throw new Error("Bracket icin en az 2 katilimci gerekir");
  }

  const drawSize = nextPowerOfTwo(participants.length);
  const totalRounds = Math.log2(drawSize);
  const ordered = orderParticipants(participants, seedingType);
  const seedOrder = standardSeedOrder(drawSize); // slot index -> seed number (1-indexli)

  // slot index -> participant id | null (BYE)
  const slots: (string | null)[] = seedOrder.map((seedNumber) =>
    seedNumber <= ordered.length ? ordered[seedNumber - 1].id : null
  );

  const matches: BracketMatch[] = [];

  // Round 1 maclarini olustur
  const round1Count = drawSize / 2;
  for (let i = 0; i < round1Count; i++) {
    const p1 = slots[i * 2];
    const p2 = slots[i * 2 + 1];
    const isBye = p1 === null || p2 === null;
    const winner = isBye ? p1 ?? p2 : null;

    matches.push({
      round: 1,
      position: i,
      player1Id: p1,
      player2Id: p2,
      winnerId: winner,
      status: isBye ? "COMPLETED" : p1 && p2 ? "SCHEDULED" : "PENDING",
      nextMatchRound: totalRounds > 1 ? 2 : null,
      nextMatchPosition: totalRounds > 1 ? Math.floor(i / 2) : null,
    });
  }

  // Sonraki turlarin bos iskeletini olustur
  for (let round = 2; round <= totalRounds; round++) {
    const count = drawSize / Math.pow(2, round);
    for (let pos = 0; pos < count; pos++) {
      matches.push({
        round,
        position: pos,
        player1Id: null,
        player2Id: null,
        winnerId: null,
        status: "PENDING",
        nextMatchRound: round < totalRounds ? round + 1 : null,
        nextMatchPosition: round < totalRounds ? Math.floor(pos / 2) : null,
      });
    }
  }

  // Round 1'deki bye'lari bir sonraki tura otomatik ilerlet (zincirleme bye
  // olabilir - orn. 2 kisilik bir dal ust uste bye alabilir kucuk turnuvalarda)
  for (const m of matches.filter((m) => m.round === 1 && m.status === "COMPLETED")) {
    if (m.winnerId && m.nextMatchRound !== null && m.nextMatchPosition !== null) {
      advanceWinner(matches, m.round, m.position, m.winnerId);
    }
  }

  return { drawSize, totalRounds, matches };
}

/**
 * Bir macin kazananini isaretler ve bracket zincirinde bir sonraki maca
 * otomatik yerlestirir (next_match_id mantigi). `matches` dizisini yerinde
 * (in place) gunceller.
 */
export function advanceWinner(
  matches: BracketMatch[],
  round: number,
  position: number,
  winnerId: string,
  score?: string
): void {
  const match = matches.find((m) => m.round === round && m.position === position);
  if (!match) throw new Error(`Mac bulunamadi: round=${round} position=${position}`);
  if (match.player1Id !== winnerId && match.player2Id !== winnerId) {
    throw new Error("winnerId bu macin oyunculari arasinda degil");
  }

  match.winnerId = winnerId;
  match.status = "COMPLETED";
  if (score) (match as any).score = score;

  if (match.nextMatchRound === null || match.nextMatchPosition === null) return; // final oynandi

  const nextMatch = matches.find(
    (m) => m.round === match.nextMatchRound && m.position === match.nextMatchPosition
  );
  if (!nextMatch) return;

  // Bu mac, bir sonraki mactaki iki slottan hangisini besliyor?
  // position % 2 === 0 -> player1 slotu, aksi halde player2 slotu (standart
  // bracket kuralı: pozisyon çiftleri bir üst turda tek bir maçı besler).
  if (match.position % 2 === 0) {
    nextMatch.player1Id = winnerId;
  } else {
    nextMatch.player2Id = winnerId;
  }

  if (nextMatch.player1Id && nextMatch.player2Id) {
    nextMatch.status = "SCHEDULED";
  }
}

/**
 * Manuel duzenleme: iki katilimcinin bracket'taki (round 1) yerini
 * degistirir. Sadece henuz hic mac oynanmamis (turnuva DRAFT durumundayken)
 * kullanilmalidir.
 */
export function swapParticipants(
  bracket: GeneratedBracket,
  participantIdA: string,
  participantIdB: string
): void {
  const round1 = bracket.matches.filter((m) => m.round === 1);
  for (const m of round1) {
    if (m.player1Id === participantIdA) m.player1Id = participantIdB;
    else if (m.player1Id === participantIdB) m.player1Id = participantIdA;

    if (m.player2Id === participantIdA) m.player2Id = participantIdB;
    else if (m.player2Id === participantIdB) m.player2Id = participantIdA;
  }
}
