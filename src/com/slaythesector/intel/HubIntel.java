package com.slaythesector.intel;

import java.awt.Color;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin.ListInfoMode;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.slaythesector.util.STSConstants;
import com.slaythesector.util.STSSettings;

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
        return "The Descent awaits (Slay the Sector)";
    }

    @Override
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {
        SectorEntityToken hub = hub();
        if (hub != null && hub.getStarSystem() != null) {
            info.addPara("New combat simulation open at the relay in the %s", initPad, tc,
                    Misc.getHighlightColor(), hub.getStarSystem().getNameWithLowercaseType());
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        float pad = 3f;
        float opad = 10f;
        Color h = Misc.getHighlightColor();
        Color g = Misc.getGrayColor();
        SectorEntityToken hub = hub();
        if (hub == null) {
            info.addPara("The Descent Relay could not be placed in this Sector.", Misc.getNegativeHighlightColor(), 0f);
            return;
        }
        String where = hub.getStarSystem() != null ? hub.getStarSystem().getNameWithLowercaseType() : "Sector";

        info.addPara("Incoming transmission: Descent Relay, %s", 0f, g, h, where);
        info.addPara("\"Captain! Yes, you. Tired of patrol routes and supply bills? Come and try %s, "
                + "the Sector's newest combat simulation.", opad, h, "The Descent");
        info.addPara("Dock with us, plug in, and pick a faction, any faction. We'll load %s of its "
                + "finest hulls under your command. Then down you go, one jump at a time, into a "
                + "battlespace that remembers every fleet ever logged in the Sector.", opad, h,
                STSSettings.startingDP() + " DP");

        info.addSectionHeading("How a Run plays", Alignment.MID, opad);
        info.setBulletedListMode(BaseIntelPlugin.BULLET);
        info.addPara("Choose your path Node by Node. The deeper you go, the harder the fights.", opad);
        info.addPara("Win battles to earn %s: a new ship, an officer, or weapons.", pad, h, "Reward picks");
        info.addPara("%s Spend them on repairs, or earn more by selling ships.", pad, h,
                "Supplies are the only currency.");
        info.addPara("A %s waits at the bottom. No retreat.", pad, h, "Boss");
        info.setBulletedListMode(null);

        info.addPara("And don't worry about your real fleet, Captain. It stays in our docking bay, "
                + "fuelled, crewed and untouched. Every ship you lose down there is just light and "
                + "numbers. Every skill you learn, every hull you win, stays in the sim. We wipe the "
                + "slate the moment you unplug.", opad);
        info.addPara("Only one thing comes back out with you: %s. Our sponsors pay for anyone who goes "
                + "deep enough to be worth watching (Depth %s and below), and %s to anyone who "
                + "beats the Apex at the bottom.", opad, h, "credits",
                String.valueOf(STSSettings.payoutMinDepth()), Misc.getDGSCredits(STSSettings.payoutWin()));
        info.addPara("First Run's on the house. Every Run is, actually.\"", opad);
        info.addPara("End transmission", g, opad);

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
        return Global.getSettings().getSpriteName("intel", "sts_hub");
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
