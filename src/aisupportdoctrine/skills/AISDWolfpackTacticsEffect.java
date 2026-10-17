package aisupportdoctrine.skills;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.skills.WolfpackTactics;

/** Vanilla WolfpackTactics$Level1A; Gamma cores on crewed ships don't count as officers. */
public class AISDWolfpackTacticsEffect extends WolfpackTactics.Level1A {

    @Override
    public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
        if (!OfficerChecks.hasOfficer(stats)) return;
        boolean frigate;
        boolean destroyer;
        if (stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            frigate = ship.isFrigate();
            destroyer = ship.isDestroyer();
        } else {
            FleetMemberAPI member = stats.getFleetMember();
            frigate = member != null && member.isFrigate();
            destroyer = member != null && member.isDestroyer();
        }
        if (frigate) {
            stats.getDamageToDestroyers().modifyPercent(id, WolfpackTactics.DAMAGE_TO_LARGER_BONUS);
            stats.getDamageToCruisers().modifyPercent(id, WolfpackTactics.DAMAGE_TO_LARGER_BONUS);
            stats.getDamageToCapital().modifyPercent(id, WolfpackTactics.DAMAGE_TO_LARGER_BONUS);
            stats.getPeakCRDuration().modifyPercent(id, WolfpackTactics.PEAK_TIME_BONUS);
        } else if (destroyer) {
            stats.getDamageToCruisers().modifyPercent(id, WolfpackTactics.DAMAGE_TO_LARGER_BONUS_DEST);
            stats.getDamageToCapital().modifyPercent(id, WolfpackTactics.DAMAGE_TO_LARGER_BONUS_DEST);
            stats.getPeakCRDuration().modifyPercent(id, WolfpackTactics.PEAK_TIME_BONUS_DEST);
        }
    }
}
