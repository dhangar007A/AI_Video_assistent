const express = require("express");
const multer = require("multer");
const path = require("path");
const fs = require("fs");
const Meeting = require("../models/Meeting");
const { runPipeline } = require("../services/pipeline");

const router = express.Router();

// ── File Upload Config ───────────────────────────────────────────────────────
const uploadsDir = path.join(__dirname, "..", "uploads");
if (!fs.existsSync(uploadsDir)) fs.mkdirSync(uploadsDir, { recursive: true });

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadsDir),
  filename: (req, file, cb) => {
    const unique = Date.now() + "-" + Math.round(Math.random() * 1e9);
    cb(null, unique + path.extname(file.originalname));
  },
});
const upload = multer({ storage, limits: { fileSize: 500 * 1024 * 1024 } }); // 500MB

// ── In-memory SSE client map ─────────────────────────────────────────────────
const sseClients = new Map(); // meetingId -> [res, res, ...]

function sendSSE(meetingId, event, data) {
  const clients = sseClients.get(meetingId) || [];
  const payload = `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`;
  clients.forEach((res) => res.write(payload));
}

// ── POST /api/meetings — Start processing ────────────────────────────────────
router.post("/", upload.single("file"), async (req, res) => {
  try {
    const { source, language = "english" } = req.body;
    let finalSource = source;

    // If a file was uploaded, use its path as the source
    if (req.file) {
      finalSource = req.file.path;
    }

    if (!finalSource) {
      return res.status(400).json({ error: "Provide a YouTube URL or upload a file" });
    }

    // Create meeting record
    const meeting = new Meeting({
      source: finalSource,
      language,
      status: "processing",
      currentStage: "downloading",
    });
    await meeting.save();

    // Start the pipeline in the background
    runPipeline(finalSource, language, meeting._id.toString(), {
      onProgress: async (evt) => {
        try {
          await Meeting.updateOne(
            { _id: meeting._id },
            { $set: { currentStage: evt.stage } }
          );
          sendSSE(meeting._id.toString(), "progress", evt);
        } catch (e) {
          console.error("Progress update error:", e.message);
        }
      },

      onResult: async (evt) => {
        try {
          meeting.status = "completed";
          meeting.title = evt.title || "";
          meeting.transcript = evt.transcript || "";
          meeting.summary = evt.summary || "";
          meeting.actionItems = evt.action_items || "";
          meeting.keyDecisions = evt.key_decisions || "";
          meeting.openQuestions = evt.open_questions || "";
          meeting.currentStage = "done";
          await meeting.save();
          sendSSE(meeting._id.toString(), "complete", {
            meetingId: meeting._id,
          });
        } catch (e) {
          console.error("Result save error:", e.message);
        }
      },

      onError: async (evt) => {
        try {
          console.error("❌ Pipeline execution failed:", evt.message);
          if (evt.traceback) {
            console.error("Python Traceback:\n", evt.traceback);
          }
          meeting.status = "failed";
          meeting.errorMessage = evt.message || "Unknown error";
          await meeting.save();
          sendSSE(meeting._id.toString(), "error", { message: evt.message });
        } catch (e) {
          console.error("Error save error:", e.message);
        }
      },
    });

    res.status(201).json({
      meetingId: meeting._id,
      status: "processing",
      message: "Pipeline started. Connect to SSE for progress.",
    });
  } catch (err) {
    console.error("POST /api/meetings error:", err);
    res.status(500).json({ error: err.message });
  }
});

// ── GET /api/meetings — List all meetings ────────────────────────────────────
router.get("/", async (req, res) => {
  try {
    const meetings = await Meeting.find()
      .select("-transcript -chatHistory")
      .sort({ createdAt: -1 })
      .limit(50);
    res.json(meetings);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── GET /api/meetings/:id — Get single meeting ──────────────────────────────
router.get("/:id", async (req, res) => {
  try {
    const meeting = await Meeting.findById(req.params.id);
    if (!meeting) return res.status(404).json({ error: "Meeting not found" });
    res.json(meeting);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// ── GET /api/meetings/:id/stream — SSE progress ─────────────────────────────
router.get("/:id/stream", (req, res) => {
  const meetingId = req.params.id;

  res.writeHead(200, {
    "Content-Type": "text/event-stream",
    "Cache-Control": "no-cache",
    Connection: "keep-alive",
  });
  res.write("\n");

  // Register this client
  if (!sseClients.has(meetingId)) sseClients.set(meetingId, []);
  sseClients.get(meetingId).push(res);

  // Remove on disconnect
  req.on("close", () => {
    const clients = sseClients.get(meetingId) || [];
    sseClients.set(
      meetingId,
      clients.filter((c) => c !== res)
    );
  });
});

// ── DELETE /api/meetings/:id — Delete meeting ────────────────────────────────
router.delete("/:id", async (req, res) => {
  try {
    await Meeting.findByIdAndDelete(req.params.id);
    // Clean up vector database directory for this meeting
    const dbDir = path.resolve(__dirname, "..", "..", `vector_db_${req.params.id}`);
    if (fs.existsSync(dbDir)) {
      fs.rmSync(dbDir, { recursive: true, force: true });
    }
    res.json({ message: "Meeting deleted" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
