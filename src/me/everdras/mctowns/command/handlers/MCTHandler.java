/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package me.everdras.mctowns.command.handlers;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.*;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.everdras.mctowns.MCTowns;
import me.everdras.mctowns.command.ActiveSet;
import me.everdras.mctowns.command.MCTCommand;
import me.everdras.mctowns.database.TownManager;
import me.everdras.mctowns.permission.Perms;
import me.everdras.mctowns.structure.*;
import me.everdras.mctowns.townjoin.TownJoinInfoPair;
import me.everdras.mctowns.townjoin.TownJoinManager;
import me.everdras.mctowns.util.Config;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Everdras
 */
public class MCTHandler extends CommandHandler {

    public MCTHandler(MCTowns parent, TownManager t, TownJoinManager j, CommandSender p, HashMap<String, ActiveSet> activeSets, WorldGuardPlugin wg, Economy econ, Config opt, MCTCommand cmd) {
        super(parent, t, j, p, activeSets, wg, econ, opt, cmd);
    }


    public void checkIfRegionIsManagedByMCTowns() {
    }

    public void flagRegion(String flagName, String[] args, TownLevel regionType) {

        MCTownsRegion reg = null;

        switch (regionType) {
            case TOWN:
                senderWrapper.sendMessage(ERR + "Can't apply flags to towns.");
                return;
            case TERRITORY:
                reg = senderWrapper.getActiveTerritory();
                break;
            case DISTRICT:
                reg = senderWrapper.getActiveDistrict();
                break;
            case PLOT:
                reg = senderWrapper.getActivePlot();
                break;
        }

        if (reg == null) {
            senderWrapper.sendMessage(ERR + "Your active " + regionType.toString() + " is not set.");
            return;
        }

        RegionManager regMan = wgp.getRegionManager(server.getWorld(reg.getWorldName()));

        ProtectedRegion wgReg = regMan.getRegion(reg.getName());

        if (wgReg == null) {
            MCTowns.logSevere("Error in CommandHandler.flagRegion(): The region in WG was null (somehow). Perhaps someone manually deleted a region through WorldGuard?");
            senderWrapper.sendMessage(ERR + "An error occurred. Please see the console output for more information. This command exited safely; nothing was changed by it.");
            return;
        }



        Flag<?> foundFlag = null;

        for (Flag<?> flag : DefaultFlag.getFlags()) {
            if (flag.getName().replace("-", "").equalsIgnoreCase(flagName.replace("-", ""))) {
                foundFlag = flag;
                break;
            }
        }

        if (foundFlag == null) {
            senderWrapper.sendMessage(ERR + "Couldn't find a matching flag.");
            return;
        }

        String s_stateOfFlag = "";
        if (args.length == 1) {
            s_stateOfFlag = args[0];
        } else {
            for (String s : args) {
                s_stateOfFlag += s;
                s_stateOfFlag += " ";
            }
            s_stateOfFlag = s_stateOfFlag.substring(0, s_stateOfFlag.length() - 1);
        }

        try {
            setFlag(wgReg, foundFlag, senderWrapper.getSender(), s_stateOfFlag);
        } catch (InvalidFlagFormat ex) {
            senderWrapper.sendMessage(ERR + "Error parsing flag arguments: " + ex.getMessage());
            return;
        }

        senderWrapper.sendMessage(ChatColor.GREEN + "Region successfully flagged.");

        doRegManSave(regMan);

    }

    public void unflagRegion(String flagName, TownLevel regionType) {
        MCTownsRegion reg = null;

        switch (regionType) {
            case TOWN:
                senderWrapper.sendMessage(ERR + "Can't apply flags to towns.");
                return;
            case TERRITORY:
                reg = senderWrapper.getActiveTerritory();
                break;
            case DISTRICT:
                reg = senderWrapper.getActiveDistrict();
                break;
            case PLOT:
                reg = senderWrapper.getActivePlot();
                break;
        }

        if (reg == null) {
            senderWrapper.sendMessage(ERR + "Your active " + regionType.toString() + " is not set.");
            return;
        }

        RegionManager regMan = wgp.getRegionManager(server.getWorld(reg.getWorldName()));

        ProtectedRegion wgReg = regMan.getRegion(reg.getName());

        if (wgReg == null) {
            MCTowns.logSevere("Error in CommandHandler.flagRegion(): The region in WG was null (somehow). Perhaps someone manually deleted a region through WorldGuard?");
            senderWrapper.sendMessage(ERR + "An error occurred. Please see the console output for more information. This command exited safely; nothing was changed by it.");
            return;
        }

        wgReg.setFlag(DefaultFlag.CHEST_ACCESS, StateFlag.State.ALLOW); //this is going to be so annoying

        Flag<?> foundFlag = null;

        for (Flag<?> flag : DefaultFlag.getFlags()) {
            if (flag.getName().replace("-", "").equalsIgnoreCase(flagName.replace("-", ""))) {
                foundFlag = flag;
                break;
            }
        }

        if (foundFlag == null) {
            senderWrapper.sendMessage(ERR + "Couldn't find a matching flag.");
            return;
        }

        wgReg.setFlag(foundFlag, null);

        senderWrapper.sendMessage(ChatColor.GREEN + "Successfully removed flag.");



    }

    public void convertRegionToMCTown(String townName, String regionName, String desiredDistrictName) {
        if (!senderWrapper.hasExternalPermissions(Perms.ADMIN.toString())) {
            senderWrapper.notifyInsufPermissions();
            return;
        }

        Town t = townManager.getTown(townName);

        if (t == null) {
            senderWrapper.sendMessage(townName + " doesn't exist.");
            return;
        }

        RegionManager regMan;
        regMan = wgp.getRegionManager(senderWrapper.getPlayer().getWorld());

        ProtectedRegion parent = regMan.getRegion(regionName);

        if (parent == null) {
            senderWrapper.sendMessage(regionName + " is not an existing region in this world.");
            return;
        }

        Stack<ProtectedRegion> children = new Stack<>();

        Collection<ProtectedRegion> regs = regMan.getRegions().values();


        senderWrapper.sendMessage("MCTowns is now searching through every single WG region that is in this world to find children plots. This may take anywhere from less than a second to over a minute depending on how many regions you have.");
        for (ProtectedRegion protReg : regs) {
            if (protReg.getParent() != null && protReg.getParent().equals(parent)) {
                children.push(protReg);
            }
        }

        Territory nuTerrit = new Territory(t.getTownName() + "_territ_" + parent.getId(), t.getWorldName());


        ProtectedCuboidRegion nuParent = new ProtectedCuboidRegion(nuTerrit.getName(), parent.getMinimumPoint(), parent.getMaximumPoint());
        nuParent.setOwners(parent.getOwners());
        nuParent.setMembers(parent.getMembers());
        nuParent.setFlags(parent.getFlags());
        regMan.addRegion(nuParent);

        District nuDist = new District(t.getTownName() + "_dist_" + desiredDistrictName, t.getWorldName());

        ProtectedCuboidRegion nuDistReg = new ProtectedCuboidRegion(nuDist.getName(), parent.getMinimumPoint(), parent.getMaximumPoint());

        regMan.addRegion(nuDistReg);
        try {
            nuDistReg.setParent(nuParent);
        } catch (ProtectedRegion.CircularInheritanceException ex) {
            ex.printStackTrace(System.err);
        }

        t.addTerritory(nuTerrit);
        nuTerrit.addDistrict(nuDist);

        Plot p = null;
        for (ProtectedRegion plotReg : children) {
            p = new Plot(plotReg.getId(), t.getWorldName());
            nuDist.addPlot(p);

            try {
                plotReg.setParent(nuDistReg);
            } catch (ProtectedRegion.CircularInheritanceException ex) {
                senderWrapper.sendMessage(ChatColor.DARK_RED + "Something bad happened. Please tell everdras that there was a Circular Inheritance Exception in /mct convert");
            }
        }

        regMan.removeRegion(parent.getId());

        try {
            regMan.save();
        } catch (IOException ex) {
            Logger.getLogger("Minecraft").log(Level.SEVERE, "MCTowns was unable to force a save of WG regions for this world. Some changes may not persist.");
        }

        senderWrapper.sendMessage("Done. Your territory should now be set up!");


    }

    public void createTown(String townName, String mayorName) {
        if (!senderWrapper.canCreateTown()) {
            senderWrapper.notifyInsufPermissions();
            return;
        }

        Player nuMayor = server.getPlayer(mayorName);

        if (nuMayor == null) {
            senderWrapper.sendMessage(ERR + mayorName + " doesn't exist or is not online.");
            return;
        }

        if (townManager.matchPlayerToTown(nuMayor) != null) {
            senderWrapper.sendMessage(ERR + nuMayor.getName() + " is already a member of a town, and as such cannot be the mayor of a new one.");
            return;
        }

        if (townManager.addTown(townName, nuMayor)) {
            senderWrapper.sendMessage("Town " + townName + " has been created.");
            server.broadcastMessage(SUCC + "The town " + townName + " has been founded.");
        } else {
            senderWrapper.sendMessage(ERR + "That town already exists!");
        }



    }

    public void removeTown(String townName) {
        if (!senderWrapper.canDeleteTown()) {
            senderWrapper.notifyInsufPermissions();
            return;
        }

        Town t = townManager.getTown(townName);
        if (t == null) {
            senderWrapper.sendMessage(ERR + "The town \"" + townName + "\" does not exist.");
            return;
        }

        townManager.removeTown(wgp, townName);

        try {
            wgp.getRegionManager(server.getWorld(t.getWorldName())).save();
        } catch (IOException ex) {
            MCTowns.logSevere("Error: unable to force a region manager save in WorldGuard. Details:");
            MCTowns.logSevere(ex.getMessage());
        } catch (NullPointerException npe) {
            MCTowns.logSevere("Couldn't force WG to save its regions. (null)");
            MCTowns.logSevere("Debug analysis:");
            MCTowns.logSevere("WG plugin was null: " + (wgp == null));
            MCTowns.logSevere("Server was null: " + (wgp == null));
            MCTowns.logSevere("Town was null: " + (t == null));
            MCTowns.logSevere("Town's world (String) was null in storage: " + (t.getWorldName() == null));
            MCTowns.logSevere("Town's world was null: " + (server.getWorld(t.getWorldName()) == null));
        }

        senderWrapper.sendMessage("Town removed.");
        server.broadcastMessage(ChatColor.DARK_RED + townName + " has been disbanded.");

    }

    public void queryTownInfo(String townName) {
        Town t = townManager.getTown(townName);

        if (t == null) {
            senderWrapper.sendMessage(ERR + "The town \"" + townName + "\" does not exist.");
            return;
        }

        ChatColor c = ChatColor.AQUA;

        senderWrapper.sendMessage(c + "Name: " + t.getTownName());
        senderWrapper.sendMessage(c + "Mayor: " + t.getMayor());
        senderWrapper.sendMessage(c + "World: " + t.getWorldName());
        senderWrapper.sendMessage(c + "Number of residents: " + t.getSize());
        senderWrapper.sendMessage(c + "Plots are buyable: " + t.usesBuyablePlots());
        senderWrapper.sendMessage(c + "Join method: " + (t.usesEconomyJoins() ? "Plot purchase" : "invitations"));



    }

    public void queryPlayerInfo(String playerName) {
        Player p = server.getPlayer(playerName);

        if (p == null && townManager.matchPlayerToTown(playerName) == null) {
            senderWrapper.sendMessage(ERR + "That player is either not online or doesn't exist.");
            return;
        }

        String playerExactName = (p == null ? playerName : p.getName());

        Town t = townManager.matchPlayerToTown(playerExactName);

        if (t == null) {
            senderWrapper.sendMessage("Player: " + playerExactName);
            senderWrapper.sendMessage("Town: None");
            senderWrapper.sendMessage("Is Mayor: n/a");
            senderWrapper.sendMessage("Is Assistant: n/a");
        } else {
            senderWrapper.sendMessage("Player: " + playerExactName);
            senderWrapper.sendMessage("Town: " + t.getTownName());
            senderWrapper.sendMessage("Is Mayor: " + t.getMayor().equals(playerExactName));
            senderWrapper.sendMessage("Is Assistant: " + t.playerIsAssistant(playerExactName));
        }

    }

    public void listTowns() {

        listTowns(1);
    }

    public void listTowns(String page) {
        int intPage = 0;
        try {
            intPage = Integer.parseInt(page);
        } catch (Exception e) {
            senderWrapper.sendMessage(ERR + "Parsing error: <page> is not a valid integer.");
            return;
        }

        this.listTowns(intPage);
    }

    private void listTowns(int page) {
        if (page <= 0) {
            senderWrapper.sendMessage(ERR + "Invalid page.");
            return;
        }
        senderWrapper.sendMessage(ChatColor.AQUA + "Existing towns (page " + page + "):");



        Town[] towns = townManager.getTownsCollection().toArray(new Town[townManager.getTownsCollection().size()]);

        for (int i = page - 1; i < towns.length && i < i + 5; i++) {
            senderWrapper.sendMessage(ChatColor.YELLOW + towns[i].getTownName());
        }


    }

    public void requestAdditionToTown(String townName) {
        if (townManager.playerIsAlreadyInATown(senderWrapper.getPlayer())) {
            senderWrapper.sendMessage(ERR + "You cannot be in more than one town at a time.");
            return;
        }

        Town addTo = townManager.getTown(townName);

        if (addTo == null) {
            senderWrapper.sendMessage(ERR + "\"" + townName + "\" doesn't exist.");
            return;
        }

        if (addTo.usesEconomyJoins()) {
            senderWrapper.sendMessage(addTo.getTownName() + " doesn't use the invitation system.");
            return;
        }

        TownJoinInfoPair infoPair = new TownJoinInfoPair(addTo, senderWrapper.getPlayer());

        if (joinManager.matchRequestToInivteAndDiscard(infoPair)) {
            addTo.addPlayer(senderWrapper.getPlayer());
            senderWrapper.sendMessage("You have joined " + addTo.getTownName() + "!");
            broadcastTownJoin(addTo, senderWrapper.getPlayer());
        } else {
            joinManager.submitRequest(infoPair);
            senderWrapper.sendMessage("You have submitted a request to join " + townName + ".");
            addTo.broadcastMessageToTown(server, senderWrapper.getPlayer().getName() + " has submitted a request to join the town.");

        }
    }

    public void rejectInvitation(String townName) {

        Player p = senderWrapper.getPlayer();
        Town t = townManager.getTown(townName);

        if (t == null) {
            senderWrapper.sendMessage(ERR + "\"" + townName + "\" doesn't exist.");
            return;
        }

        if (!joinManager.removeInvitation(t, p)) {
            senderWrapper.sendMessage(ERR + "No matching invite found.");
        } else {
            senderWrapper.sendMessage(ChatColor.GOLD + "You have rejected the request to join " + townName);

            t.broadcastMessageToTown(server, ERR + p.getName() + " has declined the invitation to join the town.");
        }

    }

    public void rejectAllInvitations() {
        LinkedList<TownJoinInfoPair> invs = joinManager.getInvitesForPlayer(senderWrapper.getPlayer());
        int count = 0;
        for (TownJoinInfoPair tjip : invs) {
            count++;
            joinManager.removeInvitation(townManager.getTown(tjip.getTown()), server.getPlayerExact(tjip.getPlayer()));
        }

        senderWrapper.sendMessage(ChatColor.LIGHT_PURPLE + "All invitations rejected. " + count + " rejected total this sweep.");
    }

    public void cancelRequest(String townName) {
        if (!senderWrapper.hasMayoralPermissions()) {
            senderWrapper.notifyInsufPermissions();
            return;
        }

        Town t = townManager.getTown(townName);

        if (t == null) {
            senderWrapper.sendMessage(ERR + "That town doesn't exist.");
            return;
        }

        if (joinManager.removeRequest(t, senderWrapper.getPlayer())) {
            senderWrapper.sendMessage(ChatColor.GOLD + "You have withdrawn your request to join " + t.getTownName() + ".");
        } else {
            senderWrapper.sendMessage(ERR + "You haven't submitted a request to join " + t.getTownName() + ".");
        }

    }

    public void listInvitesForPlayer() {
        LinkedList<TownJoinInfoPair> invs = joinManager.getInvitesForPlayer(senderWrapper.getPlayer());

        senderWrapper.sendMessage(ChatColor.DARK_BLUE + "There are pending invites from the following towns:");


        for (String s : getOutputFriendlyTownJoinListMessages(false, invs)) {
            senderWrapper.sendMessage(ChatColor.YELLOW + s);
        }

    }

    public void listRequestsForPlayer() {
        LinkedList<TownJoinInfoPair> reqs = joinManager.getRequestsForPlayer(senderWrapper.getPlayer());

        senderWrapper.sendMessage(ChatColor.DARK_BLUE + "You have requested to join the following towns:");

        for (String s : getOutputFriendlyTownJoinListMessages(false, reqs)) {
            senderWrapper.sendMessage(ChatColor.YELLOW + s);
        }
    }

    public void checkPendingInvites() {
        LinkedList<TownJoinInfoPair> invs = joinManager.getInvitesForPlayer(senderWrapper.getPlayer());

        senderWrapper.sendMessage(ChatColor.BLUE + "You have pending invitations from:");
        String temp = "";
        for (TownJoinInfoPair tjip : invs) {
            temp += tjip.getTown();
            temp += " ";
        }
        temp = ChatColor.AQUA + temp;

        senderWrapper.sendMessage(temp);
        senderWrapper.sendMessage(ChatColor.BLUE + "Type /mct join <townname> to join one, or /mct refuse <town name> to refuse.");
    }
}