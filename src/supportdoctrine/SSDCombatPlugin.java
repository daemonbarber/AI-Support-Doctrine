package supportdoctrine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

/**
 * At doctrine aggression 4 (the aggressive/reckless mix), each ship supported by a Gamma Assist
 * rolls aggressive or reckless once per battle, using the player faction's personality picker,
 * the way vanilla handles ships without officers. Registered in data/config/settings.json.
 */
public class SSDCombatPlugin extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    private boolean active = false;
    /** The roll for each ship this battle, re-applied if the ship's AI is recreated. */
    private final Map<ShipAPI, String> rolls = new HashMap<>();
    private final Set<String> playerMemberIds = new HashSet<>();

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        rolls.clear();
        playerMemberIds.clear();
        try {
            active = GammaSupport.isMixedAggression();
            if (active) {
                for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
                    playerMemberIds.add(member.getId());
                }
            }
        } catch (Throwable t) {
            active = false; // e.g. missions, where there's no campaign
        }
    }

    /** Only the player's ships follow the player's doctrine (other mods may give NPCs crewed AI cores). */
    private boolean isPlayerShip(ShipAPI ship) {
        if (ship.getOwner() != 0 || ship.isAlly()) return false;
        FleetMemberAPI member = ship.getFleetMember();
        return member != null && playerMemberIds.contains(member.getId());
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!active || engine == null) return;
        for (ShipAPI ship : engine.getShips()) {
            if (ship.isFighter() || ship.getShipAI() == null || !isPlayerShip(ship)) continue;
            if (!GammaSupport.isGamma(ship.getCaptain()) || Misc.isAutomated(ship)) continue;

            ShipAIConfig config = ship.getShipAI().getConfig();
            if (config == null) continue;
            String roll = rolls.get(ship);
            if (roll == null) {
                WeightedRandomPicker<String> picker = Global.getSector().getPlayerFaction().getPersonalityPicker();
                roll = picker.pick();
                rolls.put(ship, roll);
            }
            if (!roll.equals(config.personalityOverride)) {
                config.personalityOverride = roll;
                ship.getShipAI().forceCircumstanceEvaluation();
            }
        }
    }
}
