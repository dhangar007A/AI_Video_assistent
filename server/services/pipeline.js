const { spawn } = require("child_process");
const path = require("path");

const PYTHON_PATH = process.env.PYTHON_PATH || "python";
const PROJECT_ROOT = path.resolve(
  __dirname,
  "..",
  process.env.PROJECT_ROOT || ".."
);

/**
 * Spawn the Python pipeline bridge and return a handler object.
 *
 * @param {string} source    - YouTube URL or absolute file path
 * @param {string} language  - "english" or "hinglish"
 * @param {function} onProgress - Called with each progress JSON event
 * @param {function} onResult   - Called with the final result JSON
 * @param {function} onError    - Called on error
 */
function runPipeline(source, language, meetingId, { onProgress, onResult, onError }) {
  const scriptPath = path.join(PROJECT_ROOT, "api_bridge.py");

  const child = spawn(
    PYTHON_PATH,
    [scriptPath, "--source", source, "--language", language],
    {
      cwd: PROJECT_ROOT,
      env: { ...process.env, MEETING_ID: meetingId },
      stdio: ["pipe", "pipe", "pipe"],
    }
  );

  let stderrBuffer = "";

  child.stdout.on("data", (data) => {
    const lines = data.toString().split("\n").filter(Boolean);

    for (const line of lines) {
      try {
        const parsed = JSON.parse(line);

        if (parsed.event === "progress" && onProgress) {
          onProgress(parsed);
        } else if (parsed.event === "result" && onResult) {
          onResult(parsed);
        } else if (parsed.event === "error" && onError) {
          onError(parsed);
        }
      } catch {
        // Non-JSON stdout line — ignore (Python print statements, etc.)
      }
    }
  });

  child.stderr.on("data", (data) => {
    stderrBuffer += data.toString();
  });

  child.on("close", (code) => {
    if (code !== 0 && onError) {
      onError({
        message: `Python process exited with code ${code}`,
        traceback: stderrBuffer,
      });
    }
  });

  return child;
}

/**
 * Ask a question to the RAG chain via the chat bridge.
 *
 * @param {string} question
 * @returns {Promise<string>} answer
 */
function askQuestion(meetingId, question) {
  return new Promise((resolve, reject) => {
    const scriptPath = path.join(PROJECT_ROOT, "chat_bridge.py");

    const child = spawn(
      PYTHON_PATH,
      [scriptPath, "--question", question],
      {
        cwd: PROJECT_ROOT,
        env: { ...process.env, MEETING_ID: meetingId },
        stdio: ["pipe", "pipe", "pipe"],
      }
    );

    let stdout = "";
    let stderr = "";

    child.stdout.on("data", (d) => (stdout += d.toString()));
    child.stderr.on("data", (d) => (stderr += d.toString()));

    child.on("close", (code) => {
      if (code !== 0) {
        return reject(new Error(stderr || `Exit code ${code}`));
      }

      try {
        const result = JSON.parse(stdout.trim().split("\n").pop());
        if (result.error) return reject(new Error(result.error));
        resolve(result.answer);
      } catch (e) {
        reject(new Error(`Failed to parse Python output: ${stdout}`));
      }
    });
  });
}

module.exports = { runPipeline, askQuestion };
