package supportdoctrine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;

/** Shared helpers for Gamma cores captaining crewed (non-automated) ships. */
public final class GammaSupport {

    public static final Logger LOG = Global.getLogger(GammaSupport.class);

    public static final String SKILL_ID = "support_doctrine";

    // Tags written by early test builds, which tagged ships as automated. Only used for cleanup.
    private static final String LEGACY_MARKER = "ssd_gamma_tagged";
    private static final String LEGACY_ADDED_AUTOMATED = "ssd_added_automated";
    private static final String LEGACY_ADDED_NO_PENALTY = "ssd_added_no_auto_penalty";

    /**
     * Fleet member id -> captain, as of the last check. Used to finish swaps that vanilla's captain
     * picker only half-does when a Gamma core on a crewed ship is involved.
     */
    private static final Map<String, PersonAPI> lastCaptains = new HashMap<>();

    private GammaSupport() {
    }

    public static CampaignFleetAPI playerFleet() {
        if (Global.getSector() == null) return null;
        return Global.getSector().getPlayerFleet();
    }

    public static boolean playerHasSkill() {
        if (Global.getSector() == null || Global.getSector().getPlayerStats() == null) return false;
        return Global.getSector().getPlayerStats().getSkillLevel(SKILL_ID) > 0;
    }

    public static boolean isGamma(PersonAPI person) {
        return person != null && person.isAICore() && Commodities.GAMMA_CORE.equals(person.getAICoreId());
    }

    /** A crewed ship captained by an AI core (only possible through this mod). */
    public static boolean hasCoreOnCrewedShip(FleetMemberAPI member) {
        return member != null && !member.isFighterWing() && !Misc.isAutomated(member)
                && member.getCaptain() != null && member.getCaptain().isAICore();
    }

    /** Whether the captain picker for this ship should offer Gamma cores. */
    public static boolean canReceiveGamma(FleetMemberAPI member) {
        if (member == null || member.isFighterWing() || Misc.isAutomated(member)) return false;
        return playerHasSkill();
    }

    public static boolean isPlayerShip(FleetMemberAPI member) {
        return member != null && member.getCaptain() != null && member.getCaptain().isPlayer();
    }

    /** Removes the AI core captain from a ship and puts the core back in the player's cargo. */
    public static void returnCoreToCargo(FleetMemberAPI member) {
        PersonAPI captain = member.getCaptain();
        if (captain == null || !captain.isAICore()) return;
        String coreId = captain.getAICoreId();
        member.setCaptain(Global.getFactory().createPerson());
        addCoreToCargo(coreId);
    }

    public static void addCoreToCargo(String coreId) {
        CampaignFleetAPI fleet = playerFleet();
        if (fleet != null && coreId != null) {
            fleet.getCargo().addCommodity(coreId, 1f);
        }
    }

    /** Replaces an integrated (unremovable) Gamma core with a fresh, non-integrated one. */
    public static void resetIntegratedGamma(FleetMemberAPI member) {
        PersonAPI fresh = Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE)
                .createPerson(Commodities.GAMMA_CORE, Factions.PLAYER, null);
        member.setCaptain(fresh);
    }

    /**
     * Finishes captain swaps that vanilla's picker only half-does when a Gamma core on a crewed ship
     * is involved, by comparing against the last snapshot. The picker has two click paths:
     * - AI-core path (a Gamma entry was clicked): the core moves onto the ship and its old ship is
     *   left blank, while the officer or player it displaced is dropped. Put them on the old ship
     *   (the player via setFlagship, as vanilla does when taking command).
     * - Officer path (an officer or the player was clicked): they move onto the Gamma ship and the
     *   core is dropped. Put it on the ship they came from, or refund it if there's no such ship.
     * Unassigning or replacing a core with another core goes through vanilla's AI-core path, which
     * already returns cores to cargo, so those cases are left alone.
     */
    public static void reconcileCaptainChanges() {
        CampaignFleetAPI fleet = playerFleet();
        if (fleet == null || lastCaptains.isEmpty()) return;
        FleetDataAPI data = fleet.getFleetData();
        List<FleetMemberAPI> members = data.getMembersListCopy();
        Set<PersonAPI> assigned = new HashSet<>();
        for (FleetMemberAPI member : members) {
            if (member.getCaptain() != null) assigned.add(member.getCaptain());
        }

        // AI-core path: a Gamma core was pulled onto this ship from another one.
        for (FleetMemberAPI ship : members) {
            PersonAPI now = ship.getCaptain();
            PersonAPI before = lastCaptains.get(ship.getId());
            if (!isGamma(now) || before == now || Misc.isAutomated(ship)) continue;
            if (!isRealCaptain(before) || assigned.contains(before)) continue;
            FleetMemberAPI source = previousShipOf(now, members);
            if (source == null || source == ship || Misc.isAutomated(source)
                    || source.getCaptain() == null || !source.getCaptain().isDefault()) continue;
            if (before.isPlayer()) {
                data.setFlagship(source);
            } else {
                source.setCaptain(before);
            }
            assigned.add(before);
            LOG.info("SSD: swapped " + before.getNameString() + " onto " + source.getShipName());
        }

        // Officer path: an officer or the player replaced the Gamma core on this ship.
        for (FleetMemberAPI ship : members) {
            PersonAPI before = lastCaptains.get(ship.getId());
            PersonAPI now = ship.getCaptain();
            if (!isGamma(before) || before == now || Misc.isAutomated(ship)) continue;
            if (!isRealCaptain(now) || assigned.contains(before)) continue;
            FleetMemberAPI origin = previousShipOf(now, members);
            if (origin != null && origin != ship && !Misc.isAutomated(origin)
                    && origin.getCaptain() != null && origin.getCaptain().isDefault()) {
                origin.setCaptain(before);
                LOG.info("SSD: swapped Gamma core onto " + origin.getShipName());
            } else {
                addCoreToCargo(Commodities.GAMMA_CORE);
                LOG.info("SSD: refunded Gamma core replaced on " + ship.getShipName());
            }
            assigned.add(before);
        }
    }

    /** An officer or the player (not an AI core, not an empty captain slot). */
    private static boolean isRealCaptain(PersonAPI person) {
        return person != null && !person.isAICore() && !person.isDefault();
    }

    /** The ship this person captained at the last snapshot, if it's still in the fleet. */
    private static FleetMemberAPI previousShipOf(PersonAPI person, List<FleetMemberAPI> members) {
        for (FleetMemberAPI member : members) {
            if (lastCaptains.get(member.getId()) == person) return member;
        }
        return null;
    }

    /** Records every ship's captain. */
    public static void snapshotCaptains() {
        lastCaptains.clear();
        CampaignFleetAPI fleet = playerFleet();
        if (fleet == null) return;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            lastCaptains.put(member.getId(), member.getCaptain());
        }
    }

    /** Called on game load; tracking only makes sense within one session. */
    public static void resetTracking() {
        lastCaptains.clear();
    }

    /** Removes automated tags added by early test builds of this mod. */
    public static void clearLegacyTags() {
        CampaignFleetAPI fleet = playerFleet();
        if (fleet == null) return;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            ShipVariantAPI variant = member.getVariant();
            if (variant == null || !variant.hasTag(LEGACY_MARKER)) continue;
            if (variant.hasTag(LEGACY_ADDED_AUTOMATED)) variant.removeTag(Tags.AUTOMATED);
            if (variant.hasTag(LEGACY_ADDED_NO_PENALTY)) variant.removeTag(Tags.TAG_AUTOMATED_NO_PENALTY);
            variant.removeTag(LEGACY_ADDED_AUTOMATED);
            variant.removeTag(LEGACY_ADDED_NO_PENALTY);
            variant.removeTag(LEGACY_MARKER);
            member.updateStats();
        }
    }
}
