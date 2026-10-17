package aisupportdoctrine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Adds a Gamma core entry to the vanilla captain picker for crewed ships.
 *
 * The picker (coreui.CaptainPickerDialog) lists the player and officers for crewed ships, and only
 * AI cores for automated ships. Its entries (one obfuscated class) take a flag that selects the
 * "AI core" click behavior: take the core from cargo, assign it, and return any replaced core.
 * We append an entry built with that flag set, so officers and the Gamma core sit in one list.
 *
 * Everything here depends on obfuscated internals, so any failure disables injection for the
 * session (logged once) rather than breaking the Fleet screen.
 */
final class PickerInjector {

    private static final String PICKER_CLASS = "com.fs.starfarer.coreui.CaptainPickerDialog";
    private static final String FLEET_CLASS = "com.fs.starfarer.campaign.fleet.CampaignFleet";
    private static final String MEMBER_CLASS = "com.fs.starfarer.campaign.fleet.FleetMember";
    private static final String OFFICER_DATA_CLASS = "com.fs.starfarer.rpg.OfficerData";
    private static final String PERSON_CLASS = "com.fs.starfarer.rpg.Person";
    private static final String CHARACTER_STATS_CLASS = "com.fs.starfarer.campaign.CharacterStats";
    private static final String INTEGRATE_TEXT = "Fully integrate";
    private static final String FEARLESS_TEXT = "Fearless";
    private static final String RECKLESS_TEXT = "Reckless";

    private static boolean disabled = false;
    private static Object entryCtor = null;
    private static Object officerDataCtor = null;

    private PickerInjector() {
    }

    /** Called every frame while the core UI is open. */
    static void advance() {
        if (disabled) return;
        try {
            Object screen = Reflect.call(Global.getSector().getCampaignUI(), "getScreenPanel");
            if (screen == null) return;
            Object[] found = new Object[2]; // [captain picker, AI core "Change skills" dialog]
            findDialogs(screen, 0, found);
            if (found[0] != null) handlePicker(found[0]);
            if (found[1] != null) handleSkillDialog(found[1]);
        } catch (Throwable t) {
            disabled = true;
            GammaSupport.LOG.warn("AISD: captain picker integration failed; Gamma cores won't be offered this session", t);
        }
    }

    private static void findDialogs(Object component, int depth, Object[] found) throws Throwable {
        if (component.getClass().getName().equals(PICKER_CLASS)) {
            found[0] = component;
        } else if (isSkillDialog(component)) {
            found[1] = component;
        }
        if (depth > 20 || !Reflect.hasMethod(component, "getChildrenCopy", 0)) return;
        for (Object child : (List<?>) Reflect.call(component, "getChildrenCopy")) {
            findDialogs(child, depth + 1, found);
        }
    }

    /** The obfuscated dialog opened by an AI core's "Change skills" button. */
    private static boolean isSkillDialog(Object component) throws Throwable {
        return Reflect.hasMethod(component, "getNumSkills", 0)
                && Reflect.hasMethod(component, "updateSkillsLabel", 0)
                && Reflect.hasMethod(component, "createSkillPointsLabel", 1);
    }

    /**
     * The "Change skills" dialog edits a working copy of the core's stats, and clicking a skill
     * always sets it to level 2 (elite). For AI cores on crewed ships, apply the skill rules from the
     * settings to the working copy so the dialog shows, and confirming applies, what's allowed.
     */
    private static void handleSkillDialog(Object dialog) throws Throwable {
        PersonAPI person = (PersonAPI) Reflect.getFieldOfType(dialog, PERSON_CLASS);
        if (!GammaSupport.isCrewedCore(person)) return;
        // Known cosmetic issue: this runs a frame after a click is drawn, so a newly picked skill
        // shows as elite for one frame before dropping to regular.
        clampSkillDialog(dialog);
        if (!AISDSettings.blockCapstones) return;
        int hidden = hideBlockedSkillButtons(dialog, 0);
        if (hidden == 0 && !loggedNoSkillButtons) {
            loggedNoSkillButtons = true;
            GammaSupport.LOG.warn("AISD: no capstone skill buttons found in the Change skills dialog");
        }
    }

    private static boolean loggedNoSkillButtons = false;

    /**
     * Hides the capstone skill buttons (Gamma Assists can't take them), leaving a gap in the layout.
     * Returns how many were found. Also descends into dialogs' inner panels, since those aren't part
     * of the regular child list.
     */
    private static int hideBlockedSkillButtons(Object component, int depth) throws Throwable {
        int found = 0;
        if (component instanceof ButtonAPI) {
            ButtonAPI button = (ButtonAPI) component;
            Object data = button.getCustomData();
            if (data instanceof SkillSpecAPI && GammaSupport.BLOCKED_SKILLS.contains(((SkillSpecAPI) data).getId())) {
                button.setEnabled(false);
                button.setOpacity(0f);
                found++;
            }
        }
        if (depth > 20) return found;
        if (Reflect.hasMethod(component, "getChildrenCopy", 0)) {
            for (Object child : (List<?>) Reflect.call(component, "getChildrenCopy")) {
                found += hideBlockedSkillButtons(child, depth + 1);
            }
        }
        // Dialogs add their contents (including the skill buttons) to an inner panel that isn't in
        // their regular child list.
        if (Reflect.hasMethod(component, "getInnerPanel", 0)) {
            Object inner = Reflect.call(component, "getInnerPanel");
            if (inner != null && inner != component) found += hideBlockedSkillButtons(inner, depth + 1);
        }
        return found;
    }

    private static void clampSkillDialog(Object dialog) throws Throwable {
        MutableCharacterStatsAPI workingCopy =
                (MutableCharacterStatsAPI) Reflect.getFieldOfType(dialog, CHARACTER_STATS_CLASS);
        if (workingCopy == null) return;
        GammaSupport.applySkillRules(workingCopy);
    }

    private static void handlePicker(Object picker) throws Throwable {
        FleetMemberAPI member = (FleetMemberAPI) Reflect.getFieldOfType(picker, MEMBER_CLASS);
        if (member == null || member.isFighterWing() || Misc.isAutomated(member)) return;

        Object list = Reflect.call(picker, "getListOfficers");
        if (list == null) return;
        // getItems() is the live list; copy it since we add/remove entries below.
        List<?> items = new ArrayList<>((List<?>) Reflect.call(list, "getItems"));


        // Fighter wings and automated ships were already excluded above.
        boolean allowed = GammaSupport.playerHasSkill();
        // The player's own ship may take a core from another ship (a swap, finished by
        // GammaSupport.reconcileCaptainChanges), but not one from cargo: that would leave the player
        // without a ship, which vanilla never allows.
        boolean allowCargoCore = allowed && !GammaSupport.isPlayerShip(member);

        // Vanilla never lists AI cores for crewed ships, so every AI core entry here is ours.
        List<Object> coreEntries = new ArrayList<>();
        Set<PersonAPI> listed = new HashSet<>();
        Set<String> cargoTypesListed = new HashSet<>();
        boolean removed = false;
        FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();
        for (Object item : items) {
            if (!Reflect.hasMethod(item, "getPerson", 0)) continue;
            PersonAPI person = (PersonAPI) Reflect.call(item, "getPerson");
            if (person == null || !person.isAICore()) continue;
            boolean fromCargo = fleetData.getMemberWithCaptain(person) == null;
            if (!allowed || !GammaSupport.isSupportCore(person) || (fromCargo && !allowCargoCore)) {
                Reflect.call(list, "removeItem", item);
                removed = true;
                continue;
            }
            coreEntries.add(item);
            listed.add(person);
            if (fromCargo) cargoTypesListed.add(person.getAICoreId());
        }
        if (removed) Reflect.call(list, "collapseEmptySlots");
        if (!allowed) return;

        // Like vanilla does for automated ships: list the cores already assigned (clicking the current
        // captain unassigns it, clicking another ship's core moves it), then one of each allowed core
        // type in cargo. Cores on automated ships are never listed. The picker rebuilds its list on
        // every resize, so re-add whatever is missing each frame.
        int count = items.size();
        for (FleetMemberAPI other : fleetData.getMembersListCopy()) {
            PersonAPI captain = other.getCaptain();
            if (!GammaSupport.hasCoreOnCrewedShip(other) || !GammaSupport.isSupportCore(captain)) continue;
            if (listed.contains(captain)) continue;
            coreEntries.add(addEntry(picker, member, list, count++, captain));
        }
        if (allowCargoCore) {
            for (String coreId : AISDSettings.allowedCoreIds()) {
                if (cargoTypesListed.contains(coreId) || !hasCoreInCargo(coreId)) continue;
                // Built with the mod's rules applied, so the picker previews what the ship will get.
                coreEntries.add(addEntry(picker, member, list, count++, GammaSupport.createCrewedCore(coreId)));
            }
        }

        for (Object entry : coreEntries) {
            tidyCoreEntry(entry, 0);
        }
    }

    private static boolean hasCoreInCargo(String coreId) {
        return Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(coreId) >= 1f;
    }

    private static Object addEntry(Object picker, FleetMemberAPI member, Object list, int index, PersonAPI person)
            throws Throwable {
        if (entryCtor == null) {
            for (Object item : (List<?>) Reflect.call(list, "getItems")) {
                entryCtor = Reflect.findConstructor(item.getClass(),
                        PICKER_CLASS, FLEET_CLASS, MEMBER_CLASS, OFFICER_DATA_CLASS, "boolean", "boolean");
                if (entryCtor != null) break;
            }
            if (entryCtor == null) throw new IllegalStateException("captain picker entry constructor not found");
        }
        if (officerDataCtor == null) {
            Class<?> officerDataClass = Class.forName(OFFICER_DATA_CLASS, false, picker.getClass().getClassLoader());
            officerDataCtor = Reflect.findConstructor(officerDataClass, PERSON_CLASS);
            if (officerDataCtor == null) throw new IllegalStateException("OfficerData constructor not found");
        }

        Object officerData = Reflect.newInstance(officerDataCtor, person);
        Object fleet = Reflect.getFieldOfType(picker, FLEET_CLASS);
        boolean shaded = index % 2 == 0; // vanilla alternates row backgrounds starting with true

        Object entry = Reflect.newInstance(entryCtor, picker, fleet, member, officerData, shaded, true);
        Reflect.call(list, "addItem", entry, officerData);
        return entry;
    }

    /**
     * Per-frame fixes to our core entries: hide "Fully integrate...", and undo the picker's AI-core
     * label that shows a reckless personality as "Fearless" (Gamma Assists follow the doctrine, so
     * reckless is a real setting here and should read as one).
     */
    private static void tidyCoreEntry(Object component, int depth) throws Throwable {
        if (component instanceof ButtonAPI) {
            ButtonAPI button = (ButtonAPI) component;
            String text = button.getText();
            if (text != null && text.startsWith(INTEGRATE_TEXT)) {
                button.setEnabled(false);
                button.setOpacity(0f);
            }
        } else if (component instanceof LabelAPI) {
            LabelAPI label = (LabelAPI) component;
            String text = label.getText();
            if (text != null && text.contains(FEARLESS_TEXT)) {
                label.setText(text.replace(FEARLESS_TEXT, RECKLESS_TEXT));
            }
        }
        if (depth > 6 || !Reflect.hasMethod(component, "getChildrenCopy", 0)) return;
        for (Object child : (List<?>) Reflect.call(component, "getChildrenCopy")) {
            tidyCoreEntry(child, depth + 1);
        }
    }
}
