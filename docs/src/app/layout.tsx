import type { Metadata, Viewport } from "next";
import localFont from "next/font/local";
import "./globals.css";
import theme from "./theme.json";

// the app's own typeface (Google Sans Flex, as bundled in the app)
const sans = localFont({ src: "../fonts/google_sans_flex.ttf", display: "swap", variable: "--font-sans" });

export const metadata: Metadata = {
  title: "mi. Budget | Your Money, Private on Your Phone",
  description: "A private Android budget app: this month at a glance, receipts scanned on your phone, budgets, subscriptions and free trials. No accounts, no ads, no tracking.",
  // the app icon (public/icon.svg), under the site's base path on GitHub Pages
  icons: { icon: `${process.env.NODE_ENV === "production" ? `/${process.env.NEXT_PUBLIC_REPO_NAME}` : ""}/icon.svg` },
};

// the browser's bar takes the page's colour, light or dark (surfaceContainerLow, from scripts/theme.mjs)
export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: theme.page.light },
    { media: "(prefers-color-scheme: dark)", color: theme.page.dark },
  ],
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className={sans.variable}>
      <body className="antialiased font-sans">{children}</body>
    </html>
  );
}
