package aisupportdoctrine;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

public class AISDModPlugin extends BaseModPlugin {

    @Override
    public void onGameLoad(boolean newGame) {
        AISDSettings.reload();
        // Transient: recreated on every load and never written to the save, so the mod stays removable.
        Global.getSector().addTransientScript(new AISDGammaMonitor());
        Global.getSector().unregisterPlugin(AISDGammaCorePlugin.Picker.ID);
        Global.getSector().registerPlugin(new AISDGammaCorePlugin.Picker());

        AISDGammaMonitor.enforce();
    }
}
