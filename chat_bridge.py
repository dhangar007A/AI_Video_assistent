"""
chat_bridge.py — RAG chat bridge for Express.js.

Loads the existing ChromaDB vector store and answers a question using
the RAG chain. Returns JSON to stdout.

Usage:
    python chat_bridge.py --question "What was decided about the deadline?"
"""

import argparse
import json
import sys
import os
import traceback

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# pyrefly: ignore [missing-import]
from dotenv import load_dotenv

load_dotenv()


def main():
    parser = argparse.ArgumentParser(description="AI Video Assistant Chat Bridge")
    parser.add_argument("--question", required=True, help="User question")
    args = parser.parse_args()

    try:
        from core.rag_engine import load_rag_chain, ask_question

        rag_chain = load_rag_chain()
        answer = ask_question(rag_chain, args.question)

        result = {"answer": answer}
        print(json.dumps(result), flush=True)

    except Exception as e:
        error = {"error": str(e), "traceback": traceback.format_exc()}
        print(json.dumps(error), flush=True)
        sys.exit(1)


if __name__ == "__main__":
    main()
