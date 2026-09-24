"""
api_bridge.py — Bridge between Express.js and the Python AI pipeline.

Express spawns this script as a child process. It prints JSON events to
stdout so the Node server can parse them and relay SSE progress to the
React frontend.

Usage:
    python api_bridge.py --source <url_or_path> --language <english|hinglish>
"""

import argparse
import json
import sys
import os
import traceback

# Ensure project root is on the path so we can import core/ and utils/
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# Prepend local FFmpeg path to PATH environment variable so Whisper/Pydub can find it
FFMPEG_PATH = r"C:\Users\abhis\Downloads\ffmpeg-8.1.1-essentials_build\ffmpeg-8.1.1-essentials_build\bin"
if FFMPEG_PATH not in os.environ.get("PATH", ""):
    os.environ["PATH"] = FFMPEG_PATH + os.pathsep + os.environ.get("PATH", "")

from dotenv import load_dotenv

load_dotenv()


def emit(event_type: str, data: dict):
    """Print a JSON event line to stdout (consumed by Express)."""
    payload = {"event": event_type, **data}
    print(json.dumps(payload), flush=True)


def main():
    parser = argparse.ArgumentParser(description="AI Video Assistant Pipeline Bridge")
    parser.add_argument("--source", required=True, help="YouTube URL or local file path")
    parser.add_argument("--language", default="english", help="english or hinglish")
    args = parser.parse_args()

    try:
        # ── Step 1: Audio Processing ──────────────────────────────────────
        emit("progress", {"stage": "downloading", "message": "Downloading & processing audio..."})

        from utils.audio_processor import process_input
        chunks = process_input(args.source)
        emit("progress", {"stage": "downloading", "message": f"Audio ready — {len(chunks)} chunk(s)", "done": True})

        # ── Step 2: Transcription ─────────────────────────────────────────
        emit("progress", {"stage": "transcribing", "message": "Transcribing audio..."})

        from core.transcriber import transcribe_all
        transcript = transcribe_all(chunks, args.language)
        emit("progress", {"stage": "transcribing", "message": "Transcription complete", "done": True})

        # ── Step 3: Title Generation ──────────────────────────────────────
        emit("progress", {"stage": "summarizing", "message": "Generating title..."})

        from core.summarizer import generate_title, summarize
        title = generate_title(transcript)

        # ── Step 4: Summarization ─────────────────────────────────────────
        emit("progress", {"stage": "summarizing", "message": "Summarizing transcript..."})
        summary = summarize(transcript)
        emit("progress", {"stage": "summarizing", "message": "Summary complete", "done": True})

        # ── Step 5: Extraction ────────────────────────────────────────────
        emit("progress", {"stage": "extracting", "message": "Extracting action items, decisions & questions..."})

        from core.extractor import extract_action_items, extract_key_decisions, extract_questions
        action_items = extract_action_items(transcript)
        decisions = extract_key_decisions(transcript)
        questions = extract_questions(transcript)
        emit("progress", {"stage": "extracting", "message": "Extraction complete", "done": True})

        # ── Step 6: RAG Vector Store ──────────────────────────────────────
        emit("progress", {"stage": "building_rag", "message": "Building RAG vector store..."})

        from core.vector_store import build_vector_store
        build_vector_store(transcript)
        emit("progress", {"stage": "building_rag", "message": "RAG ready", "done": True})

        # ── Final Result ──────────────────────────────────────────────────
        emit("result", {
            "title": title,
            "transcript": transcript,
            "summary": summary,
            "action_items": action_items,
            "key_decisions": decisions,
            "open_questions": questions,
        })

    except Exception as e:
        emit("error", {"message": str(e), "traceback": traceback.format_exc()})
        sys.exit(1)


if __name__ == "__main__":
    main()
