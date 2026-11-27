// Every setting in the app, in its order (app/src/main/java/com/jlees/mibudget/ui/settings/). The User Guide lists them
// and the search finds them, both from here, so a setting added to the app needs one line here.

export interface Setting { title: string; desc: string }
export interface SettingsGroup { title: string; settings: Setting[] }

export const SETTINGS: SettingsGroup[] = [
  {
    title: "Budgeting", settings: [
      { title: "Week Starts On", desc: "Sunday or Monday: where weeks start in the calendar and in weekly budgets." },
      { title: "Uncategorized Reminder", desc: "A banner on the start screen when purchases need a category." },
      { title: "Payment Methods", desc: "Your cards, bank accounts and wallets: a name, the last 4 digits if you like, and an icon." },
    ],
  },
  {
    title: "Notifications", settings: [
      { title: "Renewal Reminders", desc: "A notification before subscriptions renew and free trials end. mi. asks for permission the first time you turn it on." },
      { title: "Remind Me", desc: "A slider from the same day to a week before a renewal. Each subscription can choose its own or turn reminders off. Free trials always get at least 2 days." },
    ],
  },
  {
    title: "Scanner", settings: [
      { title: "Standard", desc: "The built-in reader (Google's ML Kit). Reads receipts, banking screenshots and pump screens on your phone. Nothing to download." },
      { title: "Gemini Nano", desc: "Google's on-device AI, built into Android on supported phones. Looks at the photo itself. Listed only where it works." },
    ],
  },
  {
    title: "Tools", settings: [
      { title: "Built-in Rates", desc: "About 160 currencies of central-bank rates come with the app, so the converter works offline." },
      { title: "Daily Rate Updates", desc: "An optional 10 KB pack that keeps rates current, updating once a day when you're online." },
      { title: "Update Now", desc: "Get today's rates (with the update pack)." },
    ],
  },
  {
    title: "Look & Feel", settings: [
      { title: "Theme", desc: "Light, Dark or System." },
      { title: "Pure Black", desc: "True black backgrounds in dark mode." },
      { title: "Accent Color", desc: "Adaptive (your wallpaper's colours), Gray, Blue, Green, Red, Orange, Yellow or Purple. Green is mi. Budget's own." },
      { title: "Font Size", desc: "The app's text size, 80% to 120%." },
      { title: "Font", desc: "Standard, Rounded or your phone's System font." },
      { title: "Pan Overflowed Text", desc: "Text that doesn't fit slides to show the rest, in the places you choose: Settings, Purchases, Subscriptions, Budgets, Recently Deleted." },
      { title: "Corners", desc: "Square, Soft or Round." },
      { title: "List Spacing", desc: "Compact, Normal or Airy." },
      { title: "List Style", desc: "Cards or Table, for every list: purchases, subscriptions, budgets, payment methods and Recently Deleted." },
      { title: "Rows Loaded at Once", desc: "Long lists load 10, 25, 50 or 100 rows at a time, and more as you scroll, so they open smoothly." },
      { title: "Status Bar", desc: "Show or hide the system status bar." },
      { title: "Rotate Screen", desc: "Turn mi. Budget sideways with your phone." },
      { title: "Gesture Haptics", desc: "Light taps as gestures cross their thresholds." },
    ],
  },
  {
    title: "Pill", settings: [
      { title: "Recent Searches", desc: "Your past searches appear above the Pill while you type." },
    ],
  },
  {
    title: "Start Screen", settings: [
      { title: "Chart", desc: "How this month's spending is drawn: Donut, Pie, Rings, Bars or Trend." },
      { title: "Clock", desc: "A clock on the start screen: None, 12h or 24h." },
      { title: "Date", desc: "None, Above or Below the clock." },
    ],
  },
  {
    title: "Privacy & Security", settings: [
      { title: "Hide From Screenshots", desc: "Keep mi. Budget out of screenshots and the Recents screen." },
    ],
  },
  {
    title: "Gestures", settings: [
      { title: "Long Press to Search", desc: "Hold the Pill to search your purchases." },
      { title: "Swipe Between Months", desc: "Swipe the month title or the chart sideways to change month." },
    ],
  },
  {
    title: "Recently Deleted", settings: [
      { title: "Recently Deleted", desc: "Purchases and subscriptions you deleted, ready to restore." },
      { title: "Keep Deleted Items", desc: "1 Week, 30 Days or 90 Days before they're gone for good." },
      { title: "Empty Recently Deleted", desc: "Delete everything in it for good." },
    ],
  },
  {
    title: "Backup & Reset", settings: [
      { title: "Back Up", desc: "Save everything (purchases, subscriptions, budgets, receipt photos and settings) to a .mibudgetdata file." },
      { title: "Restore", desc: "Bring back everything from a backup file. Budgey backups restore too." },
      { title: "Reset All Settings", desc: "Back to defaults; your purchases are kept." },
      { title: "Restore Wiped Data", desc: "After Wipe All Data, a safety copy is kept for 7 days. Tap to bring it all back." },
      { title: "Delete Safety Copy", desc: "Remove the safety copy now instead of waiting." },
      { title: "Wipe All Data", desc: "Delete every purchase, subscription and setting." },
    ],
  },
  {
    title: "About", settings: [
      { title: "Check for Updates", desc: "Look for a new mi. Budget release and install it. Your data stays." },
      { title: "Gesture Walkthrough", desc: "Try every swipe again, step by step." },
      { title: "Report a Bug or Suggest a Feature", desc: "Open a new issue on GitHub." },
      { title: "More mi. Apps", desc: "The other mi. apps. Opens one if it's installed, or its website if not." },
      { title: "Privacy Policy", desc: "mi. Budget collects no personal data." },
      { title: "Credits", desc: "Open-source projects, exchange rates and brand logos mi. Budget is built with." },
    ],
  },
];
