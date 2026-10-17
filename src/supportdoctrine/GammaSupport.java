package supportdoctrine;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;

/** Shared helpers for Gamma cores captaining crewed (non-automated) ships. */
public final class GammaSupport {

    public static final Logger LOG = Global.getLogger(GammaSupport.class);

    public static final String SKILL_ID = "support_doctrine";

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

    /** Gamma core persons created by this mod for crewed ships' pickers (not yet assigned). */
    private static final Set<PersonAPI> createdForCrewedShips =
            Collections.newSetFromMap(new WeakHashMap<PersonAPI, Boolean>());

    /** A new Gamma core person, as it should look on a crewed ship (no elite skills). */
    public static PersonAPI createCrewedGamma() {
        PersonAPI gamma = Misc.getAICoreOfficerPlugin(Commodities.GAMMA_CORE)
                .createPerson(Commodities.GAMMA_CORE, Factions.PLAYER, null);
        removeEliteSkills(gamma);
        applyDoctrinePersonality(gamma);
        applyGammaAssistIdentity(gamma);
        createdForCrewedShips.add(gamma);
        return gamma;
    }

    /**
     * Shows the Gamma core's cargo icon as its portrait (instead of the AI officer portrait) and
     * names it after Tri-Tachyon's "Gamma Assist" product, as on the skill's flavor text.
     */
    public static void applyGammaAssistIdentity(PersonAPI person) {
        String icon = Global.getSettings().getCommoditySpec(Commodities.GAMMA_CORE).getIconName();
        if (icon != null && !icon.equals(person.getPortraitSprite())) {
            person.setPortraitSprite(icon);
        }
        if (!CREWED_GAMMA_NAME.equals(person.getNameString())) {
            person.setName(new FullName(CREWED_GAMMA_NAME, "", FullName.Gender.ANY));
        }
    }

    public static final String CREWED_GAMMA_NAME = "Gamma Assist - by Tri-Tachyon";

    /**
     * AI cores are created reckless (shown as "Fearless"). On crewed ships they should behave like
     * the ship's crew would without an officer, i.e. follow the player's doctrine aggression.
     * Returns true if the personality changed.
     */
    public static boolean applyDoctrinePersonality(PersonAPI person) {
        String current = person.getPersonalityAPI() == null ? null : person.getPersonalityAPI().getId();
        String target = doctrinePersonality(current);
        if (target == null || target.equals(current)) return false;
        person.setPersonality(target);
        return true;
    }

    private static String doctrinePersonality(String current) {
        FactionAPI faction = Global.getSector().getPlayerFaction();
        switch (faction.getDoctrine().getAggression()) {
            case 1:
                return Personalities.CAUTIOUS;
            case 2:
                return Personalities.STEADY;
            case 3:
                return Personalities.AGGRESSIVE;
            case 5:
                return Personalities.RECKLESS;
            default:
                // Level 4 is a mix of aggressive and reckless. Like vanilla does for ships without
                // officers, the actual pick happens per battle (SSDCombatPlugin); out of combat the
                // core shows as aggressive so it doesn't change between looks.
                return Personalities.AGGRESSIVE;
        }
    }

    /** True when the doctrine's aggression is the aggressive/reckless mix (level 4). */
    public static boolean isMixedAggression() {
        return Global.getSector() != null && Global.getSector().getPlayerFaction() != null
                && Global.getSector().getPlayerFaction().getDoctrine().getAggression() == 4;
    }

    /** Whether this person is a Gamma core serving (or offered to) a crewed ship. */
    public static boolean isCrewedGamma(PersonAPI person) {
        if (!isGamma(person)) return false;
        if (createdForCrewedShips.contains(person)) return true;
        CampaignFleetAPI fleet = playerFleet();
        return fleet != null && hasCoreOnCrewedShip(fleet.getFleetData().getMemberWithCaptain(person));
    }

    /**
     * Vanilla AI cores have all their skills at elite level. On crewed ships Support Doctrine only
     * provides the regular versions. Returns true if anything changed.
     */
    public static boolean removeEliteSkills(PersonAPI person) {
        boolean changed = false;
        MutableCharacterStatsAPI stats = person.getStats();
        for (MutableCharacterStatsAPI.SkillLevelAPI skill : stats.getSkillsCopy()) {
            if (skill.getSkill().isAptitudeEffect() || skill.getLevel() <= 1f) continue;
            stats.setSkillLevel(skill.getSkill().getId(), 1f);
            changed = true;
        }
        if (changed) stats.refreshCharacterStatsEffects();
        return changed;
    }

    /** Capstone (tier 5) combat skills that Gamma Assists can't have. */
    public static final Set<String> BLOCKED_SKILLS = new HashSet<>(Arrays.asList(
            Skills.SYSTEMS_EXPERTISE, Skills.MISSILE_SPECIALIZATION));

    /** Removes capstone skills; the slot is left free to refill via "Change skills". */
    public static boolean removeBlockedSkills(MutableCharacterStatsAPI stats) {
        boolean changed = false;
        for (String id : BLOCKED_SKILLS) {
            if (stats.getSkillLevel(id) > 0f) {
                stats.setSkillLevel(id, 0f);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Brings every Gamma core on a crewed ship in line with Support Doctrine: no elite or capstone skills, the
     * doctrine's aggression instead of the AI core default, and the core icon as its portrait.
     */
    public static void normalizeCrewedGammas() {
        CampaignFleetAPI fleet = playerFleet();
        if (fleet == null) return;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (!hasCoreOnCrewedShip(member) || !isGamma(member.getCaptain())) continue;
            PersonAPI gamma = member.getCaptain();
            boolean skillsChanged = removeEliteSkills(gamma);
            skillsChanged |= removeBlockedSkills(gamma.getStats());
            applyDoctrinePersonality(gamma);
            applyGammaAssistIdentity(gamma);
            if (skillsChanged) member.updateStats();
        }
    }

    /** Replaces an integrated (unremovable) Gamma core with a fresh, non-integrated one. */
    public static void resetIntegratedGamma(FleetMemberAPI member) {
        member.setCaptain(createCrewedGamma());
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

}
