import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import { authRouter } from "./routes/auth";
import { clubsRouter } from "./routes/clubs";
import { tournamentsRouter } from "./routes/tournaments";

dotenv.config();

const app = express();
app.use(cors());
app.use(express.json());

app.get("/health", (_req, res) => res.json({ ok: true }));

app.use("/auth", authRouter);
app.use("/clubs", clubsRouter);
app.use("/tournaments", tournamentsRouter);

const port = process.env.PORT ? Number(process.env.PORT) : 4000;
app.listen(port, () => {
  console.log(`API http://localhost:${port} adresinde calisiyor`);
});
