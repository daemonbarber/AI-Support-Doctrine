package supportdoctrine.skills;

import java.awt.Color;

import com.fs.starfarer.api.characters.DescriptionSkillEffect;
import com.fs.starfarer.api.util.Misc;

public class SSDGammaSupportNotes implements DescriptionSkillEffect {

    public String getString() {
        // Leading newline (as vanilla's Phase Corps note does) moves the note below the flavor text.
        return "\n*Gamma Cores on crewed ships are limited to basic non-elite skills and can not be integrated. "
                + "Ships supported by a Gamma Core do not count as having an officer.";
    }

    public Color[] getHighlightColors() {
        Color h = Misc.getDarkHighlightColor();
        return new Color[] {h, h, h};
    }

    public String[] getHighlights() {
        return new String[] {"basic non-elite skills", "can not be integrated", "do not count as having an officer"};
    }

    public Color getTextColor() {
        return null;
    }
}
