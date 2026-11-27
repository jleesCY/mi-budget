"use client";

import { Code2, FileText, Search as SearchIcon, Settings2, Shield, User, X } from "lucide-react";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { SETTINGS } from "@/lib/settings";
import { slug } from "@/lib/site";

interface SearchResult {
  title: string;
  type: "User Guide" | "Dev Guide" | "Settings" | "Architecture";
  href: string;
  excerpt: string;
}

const GUIDE: SearchResult[] = [
  // User Guide
  { title: "The Main Bar", type: "User Guide", href: "/user#the-bar", excerpt: "The + button, calendar, menu, Library and Settings, all in one bar at the bottom." },
  { title: "Gestures", type: "User Guide", href: "/user#gestures", excerpt: "Swipe the Pill right for the Library, left for Settings, up for the Calendar; hold it to search." },
  { title: "Change Month", type: "User Guide", href: "/user#gestures", excerpt: "Swipe the month title or the chart sideways." },
  { title: "Gesture Walkthrough", type: "User Guide", href: "/user#gestures", excerpt: "Try every swipe, step by step, from Settings › About." },
  { title: "Adding Purchases", type: "User Guide", href: "/user#purchases", excerpt: "Merchant logos, categories suggested from last time, payment methods, receipts and notes." },
  { title: "Refunds", type: "User Guide", href: "/user#refunds", excerpt: "Refund a purchase in full or in part; only what wasn't refunded counts." },
  { title: "Swipe Rows", type: "User Guide", href: "/user#refunds", excerpt: "Swipe a purchase right to refund it, left to delete it, with Undo." },
  { title: "Search & Filters", type: "User Guide", href: "/user#search", excerpt: "Dates, categories, amounts, payment methods, receipts and more." },
  { title: "Bulk Edit", type: "User Guide", href: "/user#search", excerpt: "Long-press to select purchases, then categorize or delete them together." },
  { title: "Charts", type: "User Guide", href: "/user#charts", excerpt: "Donut, pie, rings, bars or trend; tap to drill in, touch and hold to preview." },
  { title: "Scanning Receipts", type: "User Guide", href: "/user#scanning", excerpt: "Receipts, banking screenshots and pump screens, read on your phone." },
  { title: "Share to mi. Budget", type: "User Guide", href: "/user#scanning", excerpt: "Share a screenshot or photo from any app to scan it." },
  { title: "AI Scanners", type: "User Guide", href: "/user#scanning", excerpt: "Gemini Nano, on supported phones: reads the photo itself." },
  { title: "Calendar", type: "User Guide", href: "/user#calendar", excerpt: "Days shaded by spending, purchase counts and renewal marks." },
  { title: "Subscriptions", type: "User Guide", href: "/user#subscriptions", excerpt: "Monthly and yearly totals, coming up, and Active, Paused and Cancelled tabs." },
  { title: "Free Trials", type: "User Guide", href: "/user#free-trials", excerpt: "Nothing is billed during a trial, and you're warned before it ends." },
  { title: "Auto-log", type: "User Guide", href: "/user#subscriptions", excerpt: "Each billing date becomes a purchase, so budgets stay right." },
  { title: "Price History", type: "User Guide", href: "/user#subscriptions", excerpt: "Earlier prices and plans, with past payments at those prices." },
  { title: "Pause, Cancel, Resume", type: "User Guide", href: "/user#subscriptions", excerpt: "Stop and restart a subscription without touching the past." },
  { title: "Budgets", type: "User Guide", href: "/user#budgets", excerpt: "Weekly, monthly, quarterly or yearly budgets on any category." },
  { title: "Categories", type: "User Guide", href: "/user#budgets", excerpt: "Nested folders, as deep as you like." },
  { title: "Payment Methods", type: "User Guide", href: "/user#payment-methods", excerpt: "Cards, banks and wallets with their logos." },
  { title: "Check Splitter", type: "User Guide", href: "/user#check-splitter", excerpt: "Scan a bill and split it evenly, by amount or by item, with the tip." },
  { title: "Tip Calculator", type: "User Guide", href: "/user#tip-calculator", excerpt: "A percent slider with linked percent and dollar boxes." },
  { title: "Currency Converter", type: "User Guide", href: "/user#currency-converter", excerpt: "About 160 currencies, fully offline." },
  { title: "This Month Widget", type: "User Guide", href: "/user#widget", excerpt: "Spent so far, budget left and the next renewal on your home screen." },
  { title: "Recently Deleted", type: "User Guide", href: "/user#recently-deleted", excerpt: "Deleted purchases and subscriptions wait 30 days." },
  { title: "Backups", type: "User Guide", href: "/user#backups", excerpt: "Back up everything to a .mibudgetdata file and restore it on any phone." },
  { title: "Coming from Budgey", type: "User Guide", href: "/user#backups", excerpt: "Export in Budgey, then restore it in mi. Budget." },
  { title: "Settings Reference", type: "User Guide", href: "/user#customization", excerpt: "Every setting, section by section." },
  { title: "Privacy Policy", type: "User Guide", href: "/privacy", excerpt: "mi. Budget collects no personal data. What stays on your phone and every connection it makes." },
  { title: "Privacy Commitment", type: "User Guide", href: "/user#privacy", excerpt: "No accounts, no ads, no tracking." },

  // Dev Guide
  { title: "UI Reference", type: "Dev Guide", href: "/developer#ui-reference", excerpt: "Screenshots of the app's main screens." },
  { title: "Tech Stack", type: "Architecture", href: "/developer#tech-stack", excerpt: "Kotlin, Jetpack Compose, Material 3 Expressive, Room and ML Kit." },
  { title: "Key Decisions", type: "Architecture", href: "/developer#decisions", excerpt: "Money in cents, UUID ids, in-memory filtering, the export format." },
  { title: "Project Structure", type: "Architecture", href: "/developer#project-structure", excerpt: "How the code is organized." },
  { title: "Development Setup", type: "Dev Guide", href: "/developer#setup", excerpt: "Clone, fetch brand logos, run and test." },
  { title: "Making a Release", type: "Dev Guide", href: "/developer#release", excerpt: "Version, signing key, bundled data and the APK." },
];

// every setting, from the same list the User Guide shows
const MOCK_DATA: SearchResult[] = [
  ...GUIDE,
  ...SETTINGS.flatMap((g) => g.settings.map((s): SearchResult => ({ title: s.title, type: "Settings", href: `/user#setting-${slug(s.title)}`, excerpt: s.desc }))),
];

export function Search() {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchResult[]>([]);
  const [mounted, setMounted] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (query.trim() === "") {
      setResults([]);
      return;
    }
    const filtered = MOCK_DATA.filter(item =>
      item.title.toLowerCase().includes(query.toLowerCase()) ||
      item.excerpt.toLowerCase().includes(query.toLowerCase())
    );
    setResults(filtered);
  }, [query]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "k" && (e.metaKey || e.ctrlKey)) {
        e.preventDefault();
        setIsOpen(true);
      }
      if (e.key === "Escape") setIsOpen(false);
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  const handleResultClick = (e: React.MouseEvent, href: string) => {
    setIsOpen(false);

    // Check if we're navigating to an anchor on the current page
    const [path, hash] = href.split('#');
    const currentPath = window.location.pathname;
    const normalizedPath = currentPath.endsWith('/') ? currentPath.slice(0, -1) : currentPath;
    const targetPath = path.endsWith('/') ? path.slice(0, -1) : path;

    if (hash && (normalizedPath === targetPath || targetPath === "")) {
      e.preventDefault();
      const element = document.getElementById(hash);
      if (element) {
        // Use 'start' to ensure we see the top of the section
        element.scrollIntoView({ behavior: 'smooth', block: 'start' });

        // Add highlight effect
        element.classList.remove('highlight-section');
        // Trigger reflow
        void element.offsetWidth;
        element.classList.add('highlight-section');

        // Update URL without jump
        window.history.pushState(null, '', href);
      }
    }
  };

  const ModalContent = (
    <div className="fixed inset-0 z-[9999] bg-inverse-surface/40 backdrop-blur-md p-4 sm:p-4 flex items-start justify-center overflow-hidden">
      <div
        ref={searchRef}
        className="w-full max-w-2xl bg-surface-container-high rounded-4xl shadow-2xl border border-outline-variant overflow-hidden mt-12 sm:mt-20 flex flex-col max-h-[70vh] sm:max-h-[80vh]"
      >
        <div className="flex items-center p-4 sm:p-6 border-b border-outline-variant shrink-0">
          <SearchIcon className="text-on-surface-variant/80 mr-3 sm:mr-4" size={20} />
          <input
            autoFocus
            type="text"
            placeholder="Search..."
            className="flex-1 bg-transparent border-none outline-none text-lg sm:text-xl text-on-surface font-medium placeholder:text-on-surface-variant/60"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <button
            onClick={() => setIsOpen(false)}
            className="p-2 hover:bg-surface-container-high rounded-xl transition-colors ml-2"
          >
            <X size={20} className="text-on-surface-variant/80" />
          </button>
        </div>
        <div className="p-2 overflow-y-auto flex-1">
          {results.length > 0 ? (
            <div className="space-y-1">
              {results.map((result, idx) => (
                <Link
                  key={idx}
                  href={result.href}
                  onClick={(e) => handleResultClick(e, result.href)}
                  className="flex items-start gap-4 p-4 rounded-2xl hover:bg-surface-container-high transition-colors group"
                >
                  <div className="mt-1 p-2 rounded-lg bg-primary-container text-on-primary-container group-hover:bg-primary group-hover:text-on-primary transition-colors">
                    {result.type === "Settings" ? <Settings2 size={18} /> :
                      result.type === "Architecture" ? <Code2 size={18} /> :
                        result.title === "Privacy Commitment" ? <Shield size={18} /> :
                          result.type === "User Guide" ? <User size={18} /> : <FileText size={18} />}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-bold text-on-surface truncate">{result.title}</span>
                      <span className="shrink-0 text-[9px] font-bold uppercase tracking-widest text-on-surface-variant/70 px-1.5 py-0.5 rounded-md border border-outline-variant">
                        {result.type}
                      </span>
                    </div>
                    <p className="text-xs sm:text-sm text-on-surface-variant line-clamp-2">{result.excerpt}</p>
                  </div>
                </Link>
              ))}
            </div>
          ) : query ? (
            <div className="p-12 text-center text-on-surface-variant/60">
              <p className="text-lg font-medium">No results found for &quot;{query}&quot;</p>
            </div>
          ) : (
            <div className="p-12 text-center text-on-surface-variant/60">
              <p className="text-sm font-medium">Try searching for &quot;Refunds&quot; or &quot;Free Trials&quot;</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );

  return (
    <>
      <button
        onClick={() => setIsOpen(true)}
        className="flex items-center justify-center gap-1 sm:gap-2 px-2 sm:px-4 py-2 rounded-xl bg-surface-container-high border border-outline-variant text-on-surface-variant/80 hover:bg-surface-container-highest transition-all text-xs sm:text-sm font-semibold w-full sm:max-w-sm"
      >
        <SearchIcon size={14} className="sm:size-4" />
        <span className="text-center">Search</span>
        <kbd className="hidden sm:inline-flex h-5 select-none items-center gap-1 rounded border border-outline-variant bg-surface-container-high px-1.5 font-mono text-[10px] font-bold opacity-100">
          <span className="text-xs">⌘</span>K
        </kbd>
      </button>

      {isOpen && mounted && createPortal(ModalContent, document.body)}
    </>
  );
}