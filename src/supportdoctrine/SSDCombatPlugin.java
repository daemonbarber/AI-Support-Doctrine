package supportdoctrine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
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

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        rolls.clear();
        try {
            active = GammaSupport.isMixedAggression();
        } catch (Throwable t) {
            active = false; // e.g. missions, where there's no campaign
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (!active || engine == null) return;
        for (ShipAPI ship : engine.getShips()) {
            if (ship.isFighter() || ship.getShipAI() == null) continue;
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
