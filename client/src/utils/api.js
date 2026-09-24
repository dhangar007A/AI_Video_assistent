import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_URL || '/api';

const api = axios.create({
  baseURL: API_BASE,
  timeout: 120000,
  headers: { 'Content-Type': 'application/json' },
});

// ── Meeting endpoints ────────────────────────────────────────────────────────
export const createMeeting = async (source, language = 'english') => {
  const { data } = await api.post('/meetings', { source, language });
  return data;
};

export const uploadMeeting = async (file, language = 'english') => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('language', language);
  const { data } = await api.post('/meetings', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
};

export const getMeetings = async () => {
  const { data } = await api.get('/meetings');
  return data;
};

export const getMeeting = async (id) => {
  const { data } = await api.get(`/meetings/${id}`);
  return data;
};

export const deleteMeeting = async (id) => {
  const { data } = await api.delete(`/meetings/${id}`);
  return data;
};

// ── Chat endpoints ───────────────────────────────────────────────────────────
export const sendChatMessage = async (meetingId, question) => {
  const { data } = await api.post(`/chat/${meetingId}`, { question });
  return data;
};

export const getChatHistory = async (meetingId) => {
  const { data } = await api.get(`/chat/${meetingId}/history`);
  return data;
};

// ── SSE Stream URL ───────────────────────────────────────────────────────────
export const getStreamUrl = (meetingId) =>
  `${API_BASE}/meetings/${meetingId}/stream`;

export default api;
