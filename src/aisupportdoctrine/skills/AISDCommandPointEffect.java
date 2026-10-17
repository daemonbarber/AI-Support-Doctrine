package aisupportdoctrine.skills;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.SupportDoctrine;
import com.fs.starfarer.api.util.Misc;

/**
 * Vanilla SupportDoctrine$Level3 (faster command point recovery unless command was transferred to a
 * ship originally without an officer), except a ship supported by a Gamma core also counts as
 * "without an officer", so commanding from it gives no bonus.
 */
public class AISDCommandPointEffect extends SupportDoctrine.Level3 {

    @Override
    public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
        if (!isOriginalNoOfficerOrGamma(stats)) {
            stats.getDynamic().getMod(Stats.COMMAND_POINT_RATE_FLAT)
                    .modifyFlat(id, SupportDoctrine.COMMAND_POINT_REGEN_PERCENT * 0.01f);
        }
    }

    private static boolean isOriginalNoOfficerOrGamma(MutableShipStatsAPI stats) {
        PersonAPI captain;
        boolean automated;
        if (stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            captain = ship.getOriginalCaptain();
            if (captain == null) return false; // same as vanilla
            automated = Misc.isAutomated(ship);
        } else {
            FleetMemberAPI member = stats.getFleetMember();
            if (member == null) return true;
            captain = member.getCaptain();
            automated = Misc.isAutomated(member);
        }
        if (captain.isDefault()) return true;
        return !automated && captain.isAICore();
    }
}
