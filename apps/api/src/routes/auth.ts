import { Router } from "express";
import bcrypt from "bcryptjs";
import { prisma } from "../prisma";
import { signToken, Role } from "../middleware/auth";

export const authRouter = Router();

function toPublicUser(user: { id: string; email: string; name: string; role: string }) {
  return { id: user.id, email: user.email, name: user.name, role: user.role };
}

authRouter.post("/register", async (req, res) => {
  const { email, password, name, role } = req.body as {
    email: string;
    password: string;
    name: string;
    role: Role;
  };

  if (!email || !password || !name || !role) {
    return res.status(400).json({ error: "email, password, name ve role zorunlu" });
  }
  if (role !== "ADMIN" && role !== "PLAYER") {
    return res.status(400).json({ error: "role ADMIN ya da PLAYER olmali" });
  }
  if (password.length < 6) {
    return res.status(400).json({ error: "Sifre en az 6 karakter olmali" });
  }

  const existing = await prisma.user.findUnique({ where: { email } });
  if (existing) {
    return res.status(409).json({ error: "Bu e-posta zaten kayitli" });
  }

  const passwordHash = await bcrypt.hash(password, 10);
  const user = await prisma.user.create({ data: { email, passwordHash, name, role } });

  const token = signToken({ userId: user.id, role: user.role as Role });
  res.status(201).json({ token, user: toPublicUser(user) });
});

authRouter.post("/login", async (req, res) => {
  const { email, password } = req.body as { email: string; password: string };
  if (!email || !password) {
    return res.status(400).json({ error: "email ve password zorunlu" });
  }

  const user = await prisma.user.findUnique({ where: { email } });
  if (!user) {
    return res.status(401).json({ error: "E-posta veya sifre hatali" });
  }

  const valid = await bcrypt.compare(password, user.passwordHash);
  if (!valid) {
    return res.status(401).json({ error: "E-posta veya sifre hatali" });
  }

  const token = signToken({ userId: user.id, role: user.role as Role });
  res.json({ token, user: toPublicUser(user) });
});
