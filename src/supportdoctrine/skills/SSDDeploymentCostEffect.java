package supportdoctrine.skills;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.SupportDoctrine;
import com.fs.starfarer.api.util.Misc;
import supportdoctrine.GammaSupport;

/**
 * Vanilla SupportDoctrine$Level2 (deployment cost reduction for ships without officers), except a
 * Gamma core supporting a crewed ship doesn't count as an officer.
 */
public class SSDDeploymentCostEffect extends SupportDoctrine.Level2 {

    @Override
    public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
        if (!isNoOfficerOrGamma(stats)) return;

        float baseCost = stats.getSuppliesToRecover().getBaseValue();
        float reduction = Math.min(SupportDoctrine.DP_REDUCTION_MAX, baseCost * SupportDoctrine.DP_REDUCTION);

        FleetMemberAPI member = stats.getFleetMember();
        if (member == null || member.getVariant() == null
                || (!member.getVariant().hasHullMod(HullMods.NEURAL_INTERFACE)
                && !member.getVariant().hasHullMod(HullMods.NEURAL_INTEGRATOR))) {
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD)
                    .modifyFlat(SupportDoctrine.SUPPORT_DOCTRINE_DP_REDUCTION_ID, -reduction);
        }
    }

    private static boolean isNoOfficerOrGamma(MutableShipStatsAPI stats) {
        PersonAPI captain;
        boolean automated;
        if (stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            captain = ship.getCaptain();
            automated = Misc.isAutomated(ship);
        } else {
            FleetMemberAPI member = stats.getFleetMember();
            if (member == null) return true;
            captain = member.getCaptain();
            automated = Misc.isAutomated(member);
        }
        if (captain == null || captain.isDefault()) return true;
        return !automated && GammaSupport.isGamma(captain);
    }
}
