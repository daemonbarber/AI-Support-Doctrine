# AI Support Doctrine

Reworks the Leadership skill **Support Doctrine** into **AI Support Doctrine** (Starsector 0.98a). Flavor: Tri-Tachyon's "Gamma Assist" AI assistant.

## The skill
- **Removed:** "Gain non-elite Helmsmanship, Damage Control, Combat Endurance, and Ordnance Expertise".
- **Added:** "Ships without officers can be supported by Gamma Cores". AI cores in your cargo show up next to your officers in the captain list of crewed ships. Which core types are offered is a setting; the default is Gamma only.
- **Kept:**
  - −20% deployment cost (max 10) for unofficered ships. This also applies to crewed ships supported by an AI core.
  - +100% command point recovery. Like with an unofficered ship, it's lost when you command from a ship supported by an AI core.

## Gamma Assists
An AI core supporting a crewed ship is called a **Gamma Assist**. With the default settings:
- **Name and portrait:** every supporting core is named "Gamma Assist - by Tri-Tachyon", whatever its grade, and shows its own core icon as its portrait.
- **Personality:** it follows your fleet doctrine's aggression setting, which is shown in the picker and tooltips. At aggression 4, like vanilla's ships without officers, it rolls aggressive or reckless at the start of each battle; out of combat it shows as aggressive.
- **Skills:** it only gets the regular (non-elite) versions of its skills, including skills picked with "Change skills". *(Setting.)*
- **No capstones:** it can't take Systems Expertise or Missile Specialization. They're hidden in "Change skills", leaving a gap, and removed from existing cores, leaving a free slot. *(Setting.)*
- **No integration:** the "Fully integrate" button is hidden. If an integration still happens, it's undone and the story point refunded.
- **Not an officer:** a ship supported by an Assist counts as having no officer for the skills Carrier Group, Fighter Uplink, Coordinated Maneuvers and Wolfpack Tactics, and for battle difficulty (the post-battle XP multiplier). *(Setting.)*

## Settings
- **With LunaLib installed:** change these in its mod settings menu. They apply immediately.
- **Without LunaLib:** edit the `aisd_*` entries in this mod's `data/config/settings.json` and restart the game. With LunaLib installed, that file is ignored.

| Setting | Default | Effect |
|---|---|---|
| Allowed AI cores | Gamma only | Which cores can support crewed ships: Gamma only, Gamma + Beta, or Gamma + Beta + Alpha. Cores that are no longer allowed go back to cargo when you close the Fleet screen. |
| Non-elite skills only | On | Off: Assists get elite skills, like vanilla AI cores (existing Assists are raised to elite). |
| Block capstone skills | On | Off: Assists can take Systems Expertise and Missile Specialization. |
| Assists count as officers | Off | On: Assist-supported ships count as officered for Carrier Group, Fighter Uplink, Coordinated Maneuvers, Wolfpack Tactics and battle difficulty. AI Support Doctrine's own effects (deployment cost, command points) are unaffected. |

Settings are re-read on load and whenever you open the core UI. The skill's tooltip text (the effect line and the footnote) follows them.

The `settings.json` keys are `aisd_allowedCores` (one of `"Gamma only"`, `"Gamma + Beta"`, `"Gamma + Beta + Alpha"`), `aisd_nonEliteOnly`, `aisd_blockCapstones`, and `aisd_countsAsOfficer` (`true`/`false`).

## Notes and known issues
- **Deployment points:** when the game splits the battle size between the two sides, it ignores ships captained by an AI core unless your fleet is also commanded by one. The same goes for automated ships with cores in a normal fleet. An Assist-supported ship therefore adds nothing to your share, where an unofficered ship would add a little based on its deployment cost. Officers make up most of the calculation, so with small ships you'll barely notice. With a lot of capitals it adds up: 30 capitals with 21 Assists get roughly 15% less strength than the same fleet with those 21 left unofficered. The mod leaves this as is.
- **Skill flicker:** in "Change skills", a newly picked skill flashes as elite for one frame before dropping to regular.
- **Compatibility:** this mod replaces the skill files for Support Doctrine, Carrier Group, Fighter Uplink, Coordinated Maneuvers and Wolfpack Tactics, using `replace` in `mod_info.json`.
  - **Support Doctrine** swaps out its free-skills effect for the Gamma Assist effect. Its deployment cost and command point effects also need to treat Assist-supported ships as unofficered.
  - **The other four** give extra bonuses to ships with officers. Vanilla decides "has an officer" inside the skill's own code, so the only way to stop Assists counting is to point the skill at a copy of its effect with a different check. Everything else about those skills is unchanged.
  - Another mod that changes the same skills will conflict with this one.
- **Starsector 0.98.5a** (based on the in-development patch notes as of October 6, 2026): the upcoming update changes Carrier Group and Fighter Uplink, and reworks officers. This mod's copies of those skill effects, its battle difficulty calculation, and its captain-picker integration will need updating for that version.

## How it works
- **Captain picker:** while the Fleet or Refit tab is open, the mod finds the vanilla captain picker (`coreui.CaptainPickerDialog`). For crewed ships it adds AI-core entries, built with the picker's own "AI core" entry type, so vanilla's AI-core assignment handles cargo and the captain.
- **Finishing swaps:** on those tabs the mod snapshots every ship's captain each frame. Vanilla's picker only half-does swaps involving an AI core on a crewed ship, so the mod finishes them on the next frame. Nothing runs per frame outside those two tabs.
- **Respec:** if you respec out of the skill, every Assist goes back to your cargo.
- **Automated ships:** unaffected. Their cores behave exactly as in vanilla.
- **Reflection:** the captain picker isn't part of the modding API, so the mod reaches it through `MethodHandle`s, the same approach used by LunaLib and other UI mods. This stays within Starsector's script security rules, which don't allow mods to use `java.lang.reflect` directly.
- **Game updates:** if the picker internals change in a future version, the injection logs a warning and switches itself off for the session. The rest of the game is unaffected.
- **Saves:** no ship tags or custom classes are written to them, so the mod can be removed safely. Remove any Assists from crewed ships first.

## Build
```
powershell -File build.ps1 [-GameDir <Starsector folder>] [-JdkBin <JDK bin folder>]
```
Compiles `src/` with `javac --release 17` against the game's jars and LunaLib's jar (from `mods/LunaLib*`), into `jars/AISupportDoctrine.jar`.
