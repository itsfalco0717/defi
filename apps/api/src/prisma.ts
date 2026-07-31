import { PrismaClient } from "@prisma/client";

// Dev ortamında ts-node-dev her dosya degisikliginde yeniden yuklendigi
// icin PrismaClient'i global'de tutup coklu baglanti acilmasini onluyoruz.
declare global {
  // eslint-disable-next-line no-var
  var __prisma: PrismaClient | undefined;
}

export const prisma = global.__prisma ?? new PrismaClient();

if (process.env.NODE_ENV !== "production") {
  global.__prisma = prisma;
}
