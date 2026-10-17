package supportdoctrine;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Only does work on the Fleet and Refit tabs of the core UI, plus once when the core UI closes:
 * - adds Gamma core entries to the captain picker and keeps "Change skills" non-elite;
 * - finishes swaps / refunds Gamma cores that vanilla's picker only half-handles;
 * - once the core UI closes, enforces the rules (Gamma only, no integration, skill required).
 */
public class SSDGammaMonitor implements EveryFrameScript {

    private boolean wasInCoreUI = false;
    private boolean wasOnPickerTab = false;

    public boolean isDone() {
        return false;
    }

    public boolean runWhilePaused() {
        return true;
    }

    public void advance(float amount) {
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        CoreUITabId tab = ui == null ? null : ui.getCurrentCoreTab();

        // Set the battle difficulty (XP multiplier) before vanilla computes its own, which would count
        // supporting Gamma cores as officers. Only does work once per battle dialog.
        if (ui != null && ui.getCurrentInteractionDialog() != null) {
            try {
                BattleDifficulty.apply(ui.getCurrentInteractionDialog());
            } catch (Throwable t) {
                GammaSupport.LOG.warn("SSD: failed to compute battle difficulty; using vanilla's", t);
            }
        }

        // Captains can only be changed via the captain picker, which (with the "Change skills" dialog
        // it opens) only exists on the Fleet and Refit tabs, so the per-frame work is limited to those.
        // Changes made elsewhere (respec on the Character tab, doctrine aggression) are handled by
        // enforce() when the core UI closes.
        // Pick up changes made in the LunaLib settings menu.
        if (tab != null && !wasInCoreUI) SSDSettings.reload();

        boolean pickerTab = tab == CoreUITabId.FLEET || tab == CoreUITabId.REFIT;
        if (pickerTab) {
            // Fresh baseline on arrival; captains may have changed since we last looked.
            if (!wasOnPickerTab) GammaSupport.snapshotCaptains();
            PickerInjector.advance();
            GammaSupport.reconcileCaptainChanges();
            GammaSupport.normalizeCrewedCores();
            GammaSupport.snapshotCaptains();
        }
        if (tab == null && wasInCoreUI) {
            enforce();
        }
        wasOnPickerTab = pickerTab;
        wasInCoreUI = tab != null;
    }

    public static void enforce() {
        CampaignFleetAPI fleet = GammaSupport.playerFleet();
        if (fleet == null) return;
        boolean hasSkill = GammaSupport.playerHasSkill();

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (!GammaSupport.hasCoreOnCrewedShip(member)) continue;
            PersonAPI captain = member.getCaptain();

            String name = captain.getNameString();
            if (!hasSkill) {
                GammaSupport.returnCoreToCargo(member);
                message(member.getShipName() + ": " + name + " returned to cargo (AI Support Doctrine no longer known)");
            } else if (!GammaSupport.isSupportCore(captain)) {
                GammaSupport.returnCoreToCargo(member);
                message(name + " returned to cargo: AI Support Doctrine only allows "
                        + SSDSettings.allowedCoresText() + " on crewed ships");
            } else if (Misc.isUnremovable(captain)) {
                GammaSupport.resetIntegratedCore(member);
                Global.getSector().getPlayerStats().addStoryPoints(1);
                message(member.getShipName() + ": AI cores on crewed ships can't be integrated - story point refunded");
            }
        }
        GammaSupport.normalizeCrewedCores();
    }

    private static void message(String text) {
        GammaSupport.LOG.info("SSD: " + text);
        Global.getSector().getCampaignUI().addMessage(text, Misc.getHighlightColor());
    }
}
