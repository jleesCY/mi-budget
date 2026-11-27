"use client";

import { motion } from "framer-motion";
import { Navbar } from "@/components/navbar";
import { Footer } from "@/components/footer";
import { Screen } from "@/components/device-mockup";
import { Terminal, Cpu, Layout, FolderTree, Info, Lightbulb, Rocket } from "lucide-react";
import { useHighlight } from "@/hooks/useHighlight";

const PROJECT_STRUCTURE = `
/
├── app/
│   └── src/main/
│       ├── java/com/jlees/mibudget/
│       │   ├── MainActivity.kt      # Edge-to-edge host, share intents, permissions
│       │   ├── MiApp.kt             # App start, launch storage sweep
│       │   ├── AppContainer.kt      # Manual dependency injection
│       │   ├── data/                # Room entities & DAOs, repository, settings (JSON), receipts, backup/
│       │   ├── domain/              # Money, periods & billing cycles, category tree, filters, budget math (pure Kotlin)
│       │   ├── icons/               # Brand catalog + matcher
│       │   ├── scan/                # OCR (ML Kit), receipt parser, Gemini Nano
│       │   ├── reminders/           # Renewal reminders (JobScheduler) + notifications
│       │   ├── update/              # Release checks, in-app updates, What's New
│       │   ├── widget/              # This Month home-screen widget
│       │   └── ui/                  # common/ (Pill, sheets, main bar, motion), components/ (charts, pickers),
│       │                            # settings/ (one file per section), and one package per screen
│       ├── assets/                  # brands.json, brand_icons.json, fx_rates.json, fonts
│       └── res/                     # Launcher icons, widget layout
├── docs/                    # This website (Next.js; colours from scripts/theme.mjs), DEVELOPMENT.md, releases/
└── CHANGELOG.md
`.trim();

const fadeInUp = {
  initial: { opacity: 0, y: 20 },
  whileInView: { opacity: 1, y: 0 },
  viewport: { once: true },
  transition: { duration: 0.6 },
};

export default function DeveloperDocs() {
  useHighlight();

  return (
    <main className="min-h-screen bg-surface-container-low text-on-surface transition-colors duration-300 overflow-x-hidden">
      <Navbar />

      <div className="pt-24 sm:pt-32 pb-20 px-6">
        <div className="max-w-4xl mx-auto">
          <motion.div {...fadeInUp} className="mb-16 text-center sm:text-left">
            <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tighter mb-6">
              Developer <span className="text-primary italic">Guide</span>
            </h1>
            <p className="text-lg sm:text-xl text-on-surface-variant leading-relaxed font-medium">
              Technical architecture and development setup for the mi. Budget open-source project. The full notes live in{" "}
              <a className="text-primary hover:underline" href="https://github.com/mi-app-hub/mi-budget/blob/master/docs/DEVELOPMENT.md">docs/DEVELOPMENT.md</a>.
            </p>
          </motion.div>

          <section className="space-y-20">
            {/* UI Reference */}
            <motion.div id="ui-reference" {...fadeInUp} className="space-y-8">
              <SectionTitle icon={<Layout size={24} />} title="UI Reference" tone="bg-primary-container text-on-primary-container" />
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
                {[
                  { name: "This Month", src: "/images/home.jpg" },
                  { name: "Charts", src: "/images/charts.jpg" },
                  { name: "Add Purchase", src: "/images/add.jpg" },
                  { name: "Scan Review", src: "/images/scan.jpg" },
                  { name: "Calendar", src: "/images/calendar.jpg" },
                  { name: "Subscriptions", src: "/images/subscriptions.jpg" },
                  { name: "Budgets", src: "/images/budgets.jpg" },
                  { name: "Check Splitter", src: "/images/splitter.jpg" },
                  { name: "Settings", src: "/images/settings.jpg" },
                ].map((img) => (
                  <div key={img.name} className="space-y-2 group">
                    <div className="bg-black rounded-2xl overflow-hidden border border-outline-variant shadow-lg group-hover:scale-[1.02] transition-transform">
                      <Screen src={img.src} label={img.name} />
                    </div>
                    <p className="text-center text-[10px] font-bold text-on-surface-variant/80 uppercase tracking-widest">{img.name}</p>
                  </div>
                ))}
              </div>
            </motion.div>

            {/* Tech Stack */}
            <motion.div id="tech-stack" {...fadeInUp} className="space-y-8">
              <SectionTitle icon={<Cpu size={24} />} title="Tech Stack" tone="bg-tertiary-container text-on-tertiary-container" />
              <div className="grid sm:grid-cols-2 gap-6">
                <TechCard title="Core Framework" items={["Kotlin", "Jetpack Compose", "Single Activity + ViewModels", "Manual DI (AppContainer)"]} dot="bg-primary" />
                <TechCard title="Design" items={["Material 3 Expressive", "MaterialKolor (seed color schemes)", "Google Sans Flex", "Material Symbols Rounded"]} dot="bg-tertiary" />
                <TechCard title="Data & Storage" items={["Room (SQLite), UUID ids", "Money as Long cents", "kotlinx.serialization (settings, backups)", "JobScheduler (reminders, rates)"]} dot="bg-tertiary" />
                <TechCard title="Scanning" items={["ML Kit text recognition (bundled)", "mi. Budget's own receipt parser", "Gemini Nano (on supported phones)"]} dot="bg-secondary" />
              </div>
            </motion.div>

            {/* Key decisions */}
            <motion.div id="decisions" {...fadeInUp} className="space-y-8">
              <SectionTitle icon={<Lightbulb size={24} />} title="Key Decisions" tone="bg-primary-container text-on-primary-container" />
              <div className="grid sm:grid-cols-2 gap-4">
                <Concept title="Plain Kotlin Core">domain/, the receipt parser and the brand matcher use no Android types, so they test fast and could move to Kotlin Multiplatform later.</Concept>
                <Concept title="UUID Ids">Records use UUID strings instead of auto-increment numbers, so exports from different phones merge without collisions and re-imports are recognised as duplicates.</Concept>
                <Concept title="Money in Cents">Every amount is a Long of cents: no floating-point drift. All formatting goes through domain/Money.kt.</Concept>
                <Concept title="Filtering in Memory">Filters run in Kotlin rather than dynamic SQL. For a personal budget it&apos;s instant, and a new filter is a few lines.</Concept>
                <Concept title="Brand Matching">assets/brands.json (about 950 merchants, banks and services, with aliases, colours and default categories) matches offline, handling noise like SQ *STARBUCKS #1234. Everyday words (Target, Ring) only match the whole name.</Concept>
                <Concept title="Receipt Rows">OCR often splits &quot;TOTAL&quot; and &quot;23.45&quot; into separate columns, so the parser rebuilds rows from text bounding boxes. There&apos;s always an editable review.</Concept>
                <div className="sm:col-span-2">
                  <Concept title="Backup Format">A .mibudgetdata file is a zip of data.json (versioned schemaVersion; ISO dates, cents, #AARRGGBB colours, each category&apos;s full path) plus receipts/. Any future iOS, desktop or web version can read it. allowBackup is off: data leaves the phone only when you export it.</Concept>
                </div>
              </div>
            </motion.div>

            {/* Project Structure */}
            <motion.div id="project-structure" {...fadeInUp} className="space-y-8">
              <SectionTitle icon={<FolderTree size={24} />} title="Project Structure" tone="bg-secondary-container text-on-secondary-container" />
              <div className="bg-surface-container-high rounded-3xl p-8 border border-outline-variant font-mono text-sm leading-relaxed overflow-x-auto">
                <pre>{PROJECT_STRUCTURE}</pre>
              </div>
            </motion.div>

            {/* Development Setup */}
            <motion.div id="setup" {...fadeInUp} className="space-y-8">
              <SectionTitle icon={<Terminal size={24} />} title="Development Setup" tone="bg-secondary-container text-on-secondary-container" />

              <div className="space-y-12">
                <div className="space-y-4">
                  <h3 className="text-xl font-bold">Prerequisites</h3>
                  <ul className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {["Android Studio (2025.1 or newer)", "JDK 17+", "A phone or emulator (Android 8.0+)", "Node.js 20+ (only for this website)"].map((item) => (
                      <li key={item} className="flex items-center gap-3 text-on-surface-variant font-medium">
                        <div className="w-1.5 h-1.5 rounded-full bg-secondary" />
                        {item}
                      </li>
                    ))}
                  </ul>
                </div>

                <div className="space-y-6">
                  <h3 className="text-xl font-bold">Installation</h3>
                  <CodeBlock command={`git clone https://github.com/mi-app-hub/mi-budget.git\ncd mi-budget\n./gradlew :app:fetchBrandIcons   # optional: merchant logos (network, build time only)`} />
                </div>

                <div className="space-y-6">
                  <h3 className="text-xl font-bold text-on-surface">Running the App</h3>
                  <CodeBlock command={`./gradlew :app:installDebug        # build + install mi. Budget dev beside the release\n./gradlew :app:testDebugUnitTest   # run the logic tests`} />
                  <div className="flex gap-4 text-sm font-medium text-on-surface-variant bg-surface-container-high p-4 rounded-2xl border border-outline-variant">
                    <Info size={18} className="shrink-0" />
                    <p>Debug builds install as <strong>mi. Budget dev</strong>, a separate app from the release, so they never touch your real data. Judge speed on a release build: debug Compose builds are much slower.</p>
                  </div>
                </div>

                <div className="space-y-6">
                  <h3 className="text-xl font-bold text-on-surface">Core Concepts</h3>
                  <div className="grid sm:grid-cols-2 gap-4">
                    <Concept title="The Main Bar">ui/common/MainBar.kt: Library · calendar · + · ⋮ · Settings. Swiping its Pill brings in the Library, Settings or Calendar as sheets that follow the finger (SheetHost.kt), and each sheet&apos;s search Pill morphs out of the bar&apos;s.</Concept>
                    <Concept title="Settings">One @Serializable AppSettings (data/Settings.kt) saved as JSON, with Choice enums, sanitized() and migrated(). One file per section in ui/settings/; searchSettings() is pure and tested.</Concept>
                    <div className="sm:col-span-2">
                      <Concept title="Storage Rules">Every file the app writes has one owner and is deleted when its owner is done: receipt photos with their purchase, camera captures once copied, unpacked backups after import. A launch sweep catches leftovers from crashes, touching only files older than the launch.</Concept>
                    </div>
                  </div>
                </div>
              </div>
            </motion.div>

            {/* Release */}
            <motion.div id="release" {...fadeInUp} className="space-y-8">
              <SectionTitle icon={<Rocket size={24} />} title="Making a Release" tone="bg-tertiary-container text-on-tertiary-container" />
              <div className="space-y-6">
                <p className="text-sm text-on-surface-variant">Bump <code className="bg-surface-container-highest px-1 rounded">appVersionName</code> and <code className="bg-surface-container-highest px-1 rounded">appVersionCode</code> at the top of app/build.gradle.kts, then fetch the bundled data and build:</p>
                <CodeBlock command={`./gradlew clean\n./gradlew :app:fetchBrandIcons -Prefresh :app:fetchFxRates\n./gradlew clean :app:assembleRelease :app:testDebugUnitTest\n# → app/build/outputs/apk/release/mi-budget-<version>-release.apk`} />
                <div className="p-4 rounded-xl border border-primary/20 bg-primary/5 text-xs text-primary/80">
                  <p><strong>Signing:</strong> create a <code className="bg-surface-container-highest px-1 rounded">keystore.properties</code> file in the project root (it is git-ignored) with <code className="bg-surface-container-highest px-1 rounded">storeFile</code>, <code className="bg-surface-container-highest px-1 rounded">storePassword</code>, <code className="bg-surface-container-highest px-1 rounded">keyAlias</code> and <code className="bg-surface-container-highest px-1 rounded">keyPassword</code>. Without it, release builds use this computer&apos;s debug key: fine for testing, not for publishing.</p>
                </div>
                <p className="text-sm text-on-surface-variant"><strong>Publishing:</strong> tag the version, draft a GitHub release with the notes from docs/releases/ (full URLs only) and the APK attached, and add it to CHANGELOG.md. The website deploys separately: <code className="bg-surface-container-highest px-1 rounded">cd docs && npm run deploy</code> builds it and publishes to the gh-pages branch.</p>
              </div>
            </motion.div>
          </section>
        </div>
      </div>

      <Footer />
    </main>
  );
}

function SectionTitle({ icon, title, tone }: { icon: React.ReactNode; title: string; tone: string }) {
  return (
    <div className="flex items-center justify-center sm:justify-start gap-3 mb-6">
      <div className={`p-2 rounded-xl ${tone}`}>{icon}</div>
      <h2 className="text-2xl sm:text-3xl font-bold tracking-tight">{title}</h2>
    </div>
  );
}

function Concept({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="p-6 rounded-2xl bg-surface-container-high border border-outline-variant h-full">
      <h4 className="font-bold text-sm mb-2">{title}</h4>
      <p className="text-xs text-on-surface-variant leading-relaxed">{children}</p>
    </div>
  );
}

function TechCard({ title, items, dot }: { title: string; items: string[]; dot: string }) {
  return (
    <div className="p-8 rounded-[2rem] bg-surface-container-high border border-outline-variant shadow-sm">
      <h4 className="font-bold text-lg mb-4">{title}</h4>
      <ul className="space-y-2">
        {items.map((item) => (
          <li key={item} className="flex items-center gap-2 text-on-surface-variant font-medium">
            <div className={`w-1.5 h-1.5 rounded-full ${dot}`} />
            {item}
          </li>
        ))}
      </ul>
    </div>
  );
}

function CodeBlock({ command }: { command: string }) {
  return (
    <div className="relative group">
      <div className="absolute -inset-0.5 bg-gradient-to-r from-primary to-tertiary rounded-2xl blur opacity-20 group-hover:opacity-40 transition duration-1000"></div>
      <div className="relative bg-inverse-surface rounded-2xl p-6 font-mono text-sm text-inverse-on-surface overflow-x-auto border border-outline-variant">
        <pre>{command}</pre>
      </div>
    </div>
  );
}
