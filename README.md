# Support Doctrine: Gamma Support

Reworks the Leadership skill **Support Doctrine** (Starsector 0.98a).

- **Removed:** "Gain non-elite Helmsmanship, Damage Control, Combat Endurance, and Ordnance Expertise".
- **Added:** "Ships without officers can be supported by Gamma cores". If you carry a Gamma core, it shows up next to your officers in the captain list of crewed ships in the Fleet screen.
- **Unchanged:** −20% deployment cost (max 10) for unofficered ships, and +100% command point recovery. A ship with a Gamma core counts as officered.

Rules:
- Only Gamma cores are offered. Alpha and Beta cores never appear for crewed ships. Automated ships behave exactly as in vanilla.
- The picker works from both the Fleet and Refit screens. It lists Gamma cores already on crewed ships, plus one from cargo:
  - Click the ship's current core to unassign it.
  - Click a core on another ship to move it.
- Swaps behave like vanilla officer swaps. Gamma cores can be swapped with officers or with you, in either direction. You can never end up without a ship.
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
