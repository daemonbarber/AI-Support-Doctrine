package supportdoctrine.skills;

import java.awt.Color;

import com.fs.starfarer.api.characters.DescriptionSkillEffect;
import com.fs.starfarer.api.util.Misc;

public class SSDGammaSupportNotes implements DescriptionSkillEffect {

    public String getString() {
        return "*A Gamma core in your cargo appears in the captain list of crewed ships in the Fleet screen. "
                + "Gamma cores on crewed ships can not be integrated, and other AI cores are not supported.";
    }

    public Color[] getHighlightColors() {
        Color h = Misc.getDarkHighlightColor();
        return new Color[] {h};
    }

    public String[] getHighlights() {
        return new String[] {"can not be integrated"};
    }

    public Color getTextColor() {
        return null;
    }
}
