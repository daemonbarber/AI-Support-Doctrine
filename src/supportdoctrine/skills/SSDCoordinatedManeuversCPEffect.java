package supportdoctrine.skills;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.CoordinatedManeuvers;

/** Vanilla CoordinatedManeuvers$Level1C; Gamma cores on crewed ships don't count as officers. */
public class SSDCoordinatedManeuversCPEffect extends CoordinatedManeuvers.Level1C {

    @Override
    public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
        if (!OfficerChecks.memberHasOfficer(stats)) return;
        float bonus = 0f;
        if (hullSize == HullSize.FRIGATE) bonus = CoordinatedManeuvers.CP_REGEN_FRIGATES;
        if (hullSize == HullSize.DESTROYER) bonus = CoordinatedManeuvers.CP_REGEN_DESTROYERS;
        if (bonus > 0f) {
            stats.getDynamic().getMod(Stats.COMMAND_POINT_RATE_FLAT).modifyFlat(id, bonus * 0.01f);
        }
    }
}
