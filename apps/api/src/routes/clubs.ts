import { Router } from "express";
import { prisma } from "../prisma";
import { requireAuth, requireAdmin } from "../middleware/auth";

export const clubsRouter = Router();

/** Giris yapmis herhangi biri (ADMIN ya da PLAYER) kulupleri gorebilir. */
clubsRouter.get("/", requireAuth, async (_req, res) => {
  const clubs = await prisma.club.findMany({ orderBy: { createdAt: "desc" } });
  res.json(clubs);
});

/** Sadece ADMIN kulup olusturabilir — olusturan kisi otomatik sahibi olur. */
clubsRouter.post("/", requireAuth, requireAdmin, async (req, res) => {
  const { name } = req.body as { name: string };
  if (!name) return res.status(400).json({ error: "name zorunlu" });

  const club = await prisma.club.create({
    data: { name, ownerId: req.user!.userId },
  });
  res.status(201).json(club);
});

clubsRouter.post("/:clubId/players", requireAuth, requireAdmin, async (req, res) => {
  const club = await prisma.club.findUnique({ where: { id: req.params.clubId } });
  if (!club) return res.status(404).json({ error: "Kulup bulunamadi" });
  if (club.ownerId !== req.user!.userId) {
    return res.status(403).json({ error: "Bu kulup senin degil" });
  }

  const { name, phone, rating } = req.body as {
    name: string;
    phone?: string;
    rating?: number;
  };
  if (!name) return res.status(400).json({ error: "name zorunlu" });

  const player = await prisma.player.create({
    data: { clubId: req.params.clubId, name, phone, rating: rating ?? 1000 },
  });
  res.status(201).json(player);
});

clubsRouter.get("/:clubId/players", requireAuth, async (req, res) => {
  const players = await prisma.player.findMany({
    where: { clubId: req.params.clubId },
    orderBy: { name: "asc" },
  });
  res.json(players);
});
