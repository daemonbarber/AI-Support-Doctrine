package aisupportdoctrine;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

/** The only class that references LunaLib. Only loaded when LunaLib is enabled (see AISDSettings). */
final class AISDLunaBridge {

    private AISDLunaBridge() {
    }

    static void load() {
        String cores = LunaSettings.getString(AISDSettings.MOD_ID, "aisd_allowedCores");
        if (cores != null) AISDSettings.allowedCores = cores.trim();
        AISDSettings.nonEliteOnly = get("aisd_nonEliteOnly", AISDSettings.nonEliteOnly);
        AISDSettings.blockCapstones = get("aisd_blockCapstones", AISDSettings.blockCapstones);
        AISDSettings.countsAsOfficer = get("aisd_countsAsOfficer", AISDSettings.countsAsOfficer);
    }

    static void registerListener() {
        LunaSettings.addSettingsListener(new LunaSettingsListener() {
            @Override
            public void settingsChanged(String modId) {
                if (AISDSettings.MOD_ID.equals(modId)) load();
            }
        });
    }

    private static boolean get(String field, boolean fallback) {
        Boolean value = LunaSettings.getBoolean(AISDSettings.MOD_ID, field);
        return value == null ? fallback : value;
    }
}
