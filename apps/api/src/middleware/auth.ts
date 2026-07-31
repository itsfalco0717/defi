import { Request, Response, NextFunction } from "express";
import jwt from "jsonwebtoken";

// UYARI: gercek bir production ortaminda bu deger .env icinde ayri, uzun
// ve rastgele bir string olmali (JWT_SECRET). Burada gelistirme kolayligi
// icin bir varsayilan deger var — .env'e JWT_SECRET eklemeyi unutma.
const JWT_SECRET = process.env.JWT_SECRET || "dev-secret-change-me";

export type Role = "ADMIN" | "PLAYER";

export interface AuthPayload {
  userId: string;
  role: Role;
}

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Express {
    interface Request {
      user?: AuthPayload;
    }
  }
}

export function signToken(payload: AuthPayload): string {
  return jwt.sign(payload, JWT_SECRET, { expiresIn: "30d" });
}

/** Gecerli bir Authorization: Bearer <token> header'i zorunlu kilar. */
export function requireAuth(req: Request, res: Response, next: NextFunction) {
  const header = req.headers.authorization;
  if (!header || !header.startsWith("Bearer ")) {
    return res.status(401).json({ error: "Giris yapmaniz gerekiyor" });
  }

  const token = header.slice("Bearer ".length);
  try {
    req.user = jwt.verify(token, JWT_SECRET) as AuthPayload;
    next();
  } catch {
    return res.status(401).json({ error: "Gecersiz veya suresi dolmus oturum" });
  }
}

/** requireAuth'tan SONRA kullanilmali. Rolun ADMIN olmasini zorunlu kilar. */
export function requireAdmin(req: Request, res: Response, next: NextFunction) {
  if (req.user?.role !== "ADMIN") {
    return res.status(403).json({ error: "Bu islem icin yonetici yetkisi gerekiyor" });
  }
  next();
}
