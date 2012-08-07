package net.jmhertlein.mctowns.command.handlers;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.logging.Level;
import java.util.logging.Logger;
import static net.jmhertlein.core.chat.ChatUtil.*;
import net.jmhertlein.core.command.ECommand;
import net.jmhertlein.mctowns.MCTowns;
import net.jmhertlein.mctowns.database.TownManager;
import net.jmhertlein.mctowns.structure.Plot;
import net.jmhertlein.mctowns.structure.Territory;
import net.jmhertlein.mctowns.structure.Town;
import net.jmhertlein.mctowns.structure.TownLevel;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * @author Everdras
 */
public class TerritoryHandler extends CommandHandler {

    public TerritoryHandler(MCTowns parent) {
        super(parent);
    }

    public void addPlotToTerritory(String plotName) {
        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        boolean autoActive = !cmd.hasFlag(ECommand.DISABLE_AUTOACTIVE);

        plotName = localSender.getActiveTown().getTownName() + TownLevel.PLOT_INFIX + plotName;

        String worldName = localSender.getActiveTown().getWorldName();
        Plot p = new Plot(plotName, worldName);
        Territory parTerr = localSender.getActiveTerritory();

        if (parTerr == null) {
            localSender.notifyActiveTerritoryNotSet();
            return;
        }



        ProtectedRegion region = getSelectedRegion(p.getName());

        if (region == null) {
            return;
        }

        if (!this.selectionIsWithinParent(region, localSender.getActiveTerritory())) {
            localSender.sendMessage(ERR + "Selection is not in territory!");
            return;
        }


        ProtectedRegion parent = wgp.getRegionManager(wgp.getServer().getWorld(worldName)).getRegion(localSender.getActiveTerritory().getName());
        try {
            region.setParent(parent);
        } catch (ProtectedRegion.CircularInheritanceException ex) {
            Logger.getLogger("Minecraft").log(Level.WARNING, "Circular Inheritence in addPlotToTerritory.");
        }
        RegionManager regMan = wgp.getRegionManager(wgp.getServer().getWorld(worldName));

        if (regMan.hasRegion(plotName)) {
            localSender.sendMessage(ERR + "That name is already in use. Please pick a different one.");
            return;
        }

        regMan.addRegion(region);

        parTerr.addPlot(p);

        localSender.sendMessage("Plot added.");

        doRegManSave(regMan);

        if (autoActive) {
            localSender.setActivePlot(p);
            localSender.sendMessage(INFO + "Active plot set to newly created plot.");

        }

    }

    public void removePlotFromTerritory(String plotName) {
        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        Territory t = localSender.getActiveTerritory();

        if (t == null) {
            localSender.notifyActiveTerritoryNotSet();
            return;
        }

        Plot removeMe = t.getPlot(plotName);

        if (removeMe == null) {
            localSender.sendMessage(ERR + "That plot doesn't exist. Make sure you're using the full name of the district (townname_district_districtshortname).");
            return;
        }

        TownManager.removePlot(t, plotName);
        localSender.sendMessage(SUCC + "Plot removed.");
    }

    public void addPlayerToTerritory(String playerName) {
        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        Territory territ = localSender.getActiveTerritory();
        Player player = server.getPlayer(playerName);

        if (player == null) {
            localSender.sendMessage(ChatColor.YELLOW + playerName + " is not online. Make sure you typed their name correctly!");
        }

        if (!localSender.getActiveTown().playerIsResident(player)) {
            localSender.sendMessage(ERR + "That player is not a member of the town.");
            return;
        }

        if (territ == null) {
            localSender.notifyActiveTerritoryNotSet();
            return;
        }

        if (territ.addPlayer(playerName)) {
            localSender.sendMessage("Player added to territory.");
        } else {
            localSender.sendMessage(ERR + "That player is already in that territory.");
        }
    }

    public void removePlayerFromTerritory(String player) {
        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        boolean recursive = cmd.hasFlag(ECommand.RECURSIVE);

        Territory territ = localSender.getActiveTerritory();

        if (territ == null) {
            localSender.notifyActiveTerritoryNotSet();
            return;
        }

        if (player == null) {
            localSender.sendMessage(ERR + "That player is not online.");
            return;
        }

        if (recursive) {
            if (!territ.removePlayer(player)) {
                localSender.sendMessage(ERR + "That player is not in this territory.");
                return;
            }

            for (Plot p : territ.getPlotsCollection()) {
                p.removePlayer(player);
            }

            localSender.sendMessage("Player removed from territory.");

        } else {
            if (!territ.removePlayer(player)) {
                localSender.sendMessage(ERR + "That player is not in this territory.");
                return;
            }
            localSender.sendMessage("Player removed from territory.");
        }
    }

    public void setActiveTerritory(String territName) {
        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }



        Territory nuActive = t.getTerritory(territName);

        if (nuActive == null) {
            nuActive = t.getTerritory((t.getTownName() + TownLevel.TERRITORY_INFIX + territName).toLowerCase());
        }

        if (nuActive == null) {
            localSender.sendMessage(ERR + "The territory \"" + territName + "\" does not exist.");
            return;
        }

        localSender.setActiveTerritory(nuActive);
        localSender.sendMessage("Active territory set to " + nuActive.getName());
    }

    private void listPlots(int page) {
        page--; //shift to 0-indexing

        if (page < 0) {
            localSender.sendMessage(ERR + "Invalid page.");
            return;
        }

        Territory t = localSender.getActiveTerritory();

        if (t == null) {
            localSender.notifyActiveTerritoryNotSet();
            return;
        }
        localSender.sendMessage(ChatColor.AQUA + "Existing plots (page " + page + "):");



        Plot[] plots = t.getPlotsCollection().toArray(new Plot[t.getPlotsCollection().size()]);

        for (int i = page * RESULTS_PER_PAGE; i < plots.length && i < page * RESULTS_PER_PAGE + RESULTS_PER_PAGE; i++) {
            localSender.sendMessage(ChatColor.YELLOW + plots[i].getName());
        }
    }

    public void listPlots(String s_page) {
        int page;
        try {
            page = Integer.parseInt(s_page);
        } catch (NumberFormatException nfex) {
            localSender.sendMessage(ERR + "Error parsing integer argument. Found \"" + s_page + "\", expected integer.");
            return;
        }

        listPlots(page);
    }

    public void listPlots() {
        listPlots(1);
    }
}