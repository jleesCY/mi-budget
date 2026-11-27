// Shared by every page: links, the base path on GitHub Pages, and anchor ids.

export const GITHUB_URL = "https://github.com/mi-app-hub/mi-budget";

/** The other released mi. apps (the mi. family registry), shown in "More mi. Apps" on the homepage and in the footer. */
export const MI_APPS = [
  { name: "mi. Browser", pitch: "A simple, private browser built around one Pill.", site: "https://mi-app-hub.github.io/mi-browser/", icon: "/apps/mi-browser.png" },
  { name: "mi. Notes", pitch: "Notes in folders on one calm page.", site: "https://mi-app-hub.github.io/mi-notes/", icon: "/apps/mi-notes.png" },
  { name: "mi. Edits", pitch: "A simple, private video and audio editor.", site: "https://mi-app-hub.github.io/mi-edits/", icon: "/apps/mi-edits.png" },
  { name: "mi. Sports", pitch: "Live scores for the teams, players and leagues you follow.", site: "https://mi-app-hub.github.io/mi-sports/", icon: "/apps/mi-sports.png" },
  { name: "mi. Weather", pitch: "Every detail of the weather where you are, all inside the app.", site: "https://mi-app-hub.github.io/mi-weather/", icon: "/apps/mi-weather.png" },
];

/** A file in public/, under the site's base path in production (GitHub Pages serves it at /mi-budget/). */
export const getAssetPath = (path: string) => {
  const isProd = process.env.NODE_ENV === "production";
  const repoName = process.env.NEXT_PUBLIC_REPO_NAME || "mi-budget";
  return `${isProd ? `/${repoName}` : ""}${path}`;
};

/** "Look & Feel" → "look-feel": the #anchor of a section or setting. */
export const slug = (s: string) => s.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
