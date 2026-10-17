package supportdoctrine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;

/**
 * Mod options. The defaults are the mod's intended behavior; if LunaLib is installed, the values
 * come from its settings menu (data/config/LunaSettings.csv). LunaLib is optional, so all of its
 * classes are only touched from SSDLunaBridge, and only when LunaLib is enabled.
 */
public final class SSDSettings {

    public static final String MOD_ID = "ssd_gamma_support";
    private static final String LUNALIB_ID = "lunalib";

    public static final String CORES_GAMMA = "Gamma only";
    // Must match the Radio options in LunaSettings.csv (which are comma-separated, so no commas here).
    public static final String CORES_GAMMA_BETA = "Gamma + Beta";
    public static final String CORES_ALL = "Gamma + Beta + Alpha";

    /** Which AI cores can support crewed ships: one of the CORES_* values. */
    public static String allowedCores = CORES_GAMMA;
    /** Gamma Assists only get regular (non-elite) skills. */
    public static boolean nonEliteOnly = true;
    /** Gamma Assists can't take Systems Expertise or Missile Specialization. */
    public static boolean blockCapstones = true;
    /** Gamma Assists count as officers for other skills and battle difficulty. */
    public static boolean countsAsOfficer = false;

    private static boolean listenerRegistered = false;

    private SSDSettings() {
    }

    /** Reloads the settings from LunaLib, if it's enabled. Safe to call any time. */
    public static void reload() {
        if (!isLunaLibEnabled()) return;
        try {
            SSDLunaBridge.load();
            if (!listenerRegistered) {
                SSDLunaBridge.registerListener();
                listenerRegistered = true;
            }
        } catch (Throwable t) {
            GammaSupport.LOG.warn("SSD: couldn't read LunaLib settings; using defaults", t);
        }
    }

    /** AI core commodity ids allowed to support crewed ships, weakest first. */
    public static List<String> allowedCoreIds() {
        if (CORES_ALL.equals(allowedCores)) {
            return Arrays.asList(Commodities.GAMMA_CORE, Commodities.BETA_CORE, Commodities.ALPHA_CORE);
        }
        if (CORES_GAMMA_BETA.equals(allowedCores)) {
            return Arrays.asList(Commodities.GAMMA_CORE, Commodities.BETA_CORE);
        }
        return Collections.singletonList(Commodities.GAMMA_CORE);
    }

    public static boolean isAllowedCore(String coreId) {
        return coreId != null && allowedCoreIds().contains(coreId);
    }

    /** For text: "Gamma Cores", "Gamma and Beta Cores", "Gamma, Beta and Alpha Cores". */
    public static String allowedCoresText() {
        if (CORES_ALL.equals(allowedCores)) return "Gamma, Beta and Alpha Cores";
        if (CORES_GAMMA_BETA.equals(allowedCores)) return "Gamma and Beta Cores";
        return "Gamma Cores";
    }

    /** For text: "a Gamma Core" when only Gammas are allowed, otherwise "an AI Core". */
    public static String singleCoreText() {
        return CORES_GAMMA.equals(allowedCores) ? "a Gamma Core" : "an AI Core";
    }

    private static boolean isLunaLibEnabled() {
        return Global.getSettings().getModManager().isModEnabled(LUNALIB_ID);
    }
}
