"use client";

import { Footer } from "@/components/footer";
import { Navbar } from "@/components/navbar";
import { DeviceMockup } from "@/components/device-mockup";
import { useHighlight } from "@/hooks/useHighlight";
import { SETTINGS } from "@/lib/settings";
import { slug } from "@/lib/site";
import { motion } from "framer-motion";
import {
  ArrowLeftRight, Bell, Bot, Calculator, CalendarDays, ChevronRight, CircleDollarSign, CreditCard, FileUp, Filter,
  FolderTree, Hand, History, Landmark, LayoutGrid, Lock, MousePointer2, Navigation, PieChart, Plus, Receipt,
  RefreshCw, Repeat, RotateCcw, ScanLine, Search, Settings2, Share2, ShieldCheck, Smartphone, SplitSquareHorizontal,
  Tag, Timer, Trash2, Undo2, Wallet, Wrench,
} from "lucide-react";

const fadeInUp = {
  initial: { opacity: 0, y: 20 },
  whileInView: { opacity: 1, y: 0 },
  viewport: { once: true },
  transition: { duration: 0.6 },
};

export default function UserDocs() {
  useHighlight();

  return (
    <main className="min-h-screen bg-surface-container-low text-on-surface transition-colors duration-300 overflow-x-hidden">
      <Navbar />

      <div className="pt-24 sm:pt-32 pb-20 px-6">
        <div className="max-w-4xl mx-auto">
          <motion.div {...fadeInUp} className="mb-16 text-center sm:text-left">
            <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tighter mb-6">
              The Complete <span className="text-primary italic">User Guide</span>
            </h1>
            <p className="text-lg sm:text-xl text-on-surface-variant leading-relaxed font-medium">
              Every gesture, tool and setting in mi. Budget.
            </p>
          </motion.div>

          <section className="space-y-24 sm:space-y-32">

            {/* 1. The main bar - IMAGE RIGHT */}
            <motion.div id="the-bar" {...fadeInUp} className="space-y-12">
              <SectionTitle icon={<Navigation size={24} />} title="The Main Bar" tone="primary" />
              <div className="grid lg:grid-cols-2 gap-12 items-center text-center sm:text-left">
                <div className="space-y-6">
                  <p className="text-base sm:text-lg text-on-surface-variant leading-relaxed font-medium">
                    mi. Budget opens on this month: its chart, the total you&apos;ve spent and your purchases. Everything else is in one bar at the bottom of the screen, made for your thumb.
                  </p>
                  <div className="space-y-4 inline-block text-left w-full">
                    <GuideItem icon={<Plus size={18} />} title="The + Button" desc="Adds a purchase: choose Scan to read a receipt, or Add Manually to type it in." />
                    <GuideItem icon={<CalendarDays size={18} />} title="Calendar" desc="The button left of + opens the calendar." />
                    <GuideItem icon={<LayoutGrid size={18} />} title="The ⋮ Menu" desc="Search and filters, the tools (the check splitter, currency converter and tip calculator), and links to your renewals and payment methods." />
                    <GuideItem icon={<Wallet size={18} />} title="Library and Settings" desc="The buttons at either end: your Library (subscriptions, budgets and tools) at the left, Settings at the right." />
                  </div>

                  <div id="gestures" className="pt-4 space-y-4 border-t border-outline-variant text-left">
                    <h4 className="text-xs font-bold uppercase tracking-[0.2em] text-primary mb-4">Gestures</h4>
                    <GuideItem icon={<ChevronRight size={18} />} title="Swipe Right" desc="The Library slides in, following your finger." />
                    <GuideItem icon={<ChevronRight size={18} className="rotate-180" />} title="Swipe Left" desc="Settings slides in." />
                    <GuideItem icon={<ChevronRight size={18} className="-rotate-90" />} title="Swipe Up" desc="The Calendar rises from the bar." />
                    <GuideItem icon={<Hand size={18} />} title="Hold" desc="Search every purchase. Each page's own search Pill grows out of the bar's Pill, and swiping it back the way the page came returns you." />
                    <GuideItem icon={<ArrowLeftRight size={18} />} title="Change Month" desc="Swipe the month title or the chart sideways." />
                  </div>

                  <div className="p-4 rounded-xl border border-outline-variant bg-surface-container-high text-[10px] sm:text-xs text-on-surface-variant text-left">
                    <p><strong>Pro Tip:</strong> Forgot a swipe? Settings › About › Gesture Walkthrough takes you through each one, hands-on.</p>
                  </div>
                </div>
                <DeviceMockup src="/images/home.jpg" label="This Month" />
              </div>
            </motion.div>

            {/* 2. Purchases - IMAGE LEFT */}
            <motion.div id="purchases" {...fadeInUp} className="space-y-12">
              <SectionTitle icon={<Receipt size={24} />} title="Purchases" tone="secondary" />
              <div className="grid lg:grid-cols-2 gap-12 items-center text-center sm:text-left">
                <div className="order-2 lg:order-1">
                  <DeviceMockup src="/images/add.jpg" label="Add Purchase" color="secondary" />
                </div>
                <div className="space-y-6 order-1 lg:order-2">
                  <p className="text-base sm:text-lg text-on-surface-variant leading-relaxed font-medium">
                    Type the merchant and mi. Budget fills in the rest it can: the brand&apos;s logo, the category you used there last time, and the payment method you paid with.
                  </p>
                  <div className="space-y-4 inline-block text-left w-full">
                    <GuideItem icon={<Tag size={18} />} title="Logos and Icons" desc="About 950 brands are recognised as you type, even from bank-statement names like SQ *STARBUCKS. Tap the icon to choose a logo, up to 3 letters on a colour, a photo, or the category's icon." />
                    <GuideItem icon={<Receipt size={18} />} title="Receipt and Notes" desc="Attach a photo of the receipt and add a note. mi. asks before saving a purchase without a category." />
                    <GuideItem icon={<MousePointer2 size={18} />} title="A Purchase's Page" desc="Tap a purchase to see it. The bar becomes its buttons: Delete, Refund and Edit (Delete and Save while editing). Swipe the bar right to go back, with the page following your finger. Subscriptions work the same way, with Delete and Edit." />
                    <div id="refunds">
                      <GuideItem icon={<RotateCcw size={18} />} title="Refunds" desc="Refund a purchase in full or in part from its page, or swipe it right in the list. It stays in your history, crossed out, and only what wasn't refunded counts toward budgets and charts." />
                    </div>
                    <GuideItem icon={<Undo2 size={18} />} title="Swipe to Delete" desc="Swipe a purchase left, or tap the bin on its page, to delete it. It goes at once, with Undo to bring it straight back, and it waits in Recently Deleted too." />
                    <div id="search">
                      <GuideItem icon={<Filter size={18} />} title="Search and Filters" desc="Search from the Pill, or filter by date (presets or your own range), category, uncategorized only, amount, expenses or refunds, where it came from (typed, scanned, subscription, imported), payment method and receipts, in any order." />
                    </div>
                    <GuideItem icon={<MousePointer2 size={18} />} title="Bulk Edit" desc="Long-press a purchase to select several, then give them a category or delete them together, with Undo." />
                  </div>
                </div>
              </div>
            </motion.div>

            {/* 3. Charts - IMAGE RIGHT */}
            <motion.div id="charts" {...fadeInUp} className="space-y-12">
              <SectionTitle icon={<PieChart size={24} />} title="Charts" tone="tertiary" />
              <div className="grid lg:grid-cols-2 gap-12 items-center text-center sm:text-left">
                <div className="space-y-6">
                  <p className="text-base sm:text-lg text-on-surface-variant leading-relaxed font-medium">
                    The chart at the top of the start screen shows where this month went. Under it: the total, the change from the same point last month, and how much of your budgets is left, with a safe amount to spend each day.
                  </p>
                  <div className="space-y-4 inline-block text-left w-full">
                    <GuideItem icon={<PieChart size={18} />} title="Five Styles" desc="Donut, Pie, Rings, Bars or Trend. Tap the style button on the chart; your choice applies everywhere." />
                    <GuideItem icon={<FolderTree size={18} />} title="Drill Down" desc="Tap a slice to see inside that category, and ↑ to go back up." />
                    <GuideItem icon={<Hand size={18} />} title="Touch and Hold" desc="On the donut, pie and rings, hold a slice to preview it, then slide around the circle to preview the others. A light tick marks each change." />
                    <GuideItem icon={<LayoutGrid size={18} />} title="Shortcuts" desc="Cards for Upcoming Renewals, Budgets and the Last 7 Days sit under the chart, with reminders for free trials and purchases that need a category." />
                  </div>
                </div>
                <DeviceMockup src="/images/charts.jpg" label="Charts" color="tertiary" />
              </div>
            </motion.div>

            {/* 4. Scanning - IMAGE LEFT */}
            <motion.div id="scanning" {...fadeInUp} className="space-y-12">
              <SectionTitle icon={<ScanLine size={24} />} title="Scanning" tone="primary" />
              <div className="grid lg:grid-cols-2 gap-12 items-center text-center sm:text-left">
                <div className="order-2 lg:order-1">
                  <DeviceMockup src="/images/scan.jpg" label="Scan Review" />
                </div>
                <div className="space-y-6 order-1 lg:order-2">
                  <p className="text-base sm:text-lg text-on-surface-variant leading-relaxed font-medium">
                    Scan opens Android&apos;s chooser: take a photo, or pick one from Photos or Files. Everything is read on your phone, and you always check the result before it&apos;s saved.
                  </p>
                  <div className="space-y-4 inline-block text-left w-full">
                    <GuideItem icon={<Receipt size={18} />} title="What It Reads" desc="Receipts, banking-app screenshots and fuel pump or kiosk screens. It finds the merchant, total and date, and whether it's a purchase or a subscription (with its billing cycle and trial)." />
                    <GuideItem icon={<Share2 size={18} />} title="Share to mi. Budget" desc="From any app, share a screenshot or photo to mi. Budget to scan it." />
                    <GuideItem icon={<Bot size={18} />} title="AI Scanners" desc="Standard (Google's ML Kit) is built in. On supported phones, Settings › Scanner also offers Gemini Nano, which looks at the photo itself. After a scan, Rescan reads the photo again and Advanced Rescan reads it with Gemini Nano." />
                    <GuideItem icon={<CircleDollarSign size={18} />} title="Free Receipts" desc="A fully discounted receipt reads as $0.00, and $0 purchases can be saved." />
                  </div>
                  <div className="p-4 rounded-xl border border-outline-variant bg-surface-container-high text-[10px] sm:text-xs text-on-surface-variant text-left">
                    <p><strong>Small photos:</strong> attached receipts are resized and saved as WebP, about 100 to 250 KB instead of 3 to 5 MB.</p>
                  </div>
                </div>
              </div>
            </motion.div>

            {/* 5. Calendar - IMAGE RIGHT */}
            <motion.div id="calendar" {...fadeInUp} className="space-y-12">
              <SectionTitle icon={<CalendarDays size={24} />} title="Calendar" tone="secondary" />
              <div className="grid lg:grid-cols-2 gap-12 items-center text-center sm:text-left">
                <div className="space-y-6">
                  <p className="text-base sm:text-lg text-on-surface-variant leading-relaxed font-medium">
                    Swipe the bar up for the month as a grid. Swipe sideways or use the arrows to change month; Today brings you back.
                  </p>
                  <div className="space-y-4 inline-block text-left w-full">
                    <GuideItem icon={<LayoutGrid size={18} />} title="Each Day" desc="Shaded by how much you spent, with the day's total, a count of its purchases and a ↻ mark on renewal days." />
                    <GuideItem icon={<MousePointer2 size={18} />} title="Tap a Day" desc="See its purchases and renewals, or add a purchase on that date." />
                  </div>
                </div>
                <DeviceMockup src="/images/calendar.jpg" label="Calendar" color="secondary" />
              </div>
            </motion.div>

            {/* 6. Subscriptions - IMAGE LEFT */}
            <motion.div id="subscriptions" {...fadeInUp} className="space-y-12">
              <SectionTitle icon={<Repeat size={24} />} title="Subscriptions" tone="tertiary" />
              <div className="grid lg:grid-cols-2 gap-12 items-center text-center sm:text-left">
                <div className="order-2 lg:order-1">
                  <DeviceMockup src="/images/subscriptions.jpg" label="Subscriptions" color="tertiary" />
                </div>
                <div className="space-y-6 order-1 lg:order-2">
                  <p className="text-base sm:text-lg text-on-surface-variant leading-relaxed font-medium">
                    In the Library: your monthly and yearly totals, a Coming Up row, and Active, Paused, Cancelled and All tabs.
                  </p>
                  <div className="space-y-4 inline-block text-left w-full">
                    <GuideItem icon={<Timer size={18} />} title="Any Cycle" desc="Weekly to yearly, or every N days, weeks, months or years. The next payment is worked out from the start date for you." />
                    <GuideItem icon={<History size={18} />} title="Auto-log" desc="Each billing date becomes a purchase, so budgets stay right. A start date in the past fills in the payments since then." />
                    <GuideItem icon={<CircleDollarSign size={18} />} title="Price, Fees and Taxes" desc="Base price plus fees and taxes makes the total charged. Change any one and the others follow." />
                    <GuideItem icon={<RefreshCw size={18} />} title="Price History" desc="Add earlier periods with their own price and cycle (a student plan, say), and past payments use those prices. After a price change, choose Only Future to keep the past as it was." />
                    <GuideItem icon={<RotateCcw size={18} />} title="Pause, Cancel, Resume" desc="Stopping records the date. Resume On a Date restarts billing without rewriting what came before." />
                    <GuideItem icon={<Bell size={18} />} title="Reminders" desc="A notification before each renewal, around 9 AM. Choose how many days ahead in Settings, or per subscription." />
                    <div id="free-trials">
                      <GuideItem icon={<ShieldCheck size={18} />} title="Free Trials" desc="Nothing is billed, shown or logged during a trial, and paid billing starts on the day it ends. You're reminded at least 2 days before. When a trial ends, mi. asks: Keep It, or Cancel It." />
                    </div>
                  </div>
                </div>
              </div>
            </motion.div>

            {/* 7. Budgets & categories - IMAGE RIGHT */}
            <motion.div id="budgets" {...fadeInUp} className="space-y-12">
              <SectionTitle icon={<FolderTree size={24} />} title="Budgets & Categories" tone="primary" />
              <div className="grid lg:grid-cols-2 gap-12 items-center text-center sm:text-left">
                <div className="space-y-6">
                  <p className="text-base sm:text-lg text-on-surface-variant leading-relaxed font-medium">
                    Categories are folders, as deep as you like. Each one shows its spending, its budget, the folders inside it, its subscriptions and recent purchases.
                  </p>
                  <div className="space-y-4 inline-block text-left w-full">
                    <GuideItem icon={<Wallet size={18} />} title="Budgets" desc="Give any category a weekly, monthly, quarterly or yearly budget. A parent's budget counts everything inside it. Budgets says which are ahead of pace and which are over." />
                    <GuideItem icon={<FolderTree size={18} />} title="Defaults" desc="19 categories to start with (55 with sub-categories). Add From Defaults brings back any you deleted." />
                    <GuideItem icon={<Trash2 size={18} />} title="Nothing Lost" desc="Deleting a folder moves what was in it up a level." />
                  </div>
                </div>
                <DeviceMockup src="/images/budgets.jpg" label="Budgets" />
              </div>
            </motion.div>

            {/* 8. Payment methods */}
            <motion.div id="payment-methods" {...fadeInUp} className="space-y-12">
              <SectionTitle icon={<CreditCard size={24} />} title="Payment Methods" tone="secondary" />
              <div className="space-y-6">
                <p className="text-base sm:text-lg text-on-surface-variant leading-relaxed font-medium text-center sm:text-left">
                  Save your cards, bank accounts and wallets once in Settings › Budgeting › Payment Methods, then pick them on any purchase or subscription.
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-left">
                  <GuideItem icon={<Landmark size={18} />} title="Logos" desc="84 bank, card and wallet logos, picked from the name. Or choose a symbol, letters or your own photo." />
                  <GuideItem icon={<CreditCard size={18} />} title="Remembered" desc="A new purchase starts with the method you last used at that merchant. Filter purchases by method and see how much each is used." />
                </div>
              </div>
            </motion.div>

            {/* 9. Tools */}
            <motion.div id="tools" {...fadeInUp} className="space-y-12">
              <SectionTitle icon={<Wrench size={24} />} title="Tools" tone="tertiary" />
              <div className="space-y-6">
                <p className="text-base sm:text-lg text-on-surface-variant leading-relaxed font-medium text-center sm:text-left">
                  In the ⋮ menu and the Library.
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 text-left">
                  <div id="check-splitter" className="sm:col-span-2">
                    <GuideItem icon={<SplitSquareHorizontal size={18} />} title="Check Splitter" desc="Scan the bill to pull in every item, add people, and split evenly, by custom amounts or percentages, or by item (tap who had what; shared items split evenly). Rows like '5 Burgers' become one item each. Each person's Tip switch says who chips in for the tip, and every share rounds up to the cent. Share the result as text. Leave and come back to Continue where you were." />
                  </div>
                  <div id="tip-calculator">
                    <GuideItem icon={<Calculator size={18} />} title="Tip Calculator" desc="The bill, a 1 to 50% slider with shortcuts, and percent and dollar boxes that follow each other. Tip After Tax adds a tax box." />
                  </div>
                  <div id="currency-converter">
                    <GuideItem icon={<ArrowLeftRight size={18} />} title="Currency Converter" desc="About 160 currencies, built in, so it works with no connection. Add Daily Rate Updates in Settings › Tools to keep them current." />
                  </div>
                  <div id="widget">
                    <GuideItem icon={<Smartphone size={18} />} title="This Month Widget" desc="On your home screen: spent so far, what's left of your monthly budgets and the next renewal." />
                  </div>
                  <div id="recently-deleted">
                    <GuideItem icon={<Trash2 size={18} />} title="Recently Deleted" desc="Deleted purchases and subscriptions wait here for 30 days (or the time you choose) before they're gone. Restore any of them." />
                  </div>
                  <div id="backups" className="sm:col-span-2">
                    <GuideItem icon={<FileUp size={18} />} title="Backups" desc="Settings › Backup & Reset › Back Up saves everything, receipt photos included, to a .mibudgetdata file you keep wherever you like. Restore it on any phone. Coming from Budgey? Export in Budgey (Settings › Your Data › Export), then restore it here." />
                  </div>
                </div>
              </div>
            </motion.div>

            {/* 10. Settings */}
            <motion.div id="customization" {...fadeInUp} className="space-y-12">
              <SectionTitle icon={<Settings2 size={24} />} title="Settings" tone="secondary" />
              <div className="grid lg:grid-cols-2 gap-12 items-center text-center sm:text-left">
                <div className="space-y-6">
                  <p className="text-lg text-on-surface-variant leading-relaxed font-medium">
                    Swipe the bar left for Settings. It opens on a list of sections; tap one to open its page, or search from the Pill to find any setting from any section. Here is every setting:
                  </p>
                  <div className="p-4 rounded-xl border border-outline-variant bg-surface-container-high text-[10px] sm:text-xs text-on-surface-variant text-left">
                    <p><strong>Pro Tip:</strong> Settings apply as you change them. Pick another accent and the whole colour scheme follows straight away.</p>
                  </div>
                </div>
                <DeviceMockup src="/images/settings.jpg" label="Settings" color="secondary" />
              </div>

              <div className="grid gap-12">
                {SETTINGS.map((group) => (
                  <SettingsSection key={group.title} title={group.title}>
                    {group.settings.map((s) => <SettingItem key={s.title} title={s.title} desc={s.desc} />)}
                  </SettingsSection>
                ))}
              </div>
            </motion.div>

            {/* 11. Privacy Commitment */}
            <motion.div id="privacy" {...fadeInUp} className="bg-primary-container rounded-[2rem] p-12 text-on-primary-container relative overflow-hidden">
              <div className="absolute top-0 right-0 p-12 opacity-10">
                <ShieldCheck size={200} />
              </div>
              <div className="relative z-10 max-w-2xl">
                <h2 className="text-3xl font-extrabold mb-6 tracking-tight">Privacy by Design</h2>
                <p className="text-xl text-on-primary-container font-medium leading-relaxed mb-8">
                  Your finances are yours. mi. Budget never asks for a bank login, an account or an email.
                </p>
                <div className="grid sm:grid-cols-2 gap-8">
                  <div className="space-y-2">
                    <div className="flex items-center gap-2 font-bold">
                      <Lock size={18} /> No Tracking
                    </div>
                    <p className="text-sm text-on-primary-container/80">No analytics, no ads, no crash reporters, and no servers of our own. Receipts are read on your phone.</p>
                  </div>
                  <div className="space-y-2">
                    <div className="flex items-center gap-2 font-bold">
                      <Search size={18} /> On Your Phone
                    </div>
                    <p className="text-sm text-on-primary-container/80">Purchases, subscriptions, budgets and receipt photos stay in the app&apos;s private storage. They leave only in a backup you export.</p>
                  </div>
                </div>
              </div>
            </motion.div>
          </section>
        </div>
      </div>

      <Footer />
    </main>
  );
}

const TONE = {
  primary: "bg-primary-container text-on-primary-container",
  secondary: "bg-secondary-container text-on-secondary-container",
  tertiary: "bg-tertiary-container text-on-tertiary-container",
};

function SectionTitle({ icon, title, tone }: { icon: React.ReactNode; title: string; tone: keyof typeof TONE }) {
  return (
    <div className="flex items-center justify-center sm:justify-start gap-3">
      <div className={`p-2 rounded-xl ${TONE[tone]}`}>{icon}</div>
      <h2 className="text-2xl sm:text-3xl font-bold tracking-tight">{title}</h2>
    </div>
  );
}

function GuideItem({ icon, title, desc }: { icon: React.ReactNode; title: string; desc: string }) {
  return (
    <div className="flex gap-4">
      <div className="shrink-0 w-10 h-10 rounded-xl bg-surface-container-high border border-outline-variant flex items-center justify-center text-on-surface-variant/80">
        {icon}
      </div>
      <div>
        <h4 className="font-bold text-lg leading-tight mb-1">{title}</h4>
        <p className="text-on-surface-variant font-medium leading-relaxed">{desc}</p>
      </div>
    </div>
  );
}

function SettingsSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div id={`settings-${slug(title)}`} className="space-y-6 scroll-mt-28">
      <div className="flex items-center gap-2 pb-2 border-b border-outline-variant">
        <Settings2 size={20} className="text-primary" />
        <h3 className="font-bold uppercase text-xs tracking-[0.2em] opacity-50">{title}</h3>
      </div>
      <div className="grid sm:grid-cols-2 gap-6">{children}</div>
    </div>
  );
}

function SettingItem({ title, desc }: { title: string; desc: string }) {
  return (
    <div id={`setting-${slug(title)}`} className="p-6 rounded-2xl bg-surface-container-high border border-outline-variant shadow-sm hover:border-primary/40 transition-colors scroll-mt-28">
      <h4 className="font-bold text-sm mb-2">{title}</h4>
      <p className="text-xs text-on-surface-variant leading-relaxed font-medium">{desc}</p>
    </div>
  );
}
