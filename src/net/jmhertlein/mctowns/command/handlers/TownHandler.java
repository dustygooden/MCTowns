package net.jmhertlein.mctowns.command.handlers;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import static net.jmhertlein.core.chat.ChatUtil.*;
import net.jmhertlein.core.command.ECommand;
import net.jmhertlein.mctowns.MCTowns;
import net.jmhertlein.mctowns.banking.BlockBank;
import net.jmhertlein.mctowns.database.TownManager;
import net.jmhertlein.mctowns.permission.Perms;
import net.jmhertlein.mctowns.structure.MCTRegion;
import net.jmhertlein.mctowns.structure.Town;
import net.jmhertlein.mctowns.structure.yaml.YamlMCTRegion;
import net.jmhertlein.mctowns.structure.yaml.YamlTerritory;
import net.jmhertlein.mctowns.structure.yaml.YamlTown;
import net.jmhertlein.mctowns.structure.TownLevel;
import net.jmhertlein.mctowns.townjoin.TownJoinMethod;
import net.jmhertlein.mctowns.townjoin.TownJoinMethodFormatException;
import net.jmhertlein.mctowns.util.WGUtils;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * @author Everdras
 */
public class TownHandler extends CommandHandler {

    public TownHandler(MCTowns parent) {
        super(parent);
    }

    public void setActiveTown(String townName) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        Town t = townManager.getTown(townName);
        if (t == null) {
            localSender.sendMessage(ERR + "The town \"" + townName + "\" does not exist.");
            return;
        }

        if (!localSender.hasExternalPermissions(Perms.ADMIN.toString()) && !t.playerIsMayor(localSender.getPlayer())) {
            localSender.notifyInsufPermissions();
            return;
        }

        localSender.setActiveTown(townManager.getTown(townName));
        localSender.sendMessage("Active town set to " + townName);

    }

    public void resetActiveTown() {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        List<Town> t = townManager.matchPlayerToTowns((Player) localSender.getSender());

        if (t == null) {
            localSender.sendMessage(ERR + "Unable to match you to a town. Are you sure you belong to one?");
            return;
        }

        localSender.setActiveTown(t.get(0));
        localSender.sendMessage(ChatColor.LIGHT_PURPLE + "Active town reset to " + t.get(0).getTownName() + ".");
    }

    public void setTownSpawn() {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        if (!t.playerIsInsideTownBorders(localSender.getPlayer())) {
            localSender.sendMessage(ERR + "You need to be inside your town borders to do that.");
            return;
        }

        t.setSpawn(localSender.getPlayer().getLocation());
        localSender.sendMessage("Town spawn location updated.");
    }

    public void setTownJoinMethod(String s_method) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        TownJoinMethod method;
        try {
            method = TownJoinMethod.parseMethod(s_method);
        } catch (TownJoinMethodFormatException ex) {
            localSender.sendMessage(ERR + ex.getMessage());
            return;
        }

        //TODO: Refactor Town so that it holds a TownJoinMethod instead of a boolean that determines economy joins or invites.
        if (method == TownJoinMethod.ECONOMY) {
            t.setEconomyJoins(true);
        } else if (method == TownJoinMethod.INVITATION) {
            t.setEconomyJoins(false);
        }

        localSender.sendMessage(SUCC + "Town join method updated.");


    }

    public void setTownPlotBuyability(String s_buyability) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        if (!options.isEconomyEnabled()) {
            localSender.sendMessage(ERR + "The economy is not enabled for your server.");
            return;
        }

        boolean buyability;

        try {
            buyability = Boolean.parseBoolean(s_buyability);
        } catch (Exception e) {
            localSender.sendMessage(ERR + "Error in parsing boolean: expected true/false, found " + s_buyability);
            return;
        }

        t.setBuyablePlots(buyability);
        if (buyability) {
            localSender.sendMessage(ChatColor.GOLD + t.getTownName() + "'s plots can now be sold and new plots are buyable by default.");
        } else {
            localSender.sendMessage(ChatColor.GOLD + t.getTownName() + "'s plots are no longer for sale.");
        }


    }

    public void setDefaultPlotPrice(String plotPrice) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }


        BigDecimal price;

        try {
            price = new BigDecimal(plotPrice);
        } catch (NumberFormatException nfe) {
            localSender.sendMessage(ERR + "Error parsing plot price: " + nfe.getMessage());
            return;
        }

        t.setDefaultPlotPrice(price);
        localSender.sendMessage(SUCC + "The default price of plots in " + t.getTownName() + " has been set to " + price);

    }

    public void addTerritorytoTown(String territName) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        boolean autoActive = !cmd.hasFlag(ECommand.DISABLE_AUTOACTIVE);
        boolean admin = cmd.hasFlag(ECommand.ADMIN);
        if (!localSender.hasExternalPermissions(Perms.ADMIN.toString()) && admin) {
            localSender.sendMessage(ChatColor.DARK_RED + "You're not permitted to run this command with administrative priviliges!");
            return;
        }

        if (!(options.mayorsCanBuyTerritories() || admin)) {
            localSender.sendMessage(ChatColor.BLUE + "Mayors are not allowed to add territories. If you're an admin, try adding '-admin' to the end of the command.");
            return;
        }







        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        if (!t.getWorldName().equals(localSender.getPlayer().getWorld().getName())) {
            localSender.sendMessage(ERR + "You're not in the same world as the town, so you can't add Territories to it in this world.");
            return;
        }

        if ((t.getSize() < options.getMinNumPlayersToBuyTerritory()) && !admin) {
            localSender.sendMessage(ERR + "You don't have enough people in your town to buy territories yet.");
            localSender.sendMessage(ERR + "You have " + t.getSize() + " people, but you need a total of " + options.getMinNumPlayersToBuyTerritory() + "!");
            return;
        }


        territName = TownManager.formatRegionName(t, TownLevel.TERRITORY, territName);

        World w = Bukkit.getWorld(t.getWorldName());

        ProtectedRegion region = this.getSelectedRegion(territName);



        if (region == null) {
            localSender.sendMessage(ERR + "No region selected!");
            return;
        }




        //charge the player if they're not running this as an admin and buyable territories is enabled and the price is more than 0
        if (!admin && options.getPricePerXZBlock().compareTo(BigDecimal.ZERO) > 0) {
            MCTowns.logAssert(options.mayorsCanBuyTerritories(), "Mayers are unable to buy territories, but a player has managed to do so.");



            BigDecimal price = options.getPricePerXZBlock().multiply(new BigDecimal(WGUtils.getNumXZBlocksInRegion(region)));

            if (t.getBank().getCurrencyBalance().compareTo(price) < 0) {
                //If they can't afford it...
                localSender.sendMessage(ERR + "There is not enough money in your " + INFO + "town's bank account" + ERR + " to buy a region that large.");
                localSender.sendMessage(ERR + "Total Price: " + price);
                localSender.sendMessage(INFO + "Add money to your town's bank with: /town bank deposit currency <amount>");
                return;
            }

            //otherwise...
            t.getBank().withdrawCurrency(price);

            localSender.sendMessage(ChatColor.GREEN + "Purchase success! Total price was: " + price.toString());
        }

        if (!townManager.addTerritory(territName, w, region, t)) {
            localSender.sendMessage(ERR + "That name is already in use. Please pick a different one.");
            return;
        }

        //IF ALL THE THINGS ARE FINALLY DONE...
        region.getOwners().addPlayer(t.getMayor());

        localSender.sendMessage(SUCC + "Territory added.");

        if (autoActive) {
            localSender.setActiveTerritory(townManager.getTerritory(territName));
            localSender.sendMessage(ChatColor.LIGHT_PURPLE + "Active territory set to newly created territory.");

        }
    }

    public void removeTerritoryFromTown(String territName) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        Town to = localSender.getActiveTown();

        if (to == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        if (!townManager.removeTerritory(territName)) {
            localSender.sendMessage(ERR + "Error: Territory \"" + territName + "\" does not exist and was not removed (because it doesn't exist!)");
        } else {
            localSender.sendMessage(SUCC + "Territory removed.");
        }
    }

    public void invitePlayerToTown(String invitee) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }
        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        if (t.usesEconomyJoins()) {
            localSender.sendMessage(t.getTownName() + " doesn't use the invitation system.");
            return;
        }

        Player p = server.getPlayer(invitee);

        if (!options.playersCanJoinMultipleTowns() && townManager.playerIsAlreadyInATown(invitee)) {
            localSender.sendMessage(ERR + p.getName() + " is already in a town.");
            return;
        }

        if (p == null) {
            localSender.sendMessage(INFO + "\"" + invitee + "\" is not online. The invitation will be sent, but please double-check their name.");
        } else {
            invitee = p.getName(); //let's use that sexy name-completion
        }

        if (joinManager.townHasRequestFromPlayer(t, invitee)) {
            t.addPlayer(invitee);
            if (p != null)
                p.sendMessage("You have joined " + t.getTownName() + "!");
            broadcastTownJoin(t, invitee);
        } else {
            joinManager.invitePlayerToTown(invitee, t);
            localSender.sendMessage(SUCC + (p == null ? invitee : p.getName()) + " has been invited to join " + t.getTownName() + ".");
            if (p != null) {
                p.sendMessage(ChatColor.DARK_GREEN + "You have been invited to join the town " + t.getTownName() + "!");
                p.sendMessage(ChatColor.DARK_GREEN + "To join, type /mct join " + t.getTownName());
            }
        }
    }

    public void promoteToAssistant(String playerName) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }



        if (!(localSender.hasExternalPermissions(Perms.ADMIN.toString()) || t.playerIsMayor(localSender.getPlayer()))) {
            localSender.notifyInsufPermissions();
            return;
        }

        if (server.getPlayer(playerName) == null) {
            localSender.sendMessage(ERR + playerName + " is not online! Make sure you typed their name correctly.");
        }

        if (t.playerIsMayor(playerName)) {
            localSender.sendMessage(ERR + "That player is already the mayor of the town.");
            return;
        }


        if (!t.playerIsResident(playerName)) {
            localSender.sendMessage(ERR + playerName + " is not a resident of " + t.getTownName() + ".");
            return;
        }

        if (t.addAssistant(playerName)) {
            for (MCTRegion reg : townManager.getRegionsCollection()) {
                if (reg instanceof YamlTerritory && ((YamlTerritory) reg).getParentTown().equals(t.getTownName()))
                    ((YamlTerritory) reg).addPlayer(playerName);
            }

            localSender.sendMessage(playerName + " has been promoted to an assistant of " + t.getTownName() + ".");

            if (server.getPlayer(playerName) != null) {
                server.getPlayer(playerName).sendMessage("You are now an Assistant Mayor of " + t.getTownName());
            }
        } else {
            localSender.sendMessage(ERR + playerName + " is already an assistant in this town.");
        }



    }

    public void demoteFromAssistant(String playerName) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        Town t = localSender.getActiveTown();
        Player p = server.getPlayer(playerName);

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }



        if (!(localSender.hasExternalPermissions(Perms.ADMIN.toString()) || t.playerIsMayor(localSender.getPlayer()))) {
            localSender.notifyInsufPermissions();
            return;
        }

        if (p == null) {
            localSender.sendMessage(INFO + playerName + " doesn't exist or is not online, so their name could not be verified for correctness. Proceeding, but make sure you typed the player's name correctly just in case.");
        }


        if (!t.playerIsResident(p)) {
            localSender.sendMessage(ERR + playerName + " is not a resident of " + t.getTownName() + ".");
            return;
        }

        if (t.removeAssistant(p)) {
            localSender.sendMessage(p.getName() + " has been demoted.");
            p.sendMessage(ChatColor.DARK_RED + "You are no longer an assistant mayor for " + t.getTownName());
            YamlTerritory rmFrom;
            for (MCTRegion reg : townManager.getRegionsCollection()) {
                if (reg instanceof YamlTerritory) {
                    rmFrom = (YamlTerritory) reg;
                    if (rmFrom.getParentTown().equals(t.getTownName()))
                        rmFrom.removePlayer(p.getName());
                }
            }
        } else {
            localSender.sendMessage(ERR + p.getName() + " is not an assistant in this town.");
        }
    }

    public void setMayor(String playerName) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        Town t = localSender.getActiveTown();
        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        Player p = server.getPlayerExact(playerName);


        if (!(localSender.hasExternalPermissions("ADMIN") || t.getMayor().equals(localSender.getPlayer().getName()))) {
            localSender.notifyInsufPermissions();
            return;
        }

        if (p == null) {
            localSender.sendMessage(ERR + playerName + " either does not exist or is not online.");
            return;
        }

        if (!t.playerIsResident(p)) {
            localSender.sendMessage(ERR + "That player is not a member of the town.");
            return;
        }

        localSender.getActiveTown().setMayor(p.getName());
        t.broadcastMessageToTown(server, "The mayor of " + t.getTownName() + " is now " + p.getName() + "!");
    }

    public void cancelInvitation(String playerName) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        if (joinManager.clearInvitationForPlayerFromTown(playerName, t)) {
            localSender.sendMessage(ChatColor.GOLD + "The invitation for " + playerName + " has been withdrawn.");
        } else {
            localSender.sendMessage(ERR + playerName + " does not have any pending invitations from " + t.getTownName() + ".");
        }
    }

    public void rejectRequest(String playerName) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        Player p = server.getPlayer(playerName);
        Town t = localSender.getActiveTown();

//        if (p == null) {
//            senderWrapper.sendMessage(ERR + "Player does not exist or is not online.");
//            return;
//        }

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        if (!joinManager.clearRequestForTownFromPlayer(t, (p == null ? playerName : p.getName()))) {
            localSender.sendMessage(ERR + "No matching request found.");
        } else {
            localSender.sendMessage(ChatColor.GOLD + (p == null ? playerName : p.getName()) + "'s request has been rejected.");

            if (p != null) {
                p.sendMessage(ChatColor.DARK_RED + "Your request to join " + t.getTownName() + " has been rejected.");
            }
        }

    }

    public void listRequestsForTown() {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }
        Set<String> playerNames = joinManager.getPlayersRequestingMembershipToTown(t);

        localSender.sendMessage(ChatColor.DARK_BLUE + "There are pending requests from:");

        for (String s : getOutputFriendlyTownJoinListMessages(playerNames)) {
            localSender.sendMessage(ChatColor.YELLOW + s);
        }

    }

    public void listInvitesForTown() {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        Set<String> invitedPlayers = joinManager.getIssuedInvitesForTown(t);

        localSender.sendMessage(ChatColor.DARK_BLUE + "There are pending invites for:");


        for (String s : getOutputFriendlyTownJoinListMessages(invitedPlayers)) {
            localSender.sendMessage(ChatColor.YELLOW + s);
        }



    }

    public void removePlayerFromTown(String playerName) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        Player removeMe = server.getPlayer(playerName);
        Town removeFrom = localSender.getActiveTown();

        if (removeMe == null) {
            localSender.sendMessage(INFO + playerName + " is not online. Make sure you typed their name correctly.");
        }

        if (removeFrom == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        if (removeFrom.playerIsMayor(playerName)) {
            localSender.sendMessage(ERR + "A mayor cannot be removed from his own town.");
            return;
        }

        if (removeFrom.playerIsAssistant(playerName) && !removeFrom.playerIsMayor(localSender.getPlayer())) {
            localSender.sendMessage(ERR + "Only the mayor can remove assistants from the town.");
            return;
        }


        localSender.getActiveTown().removePlayer(playerName);

        YamlTown.recursivelyRemovePlayerFromTown(removeMe, removeFrom);

        localSender.sendMessage("\"" + playerName + "\" was removed from the town.");
        if (removeMe != null) {
            removeMe.sendMessage(ChatColor.DARK_RED + "You have been removed from " + removeFrom.getTownName() + " by " + localSender.getPlayer().getName());
        }
    }

    public void removeSelfFromTown() {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        Town t = localSender.getActiveTown();
        if (t == null) {
            localSender.sendMessage(ERR + "You're either not a member of a town, or your active town isn't set.");
            localSender.sendMessage("To set your active town to your own town, use /town active reset");
            return;
        }

        if (t.playerIsMayor(localSender.getPlayer())) {
            localSender.sendMessage(ERR + "You're the mayor. You need to specify a new mayor before leaving your current town.");
            return;
        }

        t.removePlayer(localSender.getPlayer());


        localSender.sendMessage(ChatColor.DARK_RED + "You have left " + localSender.getActiveTown().getTownName() + ".");
    }

    public void setTownFriendlyFire(String sFriendlyFire) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        boolean friendlyFire;


        friendlyFire = sFriendlyFire.equalsIgnoreCase("on");


        t.setFriendlyFire(friendlyFire);

        localSender.sendMessage(ChatColor.GREEN + "Friendly fire in " + t.getTownName() + " is now " + (friendlyFire ? "on" : "off") + ".");



    }

    public void setMOTD(String motd) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        t.setTownMOTD(motd);
        localSender.sendMessage("Town MOTD has been set.");
    }

    public void printMOTD() {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }


        localSender.sendMessage(t.getTownMOTD());
    }

    public void listResidents(String s_page) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        int i;
        try {
            i = Integer.parseInt(s_page);
        } catch (NumberFormatException ex) {
            localSender.sendMessage(ERR + "Error parsing token \"" + s_page + "\":" + ex.getMessage());
            return;
        }

        listResidents(i);
    }

    public void listResidents(int page) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        page--; //shift to 0-indexing

        if (page < 0) {
            localSender.sendMessage(ERR + "Invalid page.");
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }
        localSender.sendMessage(ChatColor.AQUA + "Players in " + t.getTownName() + "(page " + page + "):");

        String[] players = t.getResidentNames();

        for (int i = page * RESULTS_PER_PAGE; i < players.length && i < page * RESULTS_PER_PAGE + RESULTS_PER_PAGE; i++) {
            localSender.sendMessage(ChatColor.YELLOW + players[i]);
        }
    }

    public void listResidents() {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        listResidents(1);
    }

    public void warpToSpawn() {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasExternalPermissions(Perms.WARP.toString())) {
            localSender.notifyInsufPermissions();
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        localSender.getPlayer().teleport(t.getTownSpawn(server));
        localSender.sendMessage(ChatColor.DARK_GRAY + "Teleported to " + t.getTownName() + "! Welcome!");
    }

    public void warpToOtherSpawn(String townName) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasExternalPermissions(Perms.WARP_FOREIGN.toString())) {
            localSender.notifyInsufPermissions();
            return;
        }

        Town t = townManager.getTown(townName);

        if (t == null) {
            localSender.sendMessage(ERR + "That town doesn't exist.");
            return;
        }

        localSender.getPlayer().teleport(t.getTownSpawn(server));

        localSender.sendMessage(INFO + "Teleported to " + t.getTownName() + "! Welcome!");


    }

    public void checkBlockBank(String blockName) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        Material block = Material.matchMaterial(blockName);

        if (block == null) {
            localSender.sendMessage(ERR + "That block doesn't exist.");
            return;
        }

        int numBlocks = t.getBank().queryBlocks(block.getId());

        localSender.sendMessage(ChatColor.DARK_AQUA + "There are " + (numBlocks == -1 ? "0" : numBlocks) + " blocks of " + blockName + " in the bank.");
    }

    public void withdrawBlockBank(String blockName, String s_quantity) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }
        int quantity;

        try {
            quantity = Integer.parseInt(s_quantity);
        } catch (Exception e) {
            localSender.sendMessage(ERR + "Error on parsing block quantity: not a valid integer.");
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
        }

        if (!t.playerIsInsideTownBorders(localSender.getPlayer()) && !localSender.hasExternalPermissions(Perms.WITHDRAW_BANK_OUTSIDE_BORDERS.toString())) {
            localSender.sendMessage(ERR + "You must be within the borders of your town to withdraw from the bank.");
            return;
        }


        BlockBank bank = t.getBank();
        Material block = Material.matchMaterial(blockName);

        if (block == null) {
            localSender.sendMessage(ERR + blockName + " is not a valid block name.");
            return;
        }

        if (bank.withdrawBlocks(block.getId(), quantity)) {
            Player p = localSender.getPlayer();
            p.getInventory().addItem(new ItemStack(block.getId(), quantity));
            localSender.sendMessage("Blocks withdrawn.");
        } else {
            localSender.sendMessage(ERR + "Number out of valid range. Enter a number between 1 and the number of blocks in your bank.");
        }
    }

    public void depositBlockBank(String blockName, String s_quantity) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        int quantity;

        try {
            quantity = Integer.parseInt(s_quantity);
        } catch (Exception e) {
            localSender.sendMessage(ERR + "Error on parsing block quantity: not a valid integer.");
            return;
        }

        BlockBank bank = t.getBank();
        Material block = Material.matchMaterial(blockName);

        if (block == null) {
            localSender.sendMessage(ERR + blockName + " is not a valid block name.");
            return;
        }

        if (!localSender.getPlayer().getInventory().contains(block.getId(), quantity)) {
            localSender.sendMessage(ERR + "You do not have enough " + blockName + " to deposit that much.");
            return;
        }

        if (bank.depositBlocks(block.getId(), quantity)) {
            Player p = localSender.getPlayer();
            p.getInventory().removeItem(new ItemStack(block.getId(), quantity));
            localSender.sendMessage("Blocks deposited.");
        } else {
            localSender.sendMessage(ERR + "Invalid quantity. Please input a number greater than 0.");
        }

    }

    public void depositHeldItem(String quantity) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        String blockName;

        if (localSender.getPlayer().getItemInHand() == null) {
            localSender.sendMessage(ERR + "There is no item in your hand!");
            return;
        }

        blockName = localSender.getPlayer().getItemInHand().getType().toString();



        this.depositBlockBank(blockName, quantity);
    }

    public void withdrawCurrencyBank(String quantity) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!localSender.hasMayoralPermissions()) {
            localSender.notifyInsufPermissions();
            return;
        }

        if (!options.isEconomyEnabled()) {
            localSender.sendMessage(ERR + "The economy isn't enabled for your server.");
            return;
        }

        BigDecimal amt;
        try {
            amt = new BigDecimal(quantity);
        } catch (NumberFormatException nfe) {
            localSender.sendMessage(ERR + "Error parsing quantity \"" + quantity + "\" : " + nfe.getMessage());
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        //DO the withdrawl from the town bank
        amt = t.getBank().withdrawCurrency(amt);


        economy.depositPlayer(localSender.getPlayer().getName(), amt.doubleValue());
        localSender.sendMessage(amt + " was withdrawn from " + t.getTownName() + "'s town bank and deposited into your account.");


    }

    public void depositCurrencyBank(String quantity) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!options.isEconomyEnabled()) {
            localSender.sendMessage(ERR + "The economy isn't enabled for your server.");
            return;
        }

        BigDecimal amt;
        try {
            amt = new BigDecimal(quantity);
        } catch (NumberFormatException nfe) {
            localSender.sendMessage(ERR + "Error parsing quantity \"" + quantity + "\" : " + nfe.getMessage());
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        EconomyResponse result = economy.withdrawPlayer(localSender.getPlayer().getName(), amt.doubleValue());

        if (result.transactionSuccess()) {
            t.getBank().depositCurrency(amt);
            localSender.sendMessage(quantity + " was withdrawn from your account and deposited into " + t.getTownName() + "'s town bank.");
        } else {
            localSender.sendMessage(ERR + "Transaction failed; maybe you do not have enough money to do this?");
            localSender.sendMessage(ChatColor.GOLD + "Actual amount deposited: " + result.amount);
        }

    }

    public void checkCurrencyBank() {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        if (!options.isEconomyEnabled()) {
            localSender.sendMessage(ERR + "The economy isn't enabled for your server.");
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }

        localSender.sendMessage(ChatColor.BLUE + "Amount of currency in bank: " + t.getBank().getCurrencyBalance());
    }

    public void listTerritories(String s_page) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        int i;
        try {
            i = Integer.parseInt(s_page);
        } catch (NumberFormatException ex) {
            localSender.sendMessage(ERR + "Error parsing token \"" + s_page + "\":" + ex.getMessage());
            return;
        }

        listTerritories(i);
    }

    private void listTerritories(int page) {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        page--; //shift to 0-indexing

        if (page < 0) {
            localSender.sendMessage(ERR + "Invalid page.");
            return;
        }

        Town t = localSender.getActiveTown();

        if (t == null) {
            localSender.notifyActiveTownNotSet();
            return;
        }
        localSender.sendMessage(ChatColor.AQUA + "Existing territories (page " + page + "):");



        String[] territs = t.getTerritoriesCollection().toArray(new String[t.getTerritoriesCollection().size()]);

        for (int i = page * RESULTS_PER_PAGE; i < territs.length && i < page * RESULTS_PER_PAGE + RESULTS_PER_PAGE; i++) {
            localSender.sendMessage(ChatColor.YELLOW + territs[i]);
        }
    }

    public void listTerritories() {
        if (localSender.isConsole()) {
            localSender.notifyConsoleNotSupported();
            return;
        }

        listTerritories(1);
    }
}
