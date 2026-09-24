const express = require("express");
const Meeting = require("../models/Meeting");
const { askQuestion } = require("../services/pipeline");

const router = express.Router();

// ── POST /api/chat/:meetingId — Ask a question via RAG ───────────────────────
router.post("/:meetingId", async (req, res) => {
  try {
    const { question } = req.body;
    if (!question) {
      return res.status(400).json({ error: "question is required" });
    }

    const meeting = await Meeting.findById(req.params.meetingId);
    if (!meeting) {
      return res.status(404).json({ error: "Meeting not found" });
    }
    if (meeting.status !== "completed") {
      return res
        .status(400)
        .json({ error: "Meeting is still processing or failed" });
    }

    // Save user message
    meeting.chatHistory.push({ role: "user", content: question });

    // Get answer from RAG
    const answer = await askQuestion(req.params.meetingId, question);

    // Save assistant message
    meeting.chatHistory.push({ role: "assistant", content: answer });
    await meeting.save();

    res.json({ answer, chatHistory: meeting.chatHistory });
  } catch (err) {
    console.error("Chat error:", err);
    res.status(500).json({ error: err.message });
  }
});

// ── GET /api/chat/:meetingId/history — Get chat history ──────────────────────
router.get("/:meetingId/history", async (req, res) => {
  try {
    const meeting = await Meeting.findById(req.params.meetingId).select(
      "chatHistory"
    );
    if (!meeting) {
      return res.status(404).json({ error: "Meeting not found" });
    }
    res.json(meeting.chatHistory);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
