import React, { useState } from 'react';
import { usePlayer } from '../context/PlayerContext';
import { Users, X, Copy, Check, MessageSquare, Send, Radio, Sparkles } from 'lucide-react';

interface ListenTogetherModalProps {
  isOpen: boolean;
  onClose: () => void;
}

interface Message {
  id: string;
  sender: string;
  text: string;
  time: string;
}

export const ListenTogetherModal: React.FC<ListenTogetherModalProps> = ({ isOpen, onClose }) => {
  const { currentSong, isPlaying } = usePlayer();
  const [roomCode, setRoomCode] = useState<string>('ECH-792');
  const [isHost, setIsHost] = useState<boolean>(true);
  const [copied, setCopied] = useState<boolean>(false);
  const [inputCode, setInputCode] = useState<string>('');
  const [inRoom, setInRoom] = useState<boolean>(true);

  // Chat
  const [messages, setMessages] = useState<Message[]>([
    { id: '1', sender: 'Aditya (Host)', text: 'Welcome to Echo Jam! Bass boost sounds incredible 🔥', time: 'Just now' },
    { id: '2', sender: 'Elena', text: 'This synth drop is unreal!', time: '1m ago' },
  ]);
  const [chatInput, setChatInput] = useState<string>('');

  if (!isOpen) return null;

  const handleCopyCode = () => {
    navigator.clipboard?.writeText(roomCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleJoin = (e: React.FormEvent) => {
    e.preventDefault();
    if (inputCode.trim().length > 3) {
      setRoomCode(inputCode.trim().toUpperCase());
      setIsHost(false);
      setInRoom(true);
    }
  };

  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatInput.trim()) return;
    const newMsg: Message = {
      id: Date.now().toString(),
      sender: isHost ? 'You (Host)' : 'You',
      text: chatInput.trim(),
      time: 'Just now',
    };
    setMessages(prev => [...prev, newMsg]);
    setChatInput('');
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-md animate-fadeIn">
      <div className="w-full max-w-md rounded-3xl bg-[#151a21] border border-white/10 p-6 shadow-2xl flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-white/10 flex-shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-purple-600 to-indigo-500 flex items-center justify-center text-white shadow-lg shadow-purple-600/30">
              <Users className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-bold text-base text-white font-outfit flex items-center gap-1.5">
                Listen Together
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              </h3>
              <p className="text-xs text-slate-400">Synchronized Jam Room</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-white/10 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Room Code Banner */}
        <div className="my-4 p-3.5 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-between">
          <div>
            <span className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">
              Room Code
            </span>
            <p className="text-lg font-mono font-bold text-purple-400 tracking-widest">
              {roomCode}
            </p>
          </div>

          <button
            onClick={handleCopyCode}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-600/20 hover:bg-purple-600/30 text-purple-300 text-xs font-semibold border border-purple-500/30 transition-all"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{copied ? 'Copied!' : 'Invite'}</span>
          </button>
        </div>

        {/* Currently Synced Track */}
        {currentSong && (
          <div className="flex items-center gap-3 p-3 rounded-2xl bg-gradient-to-r from-purple-950/40 to-slate-900 border border-purple-500/20 mb-4">
            <img
              src={currentSong.coverUrl}
              alt={currentSong.title}
              className="w-12 h-12 rounded-xl object-cover"
            />
            <div className="min-w-0 flex-1">
              <span className="text-[10px] text-purple-400 font-semibold uppercase tracking-wider flex items-center gap-1">
                <Radio className="w-3 h-3 animate-spin-slow" />
                Live Broadcast
              </span>
              <p className="text-sm font-semibold text-white truncate">{currentSong.title}</p>
              <p className="text-xs text-slate-400 truncate">{currentSong.artist}</p>
            </div>
          </div>
        )}

        {/* Listeners list */}
        <div className="flex items-center gap-2 mb-4 px-1">
          <span className="text-xs text-slate-400 font-medium">Listening:</span>
          <div className="flex -space-x-2">
            <span className="w-7 h-7 rounded-full bg-purple-600 border-2 border-[#151a21] text-[10px] font-bold text-white flex items-center justify-center">
              AD
            </span>
            <span className="w-7 h-7 rounded-full bg-rose-600 border-2 border-[#151a21] text-[10px] font-bold text-white flex items-center justify-center">
              EL
            </span>
            <span className="w-7 h-7 rounded-full bg-amber-600 border-2 border-[#151a21] text-[10px] font-bold text-white flex items-center justify-center">
              MK
            </span>
          </div>
          <span className="text-xs text-slate-500 ml-2">3 in sync</span>
        </div>

        {/* Live Chat Messages */}
        <div className="flex-1 overflow-y-auto space-y-2.5 p-3 rounded-2xl bg-black/20 border border-white/5 mb-3 min-h-[140px]">
          {messages.map((m) => (
            <div key={m.id} className="text-xs">
              <span className="font-semibold text-purple-300 mr-1.5">{m.sender}:</span>
              <span className="text-slate-300">{m.text}</span>
            </div>
          ))}
        </div>

        {/* Chat input */}
        <form onSubmit={handleSendMessage} className="flex gap-2">
          <input
            type="text"
            placeholder="Send reaction or comment..."
            value={chatInput}
            onChange={(e) => setChatInput(e.target.value)}
            className="flex-1 px-3 py-2 text-xs rounded-xl bg-white/5 border border-white/10 text-white placeholder-slate-500 focus:outline-none focus:border-purple-500"
          />
          <button
            type="submit"
            className="p-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white transition-all shadow-md shadow-purple-600/30"
          >
            <Send className="w-4 h-4" />
          </button>
        </form>
      </div>
    </div>
  );
};
