package supportdoctrine.skills;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.skills.CarrierGroup;

/** Vanilla CarrierGroup$Level1; the officer bonus ignores Gamma cores on crewed ships. */
public class SSDCarrierGroupEffect extends CarrierGroup.Level1 {

    @Override
    public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
        if (!hasFighterBays(stats)) return;
        boolean officer = OfficerChecks.hasOfficer(stats);

        float rateBonus = computeAndCacheThresholdBonus(stats, "cg_rep_rate",
                CarrierGroup.REPLACEMENT_RATE_PERCENT, ThresholdBonusType.FIGHTER_BAYS);
        if (officer) rateBonus *= CarrierGroup.OFFICER_MULT;
        float timeMult = 1f / ((100f + rateBonus) / 100f);
        stats.getFighterRefitTimeMult().modifyMult(id, timeMult);

        float speedBonus = computeAndCacheThresholdBonus(stats, "cg_top_speed",
                CarrierGroup.TOP_SPEED_PERCENT, ThresholdBonusType.FIGHTER_BAYS);
        if (officer) speedBonus *= CarrierGroup.OFFICER_MULT;
        stats.getMaxSpeed().modifyPercent(id, speedBonus);
    }
}
