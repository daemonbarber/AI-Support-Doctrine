package supportdoctrine;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

/** The only class that references LunaLib. Only loaded when LunaLib is enabled (see SSDSettings). */
final class SSDLunaBridge {

    private SSDLunaBridge() {
    }

    static void load() {
        String cores = LunaSettings.getString(SSDSettings.MOD_ID, "ssd_allowedCores");
        if (cores != null) SSDSettings.allowedCores = cores.trim();
        SSDSettings.nonEliteOnly = get("ssd_nonEliteOnly", SSDSettings.nonEliteOnly);
        SSDSettings.blockCapstones = get("ssd_blockCapstones", SSDSettings.blockCapstones);
        SSDSettings.countsAsOfficer = get("ssd_countsAsOfficer", SSDSettings.countsAsOfficer);
    }

    static void registerListener() {
        LunaSettings.addSettingsListener(new LunaSettingsListener() {
            @Override
            public void settingsChanged(String modId) {
                if (SSDSettings.MOD_ID.equals(modId)) load();
            }
        });
    }

    private static boolean get(String field, boolean fallback) {
        Boolean value = LunaSettings.getBoolean(SSDSettings.MOD_ID, field);
        return value == null ? fallback : value;
    }
}
