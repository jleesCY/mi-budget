"use client";

import { Navbar } from "@/components/navbar";
import { Footer } from "@/components/footer";
import { GITHUB_URL } from "@/lib/site";

// Keep in step with PRIVACY.md at the root of the repository.
const UPDATED = "November 24, 2026";

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="space-y-3">
      <h2 className="text-xl sm:text-2xl font-bold tracking-tight">{title}</h2>
      <div className="space-y-3 text-base leading-relaxed text-on-surface-variant">{children}</div>
    </section>
  );
}

export default function Privacy() {
  return (
    <main className="min-h-screen bg-surface-container-low text-on-surface transition-colors duration-300 overflow-x-hidden">
      <Navbar />
      <div className="pt-24 sm:pt-32 pb-20 px-6">
        <article className="max-w-3xl mx-auto space-y-12">
          <header className="space-y-4">
            <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tighter">
              Privacy <span className="text-primary italic">Policy</span>
            </h1>
            <p className="text-sm text-on-surface-variant font-medium">Last updated {UPDATED}</p>
            <p className="text-lg leading-relaxed text-on-surface-variant">
              mi. Budget collects no personal data. There are no accounts, no ads, no analytics and no crash reporters, and
              mi. has no servers of its own.
            </p>
          </header>

          <Section title="What stays on your device">
            <p>
              Everything you put in the app: purchases, refunds, subscriptions, categories and budgets, payment methods,
              receipt pictures, your settings and your past searches. It&apos;s stored in the app&apos;s own storage on your phone
              and nowhere else.
            </p>
            <p>
              Backups are files you make yourself (Settings → Backup &amp; Reset › Back Up) and keep wherever you choose. Deleted things
              wait in Recently Deleted on the phone until they&apos;re purged. Receipts are read on the phone, including by Gemini
              Nano on phones that have it.
            </p>
          </Section>

          <Section title="Connections mi. makes">
            <p>mi. Budget goes online only for these, each doing a job for you and sending nothing about you:</p>
            <ul className="list-disc pl-6 space-y-2">
              <li>
                <strong>Update checks</strong> (GitHub, api.github.com): looks for a newer version when the app opens (at
                most every 12 hours) and when you tap Check for Updates, and downloads the update if you choose to install
                it. It also fetches a new version&apos;s release notes for What&apos;s New.
              </li>
              <li><strong>Currency rates</strong> (Frankfurter, api.frankfurter.dev): only if you add the optional daily update pack in Settings → Tools.</li>
            </ul>
            <p>
              ML Kit&apos;s usage reporting is turned off, so text recognition sends nothing. These services receive your IP
              address and basic device information, as with any web request, under their own privacy policies.
            </p>
          </Section>

          <Section title="Permissions">
            <ul className="list-disc pl-6 space-y-2">
              <li><strong>Notifications</strong>: asked the first time you turn on renewal reminders (with a plain question first, then Android&apos;s prompt). Used only for the reminders you asked for.</li>
              <li><strong>Camera</strong>: used through Android&apos;s own camera app when you scan a receipt; mi. Budget never opens the camera itself.</li>
              <li><strong>Internet</strong>: for the connections above.</li>
              <li><strong>Install apps</strong>: lets Android install an update you chose to install; Android asks you first.</li>
            </ul>
          </Section>

          <Section title="Children">
            <p>mi. Budget isn&apos;t made for children and collects nothing from anyone.</p>
          </Section>

          <Section title="Changes and contact">
            <p>
              If this policy changes, the new version will be posted on this page with a new date. Questions? Open an
              issue on <a className="text-primary hover:underline" href={`${GITHUB_URL}/issues`} target="_blank" rel="noopener noreferrer">GitHub</a>.
            </p>
          </Section>
        </article>
      </div>
      <Footer />
    </main>
  );
}
