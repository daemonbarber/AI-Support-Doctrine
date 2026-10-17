package supportdoctrine.skills;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.characters.DescriptionSkillEffect;
import com.fs.starfarer.api.util.Misc;
import supportdoctrine.SSDSettings;

/** The skill's footnote. Built from the mod settings, so it only lists the rules that apply. */
public class SSDGammaSupportNotes implements DescriptionSkillEffect {

    private static final String NON_ELITE_BASIC = "basic non-elite skills";
    private static final String NON_ELITE = "non-elite skills";
    private static final String NO_CAPSTONES = "can not take capstone skills";
    private static final String NO_INTEGRATION = "can not be integrated";
    private static final String NOT_OFFICER = "do not count as having an officer";

    public String getString() {
        List<String> rules = new ArrayList<>();
        if (SSDSettings.nonEliteOnly) {
            rules.add("are limited to " + (SSDSettings.blockCapstones ? NON_ELITE_BASIC : NON_ELITE));
        } else if (SSDSettings.blockCapstones) {
            rules.add(NO_CAPSTONES);
        }
        rules.add(NO_INTEGRATION);

        // Leading newline (as vanilla's Phase Corps note does) moves the note below the flavor text.
        StringBuilder text = new StringBuilder("\n*");
        if (!rules.isEmpty()) {
            text.append(SSDSettings.allowedCoresText()).append(" on crewed ships ")
                    .append(String.join(" and ", rules)).append(". ");
        }
        if (!SSDSettings.countsAsOfficer) {
            text.append("Ships supported by ").append(SSDSettings.singleCoreText()).append(" ")
                    .append(NOT_OFFICER).append(".");
        }
        String result = text.toString().trim();
        return result.equals("*") ? "" : "\n" + result;
    }

    public Color[] getHighlightColors() {
        String[] highlights = getHighlights();
        Color[] colors = new Color[highlights.length];
        for (int i = 0; i < colors.length; i++) colors[i] = Misc.getDarkHighlightColor();
        return colors;
    }

    public String[] getHighlights() {
        List<String> highlights = new ArrayList<>();
        if (SSDSettings.nonEliteOnly) {
            highlights.add(SSDSettings.blockCapstones ? NON_ELITE_BASIC : NON_ELITE);
        } else if (SSDSettings.blockCapstones) {
            highlights.add(NO_CAPSTONES);
        }
        highlights.add(NO_INTEGRATION);
        if (!SSDSettings.countsAsOfficer) highlights.add(NOT_OFFICER);
        return highlights.toArray(new String[0]);
    }

    public Color getTextColor() {
        return null;
    }
}
