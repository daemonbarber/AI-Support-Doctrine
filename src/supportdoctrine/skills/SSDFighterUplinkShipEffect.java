package supportdoctrine.skills;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.FighterUplink;

/** Vanilla FighterUplink$Level1; the officer bonus ignores Gamma cores on crewed ships. */
public class SSDFighterUplinkShipEffect extends FighterUplink.Level1 {

    @Override
    public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
        float crewLossReduction = computeAndCacheThresholdBonus(stats, "fu_crew_loss",
                FighterUplink.CREW_LOSS_PERCENT, ThresholdBonusType.FIGHTER_BAYS);
        if (OfficerChecks.hasOfficerIncludingFighterSource(stats)) crewLossReduction *= FighterUplink.OFFICER_MULT;
        stats.getDynamic().getStat(Stats.FIGHTER_CREW_LOSS_MULT).modifyMult(id, 1f - crewLossReduction / 100f);
    }
}
