import React from 'react';
import { usePlayer } from '../context/PlayerContext';
import { Home, Compass, Library, Users, Settings, Sparkles, Mic, Radio } from 'lucide-react';

interface NavigationProps {
  currentTab: string;
  onTabChange: (tab: string) => void;
  onOpenRecognizer: () => void;
  onOpenSettings: () => void;
}

export const Navigation: React.FC<NavigationProps> = ({
  currentTab,
  onTabChange,
  onOpenRecognizer,
  onOpenSettings,
}) => {
  const { currentSong, isPlaying } = usePlayer();

  const navItems = [
    { id: 'home', label: 'Listen Now', icon: Home },
    { id: 'search', label: 'Browse', icon: Compass },
    { id: 'library', label: 'Library', icon: Library },
    { id: 'jam', label: 'Together', icon: Users },
  ];

  // Dynamic ambient color from current track or Apple Music signature crimson
  const ambientColor = currentSong?.colorHex || '#fa243c';

  return (
    <>
      {/* Dynamic Top Header with Apple Liquid Glass */}
      <header className="sticky top-0 z-30 w-full apple-liquid-navbar transition-colors duration-700">
        {/* Ambient liquid light glow at top */}
        <div
          className="absolute -top-12 left-1/2 -translate-x-1/2 w-3/4 h-24 rounded-full opacity-25 blur-3xl pointer-events-none transition-all duration-1000"
          style={{ backgroundColor: ambientColor }}
        />

        <div className="max-w-5xl mx-auto px-4 py-3 flex items-center justify-between relative z-10">
          {/* Logo with Apple Music Liquid Wave */}
          <div className="flex items-center gap-3">
            <div className="relative group cursor-pointer">
              {/* Liquid glass icon bubble */}
              <div
                className="w-9 h-9 rounded-2xl p-[1px] bg-gradient-to-tr from-white/30 via-white/10 to-transparent shadow-lg shadow-black/40 transition-transform duration-300 group-hover:scale-105"
              >
                <div
                  className="w-full h-full rounded-2xl flex items-center justify-center transition-all duration-700 relative overflow-hidden"
                  style={{
                    background: `linear-gradient(135deg, ${ambientColor} 0%, #1e1e2d 100%)`,
                  }}
                >
                  {/* Glass refraction reflection */}
                  <div className="absolute inset-0 bg-gradient-to-b from-white/40 via-transparent to-black/20 pointer-events-none" />
                  
                  {isPlaying ? (
                    <div className="flex items-end gap-[2px] h-3.5 relative z-10">
                      <span className="w-1 bg-white rounded-full animate-bounce [animation-duration:550ms]" />
                      <span className="w-1 bg-white rounded-full animate-bounce [animation-duration:750ms]" />
                      <span className="w-1 bg-white rounded-full animate-bounce [animation-duration:620ms]" />
                    </div>
                  ) : (
                    <Radio className="w-4 h-4 text-white relative z-10" />
                  )}
                </div>
              </div>
            </div>

            <div>
              <div className="flex items-center gap-1.5">
                <h1 className="text-base font-extrabold tracking-tight font-outfit text-white">
                  Echo Music
                </h1>
                <span className="text-[9px] font-bold px-1.5 py-0.5 rounded-full bg-white/10 border border-white/15 text-slate-300 backdrop-blur-md uppercase tracking-wider">
                  Liquid Glass
                </span>
              </div>
              <p className="text-[11px] text-slate-400 font-medium">
                Lossless • Dolby Atmos
              </p>
            </div>
          </div>

          {/* Right Action Buttons */}
          <div className="flex items-center gap-2">
            {/* Song ID Liquid Button */}
            <button
              onClick={onOpenRecognizer}
              className="group relative flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-white/10 hover:bg-white/15 active:scale-95 transition-all text-xs font-semibold text-slate-200 border border-white/20 shadow-md backdrop-blur-xl overflow-hidden"
              title="Identify Playing Track (Shazam SoundID)"
            >
              {/* Liquid shine sweep */}
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-700 ease-out" />
              
              <Mic className="w-3.5 h-3.5 text-rose-400 group-hover:scale-110 transition-transform" />
              <span className="hidden sm:inline">Identify</span>
            </button>

            {/* Settings Liquid Glass Button */}
            <button
              onClick={onOpenSettings}
              className="p-2 rounded-full bg-white/10 hover:bg-white/15 active:scale-90 text-slate-200 border border-white/15 transition-all shadow-md backdrop-blur-xl"
              title="Settings & Themes"
            >
              <Settings className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      {/* Apple Music Dynamic Liquid Glass Floating Dock (Bottom) */}
      <nav className="fixed bottom-0 left-0 right-0 z-30 flex justify-center pb-3 px-4 pointer-events-none">
        <div className="pointer-events-auto relative max-w-sm w-full animate-liquid-float">
          {/* Reactive Liquid Ambient Underglow */}
          <div
            className="absolute -inset-1.5 rounded-full opacity-40 blur-xl transition-all duration-700 pointer-events-none"
            style={{
              background: `radial-gradient(ellipse at center, ${ambientColor} 0%, rgba(0,0,0,0) 70%)`,
            }}
          />

          {/* The Liquid Glass Capsule Dock */}
          <div className="relative apple-liquid-dock rounded-full px-2 py-1.5 flex items-center justify-around overflow-hidden">
            {/* Prismatic Top Rim Highlight */}
            <div className="absolute top-0 left-6 right-6 h-[1px] bg-gradient-to-r from-transparent via-white/60 to-transparent pointer-events-none" />

            {/* Navigation Tabs */}
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = currentTab === item.id;

              return (
                <button
                  key={item.id}
                  onClick={() => onTabChange(item.id)}
                  className={`relative flex flex-col items-center justify-center py-1.5 px-4 rounded-full transition-all duration-300 group ${
                    isActive ? 'scale-105' : 'hover:scale-100 active:scale-95'
                  }`}
                >
                  {/* Dynamic Active Liquid Glass Pill */}
                  {isActive && (
                    <div
                      className="absolute inset-0 rounded-full liquid-active-pill animate-fadeIn"
                      style={{
                        background: `linear-gradient(135deg, ${ambientColor}55 0%, rgba(255,255,255,0.2) 100%)`,
                      }}
                    >
                      {/* Glossy Reflection Arc */}
                      <div className="absolute top-0 inset-x-2 h-[1px] bg-gradient-to-r from-transparent via-white/80 to-transparent" />
                    </div>
                  )}

                  {/* Icon */}
                  <div className="relative z-10 transition-transform duration-200 group-hover:-translate-y-0.5">
                    <Icon
                      className={`w-5 h-5 transition-all duration-300 ${
                        isActive
                          ? 'text-white drop-shadow-[0_2px_8px_rgba(255,255,255,0.5)] stroke-[2.3px]'
                          : 'text-slate-400 group-hover:text-slate-200 stroke-[1.8px]'
                      }`}
                    />
                  </div>

                  {/* Label */}
                  <span
                    className={`relative z-10 text-[10px] mt-0.5 tracking-tight transition-all duration-300 font-medium ${
                      isActive
                        ? 'text-white font-bold drop-shadow-sm'
                        : 'text-slate-400 group-hover:text-slate-300'
                    }`}
                  >
                    {item.label}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      </nav>
    </>
  );
};
