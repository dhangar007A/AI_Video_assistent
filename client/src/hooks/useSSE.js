import { useState, useEffect, useCallback } from 'react';
import { getStreamUrl } from '../utils/api';

/**
 * Custom hook that connects to the SSE endpoint for a given meeting
 * and returns real-time progress updates.
 */
export default function useSSE(meetingId) {
  const [progress, setProgress] = useState([]);
  const [currentStage, setCurrentStage] = useState('');
  const [isComplete, setIsComplete] = useState(false);
  const [error, setError] = useState(null);

  const connect = useCallback(() => {
    if (!meetingId) return;

    const url = getStreamUrl(meetingId);
    const eventSource = new EventSource(url);

    eventSource.addEventListener('progress', (e) => {
      const data = JSON.parse(e.data);
      setCurrentStage(data.stage);
      setProgress((prev) => {
        // Replace if same stage, otherwise append
        const existing = prev.findIndex((p) => p.stage === data.stage && !p.done);
        if (existing >= 0) {
          const updated = [...prev];
          updated[existing] = data;
          return updated;
        }
        return [...prev, data];
      });
    });

    eventSource.addEventListener('complete', () => {
      setIsComplete(true);
      eventSource.close();
    });

    eventSource.addEventListener('error', (e) => {
      try {
        const data = JSON.parse(e.data);
        setError(data.message);
      } catch {
        setError('Connection lost');
      }
      eventSource.close();
    });

    eventSource.onerror = () => {
      // Browser-level EventSource error (connection drop, etc.)
      eventSource.close();
    };

    return () => eventSource.close();
  }, [meetingId]);

  useEffect(() => {
    const cleanup = connect();
    return () => cleanup?.();
  }, [connect]);

  return { progress, currentStage, isComplete, error };
}
