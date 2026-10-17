# AI Support Doctrine

Reworks the Leadership skill **Support Doctrine** into **AI Support Doctrine** (Starsector 0.98a). Flavor: Tri-Tachyon's "Gamma Assist" AI assistant.

- **Removed:** "Gain non-elite Helmsmanship, Damage Control, Combat Endurance, and Ordnance Expertise".
- **Added:** "Ships without officers can be supported by Gamma cores". If you carry a Gamma core, it shows up next to your officers in the captain list of crewed ships in the Fleet screen.
- **Kept:** −20% deployment cost (max 10) for unofficered ships, which also applies to crewed ships supported by a Gamma core, and +100% command point recovery, which (like an unofficered ship) is lost when commanding from a ship supported by a Gamma core.
- A Gamma core on a crewed ship doesn't count as an officer for vanilla skills that boost ships with officers: Carrier Group, Fighter Uplink, Coordinated Maneuvers and Wolfpack Tactics. This mod overrides those skill files, so another mod that changes the same skills will conflict with it. The same applies to battle difficulty (the post-battle XP multiplier): ships supported by a Gamma core count as ships without officers.
- Gamma cores on crewed ships appear as "Gamma Assist" with the Gamma core icon as their portrait, and follow the fleet doctrine's aggression setting (shown as their personality in the picker and tooltips). At aggression 4, like vanilla's ships without officers, each one rolls aggressive or reckless at the start of each battle; out of combat it shows as aggressive.

Rules:
- Only Gamma cores are offered. Alpha and Beta cores never appear for crewed ships. Automated ships behave exactly as in vanilla.
- The picker works from both the Fleet and Refit screens. It lists Gamma cores already on crewed ships, plus one from cargo:
  - Click the ship's current core to unassign it.
  - Click a core on another ship to move it.
- Swaps behave like vanilla officer swaps. Gamma cores can be swapped with officers or with you, in either direction. You can never end up without a ship.
- Gamma cores on crewed ships can't take the capstone skills Systems Expertise or Missile Specialization. They're hidden in "Change skills", leaving a gap, and removed from existing cores, leaving a free slot.
- Gamma cores on crewed ships get the regular (non-elite) versions of their skills, including skills picked with "Change skills". Known cosmetic issue: a newly picked skill flashes as elite for one frame.
- Gamma cores on crewed ships can't be integrated. The button is hidden; if an integration still happens, it is undone and the story point is refunded.
- Replacing a Gamma core with an unassigned officer refunds the core. Selling, storing or scuttling the ship returns it through vanilla.
- If you respec out of the skill, assigned Gamma cores go back to your cargo.

## How it works
- While a core screen is open, the mod finds the vanilla captain picker (`coreui.CaptainPickerDialog`). For crewed ships it appends Gamma-core entries built with the picker's own "AI core" entry type, so the game's AI-core assignment handles cargo and the captain.
- Each frame the mod snapshots every ship's captain. Vanilla's picker only half-does swaps involving a Gamma core on a crewed ship, so the mod finishes them on the next frame.
- Reflection goes through `MethodHandle`s only, because Starsector refuses to load mod classes that reference `java.lang.reflect`.
- If the picker internals change in a future game version, the injection logs a warning and switches itself off for the session. The rest of the game is unaffected.
- No ship tags or custom classes are written to saves, so the mod can be removed safely. Remove any Gamma cores from crewed ships first.

## Build
```
powershell -File build.ps1 [-GameDir <Starsector folder>] [-JdkBin <JDK bin folder>]
```
Compiles `src/` with `javac --release 17` against the game's jars into `jars/SupportDoctrineGamma.jar`.
