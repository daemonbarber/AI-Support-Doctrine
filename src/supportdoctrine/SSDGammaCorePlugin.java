package supportdoctrine;

import java.awt.Color;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.PersonalityAPI;
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * The officer tooltip asks the AI core's plugin to draw the personality section, and vanilla's
 * always says "fearless" with the AI core blurb. For Gamma Assists on crewed ships (which follow the
 * doctrine's aggression), show their actual personality like a human officer's tooltip does.
 */
public class SSDGammaCorePlugin extends AICoreOfficerPluginImpl {

    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
        PersonalityAPI personality = person.getPersonalityAPI();
        if (!GammaSupport.isCrewedGamma(person) || personality == null) {
            super.createPersonalitySection(person, tooltip);
            return;
        }
        float opad = 10f;
        Color text = person.getFaction().getBaseUIColor();
        Color bg = person.getFaction().getDarkUIColor();
        tooltip.addSectionHeading("Personality: " + personality.getDisplayName().toLowerCase(),
                text, bg, Alignment.MID, 20);
        tooltip.addPara(personality.getDescription(), opad);
        tooltip.addPara("Gamma Assist follows the %s setting of your fleet doctrine.", opad,
                Misc.getGrayColor(), Misc.getHighlightColor(), "aggression");
        if (GammaSupport.isMixedAggression()) {
            tooltip.addPara("At the current setting, it is %s or %s at random each battle.", opad,
                    Misc.getGrayColor(), Misc.getHighlightColor(), "aggressive", "reckless");
        }
    }

    /** Offers SSDGammaCorePlugin for Gamma cores. Transient, so it's never written to the save. */
    public static class Picker extends BaseCampaignPlugin {
        public static final String ID = "ssd_gamma_core_plugin_picker";

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public boolean isTransient() {
            return true;
        }

        @Override
        public PluginPick<AICoreOfficerPlugin> pickAICoreOfficerPlugin(String commodityId) {
            if (!Commodities.GAMMA_CORE.equals(commodityId)) return null;
            return new PluginPick<AICoreOfficerPlugin>(new SSDGammaCorePlugin(), PickPriority.MOD_SET);
        }
    }
}
