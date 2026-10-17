package supportdoctrine;

import java.util.HashSet;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.util.Misc;

/**
 * Battle difficulty (the post-battle XP multiplier) counts officers as extra strength on each side.
 * A Gamma core supporting a crewed ship isn't an officer, so we compute the difficulty ourselves,
 * a copy of FleetEncounterContext.computeBattleDifficulty() with that one change, and hand it to
 * the encounter before vanilla computes its own (vanilla only computes it if not already set).
 */
final class BattleDifficulty {

    private BattleDifficulty() {
    }

    /** Called every frame while an interaction dialog is open. */
    static void apply(InteractionDialogAPI dialog) {
        if (dialog == null || !(dialog.getPlugin() instanceof FleetInteractionDialogPluginImpl)) return;
        Object context = ((FleetInteractionDialogPluginImpl) dialog.getPlugin()).getContext();
        if (!(context instanceof FleetEncounterContext)) return;
        FleetEncounterContext encounter = (FleetEncounterContext) context;
        if (encounter.isComputedDifficulty()) return;
        BattleAPI battle = encounter.getBattle();
        if (battle == null || !battle.isPlayerInvolved()) return;

        encounter.setDifficulty(compute(battle));
        encounter.setComputedDifficulty(true);
    }

    private static boolean isOfficer(FleetMemberAPI member) {
        PersonAPI captain = member.getCaptain();
        if (captain == null || captain.isDefault()) return false;
        return Misc.isAutomated(member) || !captain.isAICore() || SSDSettings.countsAsOfficer;
    }

    private static float compute(BattleAPI battle) {
        float scorePlayer = 0f;
        float scoreEnemy = 0f;

        float officerBase = 30;
        float officerPerLevel = 15;
        float baseMult = 2f;
        float dModMult = 0.9f;

        for (FleetMemberAPI member : battle.getNonPlayerCombined().getFleetData().getMembersListCopy()) {
            if (member.isMothballed()) continue;
            float mult = baseMult;
            if (member.isCivilian()) mult *= 0.25f;
            if (isOfficer(member)) {
                scoreEnemy += officerBase + officerPerLevel * Math.max(1f, member.getCaptain().getStats().getLevel());
            }
            int dMods = DModManager.getNumDMods(member.getVariant());
            for (int i = 0; i < dMods; i++) {
                mult *= dModMult;
            }
            scoreEnemy += member.getFleetPointCost() * mult;
        }
        scoreEnemy *= 0.6f;

        float maxPlayerShipScore = 0f;

        officerBase *= 0.5f;
        officerPerLevel *= 0.5f;
        Set<PersonAPI> seenOfficers = new HashSet<>();
        int unofficeredShips = 0;
        for (FleetMemberAPI member : battle.getPlayerCombined().getFleetData().getMembersListCopy()) {
            if (member.isMothballed()) continue;
            float mult = baseMult;
            if (member.isCivilian()) mult *= 0.25f;
            if (isOfficer(member)) {
                scorePlayer += officerBase + officerPerLevel * Math.max(1f, member.getCaptain().getStats().getLevel());
                seenOfficers.add(member.getCaptain());
            } else if (!member.isCivilian()) {
                unofficeredShips++;
            }
            int dMods = DModManager.getNumDMods(member.getVariant());
            for (int i = 0; i < dMods; i++) {
                mult *= dModMult;
            }
            float currShipBaseScore = member.getFleetPointCost() * mult;
            scorePlayer += currShipBaseScore;
            if (battle.getSourceFleet(member) != null && battle.getSourceFleet(member).isPlayerFleet()) {
                maxPlayerShipScore = Math.max(maxPlayerShipScore, currShipBaseScore);
            }
        }

        // Vanilla: unassigned officers still count (up to the number of unofficered ships), so that
        // unassigning officers before a fight doesn't raise the XP gained.
        for (OfficerDataAPI od : Global.getSector().getPlayerFleet().getFleetData().getOfficersCopy()) {
            if (seenOfficers.contains(od.getPerson())) continue;
            if (od.getPerson().isPlayer()) continue;
            if (unofficeredShips <= 0) break;
            unofficeredShips--;
            scorePlayer += officerBase + officerPerLevel * Math.max(1f, od.getPerson().getStats().getLevel());
        }

        scorePlayer = Math.max(scorePlayer, Math.min(scoreEnemy * 0.5f, maxPlayerShipScore * 6f));

        if (scorePlayer < 1) scorePlayer = 1;
        if (scoreEnemy < 1) scoreEnemy = 1;

        float difficulty = scoreEnemy / scorePlayer;
        if (difficulty < 0) difficulty = 0;
        if (difficulty > FleetEncounterContext.MAX_XP_MULT) difficulty = FleetEncounterContext.MAX_XP_MULT;
        return difficulty;
    }
}
