package com.slaythesector.intel;

import java.awt.Color;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.util.STSConstants;

/**
 * Permanent, important intel entry pointing at the Hub, so the player can find it from the
 * Intel screen. Holds no world reference: the Hub is looked up by id.
 */
public class HubIntel extends BaseIntelPlugin {

    private static final String BUTTON_SHOW = "sts_hub_show";
    private static final String BUTTON_COURSE = "sts_hub_course";
    public static final String TAG = "Slay the Sector";

    public HubIntel() {
        setImportant(true);
    }

    /** Adds the entry once per save. */
    public static void ensure() {
        if (Global.getSector().getIntelManager().hasIntelOfClass(HubIntel.class)) return;
        Global.getSector().getIntelManager().addIntel(new HubIntel(), false);
    }

    private SectorEntityToken hub() {
        return Global.getSector().getEntityById(STSConstants.HUB_ID);
    }

    @Override
    protected String getName() {
        return "Descent Relay";
    }

    @Override
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {
        SectorEntityToken hub = hub();
        if (hub != null && hub.getStarSystem() != null) {
            info.addPara("Orbiting in the %s", initPad, tc, Misc.getHighlightColor(), hub.getStarSystem().getNameWithLowercaseType());
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        float opad = 10f;
        info.addPara("The Descent Relay is where a Run begins. Dock with it to pick a faction and a "
                + "starting fleet, then jump into the Descent. Your own fleet waits at the relay "
                + "until the Run ends.", 0f);
        SectorEntityToken hub = hub();
        if (hub == null) {
            info.addPara("The relay could not be placed in this Sector.", Misc.getNegativeHighlightColor(), opad);
            return;
        }
        if (hub.getStarSystem() != null) {
            info.addPara("Location: %s.", opad, Misc.getHighlightColor(), hub.getStarSystem().getNameWithLowercaseType());
        }
        addGenericButton(info, width, "Show on map", BUTTON_SHOW);
        addGenericButton(info, width, "Lay in course", BUTTON_COURSE);
    }

    @Override
    public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
        SectorEntityToken hub = hub();
        if (hub == null) return;
        if (BUTTON_SHOW.equals(buttonId)) {
            ui.showOnMap(hub);
        } else if (BUTTON_COURSE.equals(buttonId)) {
            Global.getSector().layInCourseFor(hub);
            ui.updateUIForItem(this);
        }
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("intel", "gate_active");
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(TAG);
        return tags;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return hub();
    }

    @Override
    public boolean shouldRemoveIntel() {
        return false;
    }
}
