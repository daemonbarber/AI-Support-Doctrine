package supportdoctrine.skills;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import supportdoctrine.GammaSupport;

/**
 * Officer checks used by vanilla skills that give extra bonuses to ships with officers. A Gamma core
 * supporting a crewed ship isn't an officer, so those ships don't get the officer bonuses (AI cores
 * on automated ships still count, as in vanilla).
 */
final class OfficerChecks {

    private OfficerChecks() {
    }

    /** Vanilla's "!captain.isDefault()" check, minus Gamma cores on crewed ships. */
    static boolean hasOfficer(MutableShipStatsAPI stats) {
        if (stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            return hasOfficer(ship.getCaptain(), Misc.isAutomated(ship));
        }
        FleetMemberAPI member = stats.getFleetMember();
        if (member == null) return false;
        return hasOfficer(member.getCaptain(), Misc.isAutomated(member));
    }

    /** As above, but fighters are judged by the ship that launched them (Fighter Uplink). */
    static boolean hasOfficerIncludingFighterSource(MutableShipStatsAPI stats) {
        if (stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            if (ship.isFighter() && ship.getWing() != null && ship.getWing().getSourceShip() != null) {
                ship = ship.getWing().getSourceShip();
            }
            return hasOfficer(ship.getCaptain(), Misc.isAutomated(ship));
        }
        return hasOfficer(stats);
    }

    /** Coordinated Maneuvers only looks at the fleet member, never the combat ship. */
    static boolean memberHasOfficer(MutableShipStatsAPI stats) {
        FleetMemberAPI member = stats.getFleetMember();
        if (member == null) return false;
        return hasOfficer(member.getCaptain(), Misc.isAutomated(member));
    }

    private static boolean hasOfficer(PersonAPI captain, boolean automated) {
        if (captain == null || captain.isDefault()) return false;
        return automated || !GammaSupport.isGamma(captain);
    }
}
