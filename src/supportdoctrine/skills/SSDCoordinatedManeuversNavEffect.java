package supportdoctrine.skills;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.CoordinatedManeuvers;

/** Vanilla CoordinatedManeuvers$Level1A; Gamma cores on crewed ships don't count as officers. */
public class SSDCoordinatedManeuversNavEffect extends CoordinatedManeuvers.Level1A {

    @Override
    public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
        if (!OfficerChecks.memberHasOfficer(stats)) return;
        float bonus = 0f;
        if (hullSize == HullSize.FRIGATE) bonus = CoordinatedManeuvers.NAV_FRIGATES;
        if (hullSize == HullSize.DESTROYER) bonus = CoordinatedManeuvers.NAV_DESTROYERS;
        if (hullSize == HullSize.CRUISER || hullSize == HullSize.CAPITAL_SHIP) bonus = CoordinatedManeuvers.NAV_OTHER;
        if (bonus > 0f) {
            stats.getDynamic().getMod(Stats.COORDINATED_MANEUVERS_FLAT).modifyFlat(id, bonus);
        }
    }
}
