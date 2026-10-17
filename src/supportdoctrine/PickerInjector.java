package supportdoctrine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.ui.ButtonAPI;
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
    private static final String INTEGRATE_TEXT = "Fully integrate";

    private static boolean disabled = false;
    private static Object entryCtor = null;
    private static Object officerDataCtor = null;

    private PickerInjector() {
    }

    /** Called every frame while the Fleet tab is open. */
    static void advance() {
        if (disabled) return;
        try {
            Object screen = Reflect.call(Global.getSector().getCampaignUI(), "getScreenPanel");
            if (screen == null) return;
            Object picker = findPicker(screen, 0);
            if (picker != null) handlePicker(picker);
        } catch (Throwable t) {
            disabled = true;
            GammaSupport.LOG.warn("SSD: captain picker integration failed; Gamma cores won't be offered this session", t);
        }
    }

    private static Object findPicker(Object component, int depth) throws Throwable {
        if (component.getClass().getName().equals(PICKER_CLASS)) return component;
        if (depth > 20 || !Reflect.hasMethod(component, "getChildrenCopy", 0)) return null;
        for (Object child : (List<?>) Reflect.call(component, "getChildrenCopy")) {
            Object found = findPicker(child, depth + 1);
            if (found != null) return found;
        }
        return null;
    }

    private static void handlePicker(Object picker) throws Throwable {
        FleetMemberAPI member = (FleetMemberAPI) Reflect.getFieldOfType(picker, MEMBER_CLASS);
        if (member == null || member.isFighterWing() || Misc.isAutomated(member)) return;

        Object list = Reflect.call(picker, "getListOfficers");
        if (list == null) return;
        // getItems() is the live list; copy it since we add/remove entries below.
        List<?> items = new ArrayList<>((List<?>) Reflect.call(list, "getItems"));

        boolean allowed = GammaSupport.canReceiveGamma(member);
        // The player's own ship may take a core from another ship (a swap, finished by
        // GammaSupport.reconcileCaptainChanges), but not one from cargo: that would leave the player
        // without a ship, which vanilla never allows.
        boolean allowCargoCore = allowed && !GammaSupport.isPlayerShip(member);

        List<Object> gammaEntries = new ArrayList<>();
        Set<PersonAPI> listed = new HashSet<>();
        boolean cargoEntryListed = false;
        boolean removed = false;
        FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();
        for (Object item : items) {
            if (!Reflect.hasMethod(item, "getPerson", 0)) continue;
            PersonAPI person = (PersonAPI) Reflect.call(item, "getPerson");
            if (!GammaSupport.isGamma(person)) continue;
            boolean fromCargo = fleetData.getMemberWithCaptain(person) == null;
            if (!allowed || (fromCargo && !allowCargoCore)) {
                Reflect.call(list, "removeItem", item);
                removed = true;
                continue;
            }
            gammaEntries.add(item);
            listed.add(person);
            if (fromCargo) cargoEntryListed = true;
        }
        if (removed) Reflect.call(list, "collapseEmptySlots");
        if (!allowed) return;

        // Like vanilla does for automated ships: list the cores already assigned (clicking the current
        // captain unassigns it, clicking another ship's core moves it), then one core from cargo.
        // Cores on automated ships are never listed. The picker rebuilds its list on every resize, so
        // re-add whatever is missing each frame.
        int count = items.size();
        for (FleetMemberAPI other : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            PersonAPI captain = other.getCaptain();
            if (!GammaSupport.hasCoreOnCrewedShip(other) || !GammaSupport.isGamma(captain)) continue;
            if (listed.contains(captain)) continue;
            gammaEntries.add(addEntry(picker, member, list, count++, captain));
        }
        if (allowCargoCore && !cargoEntryListed && hasGammaInCargo()) {
            PersonAPI fresh = Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE)
                    .createPerson(Commodities.GAMMA_CORE, Factions.PLAYER, null);
            gammaEntries.add(addEntry(picker, member, list, count, fresh));
        }

        for (Object entry : gammaEntries) {
            disableIntegrateButtons(entry, 0);
        }
    }

    private static boolean hasGammaInCargo() {
        return Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(Commodities.GAMMA_CORE) >= 1f;
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

    private static void disableIntegrateButtons(Object component, int depth) throws Throwable {
        if (component instanceof ButtonAPI) {
            ButtonAPI button = (ButtonAPI) component;
            String text = button.getText();
            if (text != null && text.startsWith(INTEGRATE_TEXT)) {
                button.setEnabled(false);
                button.setOpacity(0f);
            }
        }
        if (depth > 6 || !Reflect.hasMethod(component, "getChildrenCopy", 0)) return;
        for (Object child : (List<?>) Reflect.call(component, "getChildrenCopy")) {
            disableIntegrateButtons(child, depth + 1);
        }
    }
}
