package supportdoctrine.skills;

import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

/**
 * Replaces vanilla SupportDoctrine$Level1. Has no stat effect; the actual behavior lives in
 * SSDCoreUIListener / SSDGammaMonitor, which check whether the player has this skill.
 */
public class SSDGammaSupportEffect implements ShipSkillEffect {

    public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
    }

    public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
    }

    public String getEffectDescription(float level) {
        return "Ships without officers can be supported by Gamma Cores";
    }

    public String getEffectPerLevelDescription() {
        return null;
    }

    public ScopeDescription getScopeDescription() {
        return ScopeDescription.PILOTED_SHIP;
    }
}
