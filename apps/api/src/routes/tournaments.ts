import { Router } from "express";
import { prisma } from "../prisma";
import { requireAuth, requireAdmin } from "../middleware/auth";
import {
  generateBracket,
  advanceWinner,
  type ParticipantInput,
  type SeedingType,
} from "@tenis-turnuva/core";

export const tournamentsRouter = Router();

/** Bir turnuvayi getirip cagiran kullanicinin (req.user.userId) o turnuvanin
 * bagli oldugu kulubun sahibi olup olmadigini dogrular. Degilse null doner
 * ve caller uygun bir hata yaniti verir. */
async function loadOwnedTournament(tournamentId: string, userId: string) {
  const tournament = await prisma.tournament.findUnique({
    where: { id: tournamentId },
    include: { club: true },
  });
  if (!tournament) return { tournament: null, owns: false };
  return { tournament, owns: tournament.club.ownerId === userId };
}

/** Yeni turnuva olustur (DRAFT durumunda baslar). Sadece kulubun sahibi olan ADMIN yapabilir. */
tournamentsRouter.post("/", requireAuth, requireAdmin, async (req, res) => {
  const { clubId, name, seedingType } = req.body as {
    clubId: string;
    name: string;
    seedingType?: SeedingType;
  };

  if (!clubId || !name) {
    return res.status(400).json({ error: "clubId ve name zorunlu" });
  }

  const club = await prisma.club.findUnique({ where: { id: clubId } });
  if (!club) return res.status(404).json({ error: "Kulup bulunamadi" });
  if (club.ownerId !== req.user!.userId) {
    return res.status(403).json({ error: "Bu kulup senin degil" });
  }

  const tournament = await prisma.tournament.create({
    data: {
      clubId,
      name,
      seedingType: (seedingType ?? "random").toUpperCase() as any,
      drawSize: 0, // katilimcilar eklenip bracket uretilince belirlenecek
    },
  });

  res.status(201).json(tournament);
});

/** Bir kulubun turnuvalarini listele — giris yapmis herkes gorebilir. */
tournamentsRouter.get("/club/:clubId", requireAuth, async (req, res) => {
  const tournaments = await prisma.tournament.findMany({
    where: { clubId: req.params.clubId },
    orderBy: { createdAt: "desc" },
  });
  res.json(tournaments);
});

/** Tek bir turnuvayi katilimcilari ve tum maclariyla birlikte getir. */
tournamentsRouter.get("/:id", requireAuth, async (req, res) => {
  const tournament = await prisma.tournament.findUnique({
    where: { id: req.params.id },
    include: {
      participants: { include: { player: true } },
      matches: { orderBy: [{ round: "asc" }, { position: "asc" }] },
    },
  });

  if (!tournament) return res.status(404).json({ error: "Turnuva bulunamadi" });
  res.json(tournament);
});

/** Turnuvaya katilimci ekle. Sadece turnuvanin kulubunun sahibi ekleyebilir. */
tournamentsRouter.post("/:id/participants", requireAuth, requireAdmin, async (req, res) => {
  const { playerId, seed } = req.body as { playerId: string; seed?: number };
  if (!playerId) return res.status(400).json({ error: "playerId zorunlu" });

  const { tournament, owns } = await loadOwnedTournament(req.params.id, req.user!.userId);
  if (!tournament) return res.status(404).json({ error: "Turnuva bulunamadi" });
  if (!owns) return res.status(403).json({ error: "Bu turnuva senin kulubune ait degil" });
  if (tournament.status !== "DRAFT") {
    return res.status(400).json({ error: "Bracket uretildikten sonra katilimci eklenemez" });
  }

  const participant = await prisma.participant.create({
    data: { tournamentId: tournament.id, playerId, seed },
  });
  res.status(201).json(participant);
});

/**
 * Bracket'i uret: katilimcilari + seedingType'i kullanarak packages/core'daki
 * motoru calistirir, sonucu Match tablosuna yazar ve turnuvayi ACTIVE yapar.
 * Bu islem sadece bir kere yapilabilir (DRAFT -> ACTIVE gecisinde) ve sadece
 * turnuvanin kulubunun sahibi tarafindan.
 */
tournamentsRouter.post("/:id/generate-bracket", requireAuth, requireAdmin, async (req, res) => {
  const { tournament, owns } = await loadOwnedTournament(req.params.id, req.user!.userId);
  if (!tournament) return res.status(404).json({ error: "Turnuva bulunamadi" });
  if (!owns) return res.status(403).json({ error: "Bu turnuva senin kulubune ait degil" });
  if (tournament.status !== "DRAFT") {
    return res.status(400).json({ error: "Bu turnuva icin bracket zaten uretilmis" });
  }

  const participants = await prisma.participant.findMany({
    where: { tournamentId: tournament.id },
    include: { player: true },
  });
  if (participants.length < 2) {
    return res.status(400).json({ error: "Bracket icin en az 2 katilimci gerekir" });
  }

  const seedingType = tournament.seedingType.toLowerCase() as SeedingType;
  const participantInputs: ParticipantInput[] = participants.map((p) => ({
    id: p.id,
    playerName: p.player.name,
    rating: p.player.rating,
    seed: p.seed ?? undefined,
  }));

  const bracket = generateBracket(participantInputs, seedingType);

  // 1. gecis: tum maclari olustur (nextMatchId henuz bilinmiyor)
  const dbIdByRoundPosition = new Map<string, string>();
  for (const m of bracket.matches) {
    const created = await prisma.match.create({
      data: {
        tournamentId: tournament.id,
        round: m.round,
        position: m.position,
        player1Id: m.player1Id,
        player2Id: m.player2Id,
        winnerId: m.winnerId,
        status: m.status,
      },
    });
    dbIdByRoundPosition.set(`${m.round}-${m.position}`, created.id);
  }

  // 2. gecis: nextMatchId baglantilarini kur
  for (const m of bracket.matches) {
    if (m.nextMatchRound === null || m.nextMatchPosition === null) continue;
    const matchDbId = dbIdByRoundPosition.get(`${m.round}-${m.position}`)!;
    const nextDbId = dbIdByRoundPosition.get(`${m.nextMatchRound}-${m.nextMatchPosition}`)!;
    await prisma.match.update({ where: { id: matchDbId }, data: { nextMatchId: nextDbId } });
  }

  const updated = await prisma.tournament.update({
    where: { id: tournament.id },
    data: { status: "ACTIVE", drawSize: bracket.drawSize },
    include: { matches: { orderBy: [{ round: "asc" }, { position: "asc" }] } },
  });

  res.json(updated);
});

/**
 * Bir macin sonucunu kaydet. Kazanan otomatik olarak bracket'ta bir sonraki
 * maca ilerletilir (nextMatchId varsa). Sadece turnuvanin kulubunun sahibi.
 */
tournamentsRouter.post(
  "/:tournamentId/matches/:matchId/result",
  requireAuth,
  requireAdmin,
  async (req, res) => {
    const { winnerId, score } = req.body as { winnerId: string; score?: string };
    if (!winnerId) return res.status(400).json({ error: "winnerId zorunlu" });

    const { tournament, owns } = await loadOwnedTournament(
      req.params.tournamentId,
      req.user!.userId
    );
    if (!tournament) return res.status(404).json({ error: "Turnuva bulunamadi" });
    if (!owns) return res.status(403).json({ error: "Bu turnuva senin kulubune ait degil" });

    const match = await prisma.match.findUnique({ where: { id: req.params.matchId } });
    if (!match || match.tournamentId !== req.params.tournamentId) {
      return res.status(404).json({ error: "Mac bulunamadi" });
    }
    if (match.player1Id !== winnerId && match.player2Id !== winnerId) {
      return res.status(400).json({ error: "winnerId bu macin oyunculari arasinda degil" });
    }
    if (!match.player1Id || !match.player2Id) {
      return res.status(400).json({ error: "Iki oyuncu da belli olmadan sonuc girilemez" });
    }

    // packages/core'daki advanceWinner ile ayni mantigi burada, tek bir mac +
    // (varsa) bir sonraki mac uzerinde, hafif bir bellek-ici temsille calistirip
    // sonra iki satiri da DB'ye yaziyoruz.
    const virtualMatches = [
      {
        round: match.round,
        position: match.position,
        player1Id: match.player1Id,
        player2Id: match.player2Id,
        winnerId: null as string | null,
        status: "SCHEDULED" as const,
        nextMatchRound: null as number | null,
        nextMatchPosition: null as number | null,
      },
    ];

    let nextMatchRecord = null;
    if (match.nextMatchId) {
      nextMatchRecord = await prisma.match.findUnique({ where: { id: match.nextMatchId } });
      if (nextMatchRecord) {
        virtualMatches[0].nextMatchRound = nextMatchRecord.round;
        virtualMatches[0].nextMatchPosition = nextMatchRecord.position;
        virtualMatches.push({
          round: nextMatchRecord.round,
          position: nextMatchRecord.position,
          player1Id: nextMatchRecord.player1Id,
          player2Id: nextMatchRecord.player2Id,
          winnerId: nextMatchRecord.winnerId,
          status: nextMatchRecord.status as any,
          nextMatchRound: null,
          nextMatchPosition: null,
        });
      }
    }

    advanceWinner(virtualMatches, match.round, match.position, winnerId, score);

    const updatedMatch = await prisma.match.update({
      where: { id: match.id },
      data: { winnerId, status: "COMPLETED", score: score ?? null },
    });

    let updatedNext = null;
    if (nextMatchRecord) {
      const nv = virtualMatches[1];
      updatedNext = await prisma.match.update({
        where: { id: nextMatchRecord.id },
        data: {
          player1Id: nv.player1Id,
          player2Id: nv.player2Id,
          status: nv.status,
        },
      });
    }

    res.json({ match: updatedMatch, nextMatch: updatedNext });
  }
);
