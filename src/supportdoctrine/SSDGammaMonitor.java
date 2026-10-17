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
 * Runs every frame (including while the core UI is open):
 * - while the core UI is open, adds Gamma core entries to the captain picker;
 * - refunds Gamma cores that vanilla code overwrote without returning;
 * - once the core UI closes, enforces the rules (Gamma only, no integration, skill required).
 */
public class SSDGammaMonitor implements EveryFrameScript {

    private boolean wasInCoreUI = false;

    public boolean isDone() {
        return false;
    }

    public boolean runWhilePaused() {
        return true;
    }

    public void advance(float amount) {
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        CoreUITabId tab = ui == null ? null : ui.getCurrentCoreTab();

        // The captain picker can be opened from the Fleet and Refit tabs, so look for it on any tab.
        if (tab != null) {
            PickerInjector.advance();
        }

        GammaSupport.reconcileCaptainChanges();
        if (tab == null && wasInCoreUI) {
            enforce();
        }
        GammaSupport.snapshotCaptains();
        wasInCoreUI = tab != null;
    }

    public static void enforce() {
        CampaignFleetAPI fleet = GammaSupport.playerFleet();
        if (fleet == null) return;
        boolean hasSkill = GammaSupport.playerHasSkill();

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (!GammaSupport.hasCoreOnCrewedShip(member)) continue;
            PersonAPI captain = member.getCaptain();

            if (!hasSkill) {
                GammaSupport.returnCoreToCargo(member);
                message(member.getShipName() + ": Gamma core returned to cargo (Support Doctrine no longer known)");
            } else if (!GammaSupport.isGamma(captain)) {
                String name = captain.getNameString();
                GammaSupport.returnCoreToCargo(member);
                message(name + " returned to cargo: Support Doctrine only supports Gamma cores on crewed ships");
            } else if (Misc.isUnremovable(captain)) {
                GammaSupport.resetIntegratedGamma(member);
                Global.getSector().getPlayerStats().addStoryPoints(1);
                message(member.getShipName() + ": Gamma cores on crewed ships can't be integrated - story point refunded");
            }
        }
        GammaSupport.snapshotCaptains();
    }

    private static void message(String text) {
        GammaSupport.LOG.info("SSD: " + text);
        Global.getSector().getCampaignUI().addMessage(text, Misc.getHighlightColor());
    }
}
