package supportdoctrine.skills;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.skills.FighterUplink;

/** Vanilla FighterUplink$Level2; the officer bonus ignores Gamma cores on crewed carriers. */
public class SSDFighterUplinkFighterEffect extends FighterUplink.Level2 {

    @Override
    public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
        boolean officer = OfficerChecks.hasOfficerIncludingFighterSource(stats);

        float speedBonus = getMaxSpeedBonus(stats);
        if (officer) speedBonus *= FighterUplink.OFFICER_MULT;
        stats.getMaxSpeed().modifyPercent(id, speedBonus);
        stats.getAcceleration().modifyPercent(id, speedBonus * 2f);
        stats.getDeceleration().modifyPercent(id, speedBonus * 2f);

        float aimBonus = getAimBonus(stats);
        if (officer) aimBonus *= FighterUplink.OFFICER_MULT;
        stats.getAutofireAimAccuracy().modifyFlat(id, aimBonus * 0.01f);

        float damBonus = getDamageBonus(stats);
        if (officer) damBonus *= FighterUplink.OFFICER_MULT;
        stats.getBallisticWeaponDamageMult().modifyPercent(id, damBonus);
        stats.getEnergyWeaponDamageMult().modifyPercent(id, damBonus);
        stats.getMissileWeaponDamageMult().modifyPercent(id, damBonus);
    }
}
