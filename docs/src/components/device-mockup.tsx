"use client";

import { useEffect, useRef, useState } from "react";
import { getAssetPath } from "@/lib/site";

/**
 * A screenshot from public/images/. Until that file exists it shows the app icon and the screen's name on the page
 * colour, so the site looks finished before the screenshots are taken: drop in the .jpg and it appears.
 */
export function Screen({ src, label, className = "" }: { src: string; label: string; className?: string }) {
  const [missing, setMissing] = useState(false);
  const img = useRef<HTMLImageElement>(null);
  // a static page can fail to load the image before React is listening, so check once it is
  useEffect(() => { if (img.current?.complete && img.current.naturalWidth === 0) setMissing(true); }, []);
  if (missing) {
    return (
      <div className={`aspect-[9/19.5] w-full flex flex-col items-center justify-center gap-4 bg-surface-container-high ${className}`}>
        <img src={getAssetPath("/icon.svg")} alt="" className="w-16 h-16 opacity-90" />
        <span className="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">{label}</span>
      </div>
    );
  }
  return <img ref={img} src={getAssetPath(src)} alt={label} onError={() => setMissing(true)} className={`w-full h-auto block ${className}`} />;
}

const GLOW = { primary: "bg-primary/20", secondary: "bg-secondary/20", tertiary: "bg-tertiary/20" };
export type Glow = keyof typeof GLOW;

/** A phone around a screenshot, glowing in one of the theme's colours. */
export function DeviceMockup({ src, label, color = "primary" }: { src: string; label: string; color?: Glow }) {
  return (
    <div className="relative group flex justify-center">
      <div className={`absolute -inset-10 ${GLOW[color]} rounded-full blur-[80px] group-hover:opacity-100 transition-opacity opacity-50`} />
      <div className="relative w-[220px] sm:w-[280px] bg-black rounded-[2rem] sm:rounded-[2.5rem] p-2 sm:p-3 shadow-2xl border-[4px] sm:border-[6px] border-[#1a1a1a] overflow-hidden transition-transform duration-500 group-hover:scale-105 group-hover:-rotate-1">
        <Screen src={src} label={label} className="rounded-[1.4rem] sm:rounded-[1.8rem]" />
      </div>
    </div>
  );
}
