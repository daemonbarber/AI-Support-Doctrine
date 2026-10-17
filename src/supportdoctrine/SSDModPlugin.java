package supportdoctrine;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

public class SSDModPlugin extends BaseModPlugin {

    @Override
    public void onGameLoad(boolean newGame) {
        // Transient: recreated on every load and never written to the save, so the mod stays removable.
        Global.getSector().addTransientScript(new SSDGammaMonitor());
        Global.getSector().unregisterPlugin(SSDGammaCorePlugin.Picker.ID);
        Global.getSector().registerPlugin(new SSDGammaCorePlugin.Picker());

        GammaSupport.resetTracking();
        GammaSupport.clearLegacyTags();
        SSDGammaMonitor.enforce();
    }
}
