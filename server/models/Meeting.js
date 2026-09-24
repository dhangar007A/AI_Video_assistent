const mongoose = require("mongoose");

const chatMessageSchema = new mongoose.Schema({
  role: { type: String, enum: ["user", "assistant"], required: true },
  content: { type: String, required: true },
  timestamp: { type: Date, default: Date.now },
});

const meetingSchema = new mongoose.Schema(
  {
    source: { type: String, required: true }, // YouTube URL or filename
    language: { type: String, default: "english" },
    status: {
      type: String,
      enum: ["processing", "completed", "failed"],
      default: "processing",
    },
    currentStage: { type: String, default: "" },
    title: { type: String, default: "" },
    transcript: { type: String, default: "" },
    summary: { type: String, default: "" },
    actionItems: { type: String, default: "" },
    keyDecisions: { type: String, default: "" },
    openQuestions: { type: String, default: "" },
    errorMessage: { type: String, default: "" },
    chatHistory: [chatMessageSchema],
  },
  { timestamps: true }
);

module.exports = mongoose.model("Meeting", meetingSchema);
