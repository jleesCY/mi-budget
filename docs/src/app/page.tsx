"use client";

import { motion } from "framer-motion";
import { Download, ChevronRight, ScanLine, PieChart, Shield, Sparkles, Wrench, Check } from "lucide-react";
import { Navbar } from "@/components/navbar";
import { Footer } from "@/components/footer";
import { DeviceMockup, type Glow } from "@/components/device-mockup";
import { GITHUB_URL, MI_APPS, getAssetPath } from "@/lib/site";

export default function Home() {
  return (
    <main className="min-h-screen bg-surface-container-low text-on-surface overflow-x-hidden transition-colors duration-300">
      <Navbar />

      {/* Hero Section */}
      <section className="pt-32 sm:pt-48 pb-16 sm:pb-20 px-6 relative">
        <div className="absolute top-20 left-1/2 -translate-x-1/2 w-full max-w-[600px] h-[300px] sm:h-[600px] bg-primary/10 rounded-full blur-[80px] sm:blur-[120px] -z-10" />

        <div className="max-w-6xl mx-auto grid lg:grid-cols-2 gap-12 sm:gap-16 items-center text-center lg:text-left">
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary-container text-primary text-[10px] sm:text-xs font-bold tracking-widest uppercase mb-6 border border-primary/20">
              <Sparkles size={14} />
              0.2 · For Android
            </div>
            <h1 className="text-4xl sm:text-6xl lg:text-7xl font-extrabold tracking-tighter mb-6 leading-[1.1]">
              Your money, <span className="text-primary italic">at a glance</span>.
            </h1>
            <p className="text-base sm:text-xl text-on-surface-variant mb-8 max-w-lg mx-auto lg:mx-0 leading-relaxed font-medium">
              mi. Budget shows you this month at a glance: what you&apos;ve spent, where it went and what&apos;s left of your budgets. Scan a receipt or type a purchase in, swipe between months, and keep an eye on subscriptions and free trials before they charge you. Everything stays on your phone: no accounts, no ads, no tracking.
            </p>
            <div className="flex flex-col sm:flex-row justify-center lg:justify-start gap-4">
              <a href="#download" className="flex items-center justify-center gap-2 bg-primary text-on-primary px-8 py-4 rounded-2xl font-bold text-lg hover:shadow-xl hover:shadow-primary/25 transition-all active:scale-95">
                <Download size={20} />
                Download APK
              </a>
              <a href={GITHUB_URL} className="flex items-center justify-center gap-2 bg-surface-container-high px-8 py-4 rounded-2xl font-bold text-lg hover:bg-surface-container-highest transition-all border border-outline-variant">
                View on GitHub
                <ChevronRight size={20} />
              </a>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.8, delay: 0.2 }}
            className="relative flex justify-center lg:justify-end mt-8 lg:mt-0"
          >
            <DeviceMockup src="/images/home.jpg" label="This Month" />
          </motion.div>
        </div>
      </section>

      {/* Quick Specs Section */}
      <section className="py-16 sm:py-24 border-y border-outline-variant bg-surface-container">
        <div className="max-w-6xl mx-auto px-6 grid sm:grid-cols-2 md:grid-cols-3 gap-8 sm:gap-12">
          <FeatureBrief icon={<PieChart size={24} />} title="This Month, First" desc="Spending, budgets and what's coming up, the moment you open it." />
          <FeatureBrief icon={<ScanLine size={24} />} title="Scan on Your Phone" desc="Receipts and screenshots read on-device, with optional on-device AI." />
          <FeatureBrief icon={<Shield size={24} />} title="Private by Design" desc="No accounts, no bank logins, no tracking. Your data never leaves your phone." />
        </div>
      </section>

      {/* Detailed Features: Alternating Sections */}
      <div className="space-y-24 sm:space-y-32 py-24 sm:py-32">

        <FeatureDetail
          image="/images/home.jpg" label="Main Bar"
          tag="Interaction"
          tagColor="text-primary bg-primary-container border-primary/20"
          title="One bar. Everything in reach."
          description="A big + adds a purchase. Beside it are the calendar and a menu for search, filters and the tools, with your Library at the left and Settings at the right. Swipe the bar and those pages follow your finger in. Open a purchase and the bar becomes its own buttons."
          bullets={[
            "Swipe right for the Library, left for Settings",
            "Swipe up for the Calendar",
            "Hold to search every purchase",
            "Swipe the month sideways to change it",
            "On a purchase: Delete, Refund and Edit; swipe right to go back",
          ]}
          imageSide="right"
          glow="primary"
        />

        <FeatureDetail
          image="/images/charts.jpg" label="Charts"
          tag="Insight"
          tagColor="text-tertiary bg-tertiary-container border-tertiary/20"
          title="Charts that read at any size."
          description="Donut, pie, rings, bars or trend. Small categories still get a proper segment, tapping a slice drills into it, and touching and holding previews it while you slide around the circle."
          bullets={[
            "Five chart styles, chosen once for the whole app",
            "Change from last month and a safe daily amount",
            "Drill down through nested categories",
            "Haptic ticks as you slide between slices",
          ]}
          imageSide="left"
          glow="tertiary"
        />

        <FeatureDetail
          image="/images/scan.jpg" label="Scanning"
          tag="Scanning"
          tagColor="text-secondary bg-secondary-container border-secondary/20"
          title="Scan it. Check it. Done."
          description="Snap a receipt, or share a banking screenshot or a pump screen from any app. mi. Budget reads the merchant, total and date on your phone, tells a purchase from a subscription, and opens an editor ready to check."
          bullets={[
            "Receipts, banking screenshots and pump screens",
            "Merchant logos for about 950 brands",
            "On-device AI with Gemini Nano, on supported phones",
            "Photos kept small: about 100 to 250 KB each",
          ]}
          imageSide="right"
          glow="secondary"
        />

        <FeatureDetail
          image="/images/subscriptions.jpg" label="Subscriptions"
          tag="Subscriptions"
          tagColor="text-primary bg-primary-container border-primary/20"
          title="No more surprise charges."
          description="See what your subscriptions cost each month and year, what renews next, and which free trials are about to start charging. Reminders arrive before the money goes, and every payment can log itself."
          bullets={[
            "Renewal reminders and free-trial warnings",
            "Payments logged automatically",
            "Price history, pause, cancel and resume",
            "Weekly to yearly, or every N days",
          ]}
          imageSide="left"
          glow="primary"
        />

        <FeatureDetail
          image="/images/budgets.jpg" label="Budgets"
          tag="Budgets"
          tagColor="text-tertiary bg-tertiary-container border-tertiary/20"
          title="Budgets that roll up."
          description="Categories are folders you can nest as deep as you like. Give any of them a weekly, monthly, quarterly or yearly budget, and a parent's budget counts everything inside it."
          bullets={[
            "19 categories to start with, 55 with sub-categories",
            "Ahead of pace and over budget, said in words",
            "Refunds only count what you kept",
            "Deleting a folder moves its contents up",
          ]}
          imageSide="right"
          glow="tertiary"
        />

        <Tools />
      </div>

      {/* Download CTA */}
      <section id="download" className="py-20 sm:py-40 bg-primary-container text-on-primary-container rounded-[2rem] sm:rounded-[4rem] mx-4 mb-20 relative overflow-hidden">
        <div className="absolute top-0 left-0 w-full h-full opacity-40 pointer-events-none">
          <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-primary rounded-full blur-[120px]" />
          <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-tertiary rounded-full blur-[120px]" />
        </div>
        <div className="max-w-3xl mx-auto px-6 text-center relative z-10">
          <h2 className="text-4xl sm:text-6xl font-extrabold mb-8 tracking-tighter">Ready to see where it goes?</h2>
          <p className="text-lg sm:text-xl opacity-70 mb-12 font-medium">
            Download the latest APK for Android. Updates come from the app itself, and your data stays.
          </p>
          <div className="flex flex-col items-center gap-6">
            <a href={`${GITHUB_URL}/releases/latest`} className="group flex items-center gap-4 bg-primary text-on-primary px-8 py-4 sm:px-12 sm:py-6 rounded-2xl sm:rounded-3xl font-black text-xl sm:text-2xl hover:scale-105 active:scale-95 transition-all shadow-2xl shadow-primary/30">
              <Download size={28} />
              Install Latest APK
            </a>
            <span className="text-sm opacity-40 font-bold tracking-widest uppercase">Requires Android 8.0+</span>
          </div>
        </div>
      </section>

      <MoreApps />

      <Footer />
    </main>
  );
}

/** The other mi. apps: each a card with its icon (a copy in public/apps), name and one line, linking to its own site. */
function MoreApps() {
  return (
    <section className="max-w-6xl mx-auto px-6 pb-24">
      <h3 className="text-2xl sm:text-3xl font-extrabold tracking-tight mb-8 text-center sm:text-left">More mi. Apps</h3>
      <div className="grid sm:grid-cols-2 gap-4">
        {MI_APPS.map((app) => (
          <a key={app.name} href={app.site} target="_blank" rel="noopener noreferrer"
            className="flex items-center gap-4 p-5 rounded-3xl bg-surface-container hover:bg-surface-container-high transition-colors">
            <img src={getAssetPath(app.icon)} alt="" className="w-14 h-14 rounded-2xl" />
            <div className="min-w-0">
              <p className="font-bold text-lg">{app.name}</p>
              <p className="text-sm text-on-surface-variant">{app.pitch}</p>
            </div>
            <ChevronRight size={20} className="ml-auto text-on-surface-variant shrink-0" />
          </a>
        ))}
      </div>
    </section>
  );
}

/** The everyday extras: the Tools page and the bits around it. */
function Tools() {
  const tools = ["Check Splitter", "Tip Calculator", "Currency Converter", "This Month Widget", "Refunds", "Recently Deleted", "Backups You Keep"];
  return (
    <section className="max-w-6xl mx-auto px-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }} transition={{ duration: 0.6 }}
        className="rounded-[2rem] sm:rounded-[3rem] bg-surface-container p-8 sm:p-14 grid lg:grid-cols-2 gap-10 items-center"
      >
        <div className="space-y-6 text-center lg:text-left">
          <span className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full text-[10px] sm:text-xs font-bold tracking-[0.2em] uppercase bg-tertiary-container text-on-tertiary-container">
            <Wrench size={14} /> Tools
          </span>
          <h3 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold tracking-tighter leading-tight">The little things, handled.</h3>
          <p className="text-base sm:text-xl text-on-surface-variant leading-relaxed font-medium">
            Scan a restaurant bill and split it by item, work out the tip, or convert a price in about 160 currencies without a connection. Refund what you return, undo what you delete, and keep your backups as files of your own.
          </p>
        </div>
        <div className="flex flex-wrap gap-3 justify-center lg:justify-start">
          {tools.map((name) => (
            <span key={name} className="inline-flex items-center gap-2 pl-3 pr-4 py-2.5 rounded-full bg-surface-container-highest text-on-surface font-bold text-sm sm:text-base">
              <Check size={16} className="text-primary" /> {name}
            </span>
          ))}
        </div>
      </motion.div>
    </section>
  );
}

function FeatureBrief({ icon, title, desc }: { icon: React.ReactNode; title: string; desc: string }) {
  return (
    <div className="space-y-3">
      <div className="bg-primary-container text-on-primary-container w-12 h-12 flex items-center justify-center rounded-full mb-4">
        {icon}
      </div>
      <h4 className="font-bold text-lg tracking-tight">{title}</h4>
      <p className="text-on-surface-variant leading-relaxed font-medium">{desc}</p>
    </div>
  );
}

const DOT = { primary: "bg-primary", secondary: "bg-secondary", tertiary: "bg-tertiary" };

function FeatureDetail({ image, label, tag, tagColor, title, description, bullets, imageSide, glow }: {
  image: string; label: string; tag: string; tagColor: string; title: string; description: string; bullets: string[];
  imageSide: "left" | "right"; glow: Glow;
}) {
  return (
    <section className="max-w-6xl mx-auto px-6">
      <div className={`flex flex-col ${imageSide === "right" ? "lg:flex-row" : "lg:flex-row-reverse"} items-center gap-12 lg:gap-20`}>
        <motion.div
          initial={{ opacity: 0, x: imageSide === "right" ? -30 : 30 }}
          whileInView={{ opacity: 1, x: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8 }}
          className="flex-1 space-y-8 text-center lg:text-left"
        >
          <div className="space-y-4">
            <span className={`inline-block px-4 py-1.5 rounded-full text-[10px] sm:text-xs font-bold tracking-[0.2em] uppercase border ${tagColor}`}>
              {tag}
            </span>
            <h3 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold tracking-tighter leading-tight">{title}</h3>
            <p className="text-base sm:text-xl text-on-surface-variant leading-relaxed font-medium">{description}</p>
          </div>
          <ul className="space-y-4 inline-block lg:block text-left">
            {bullets.map((b) => (
              <li key={b} className="flex items-center gap-3 font-bold text-on-surface text-sm sm:text-base">
                <div className={`w-2 h-2 rounded-full shrink-0 ${DOT[glow]}`} />
                {b}
              </li>
            ))}
          </ul>
        </motion.div>
        <div className="flex-1 flex justify-center mt-8 lg:mt-0">
          <DeviceMockup src={image} label={label} color={glow} />
        </div>
      </div>
    </section>
  );
}
