package net.jmhertlein.mctowns.structure.yaml;

import net.jmhertlein.mctowns.structure.Town;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import net.jmhertlein.core.location.Location;
import net.jmhertlein.mctowns.MCTowns;
import net.jmhertlein.mctowns.banking.BlockBank;
import net.jmhertlein.mctowns.database.YamlTownManager;
import net.jmhertlein.mctowns.structure.MCTRegion;
import net.jmhertlein.mctowns.structure.Territory;
import net.jmhertlein.mctowns.structure.factory.YamlMCTFactory;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 *
 * @author joshua
 */
public class YamlTown implements Town {
    private static final long serialVersionUID = "TOWN".hashCode(); // DO NOT CHANGE
    private static final int VERSION = 0;
    //the town name
    private String townName;
    private String worldName;
    //the town MOTD
    private String townMOTD;
    private ChatColor motdColor;
    //town spawn point
    private Location townSpawn;
    //town bank
    private BlockBank bank;
    //the territories associated with it
    private HashSet<String> territories;
    //the players in it
    private HashSet<String> residents;
    //its mayor (string)
    private String mayor;
    //the assistants (strings)
    private HashSet<String> assistants;
    //whether or not plots are buyable and thus have a price
    private boolean buyablePlots;
    //turns off the join request/invitation system. Instead, buying a plot in
    //the town makes you a member. Also, if this is false, you need to be a
    //member of the town in order to buy plots.
    private boolean economyJoins;
    private BigDecimal defaultPlotPrice;
    private boolean friendlyFire;
    
    private transient File saveLocation;

    /**
     * Creates a new town, setting the name to townName, the mayor to the player
     * passed as mayor, and adds the mayor to the list of residents. The MOTD is
     * set to a default motd.
     *
     * @param townName the desired name of the town
     * @param mayor the player to be made the mayor of the town
     *
     */
    public YamlTown(String townName, Player mayor, File saveLocation) {
        this.townName = townName;
        this.mayor = mayor.getName();
        townMOTD = "Use /town motd set <msg> to set the town MOTD!";
        townSpawn = Location.convertFromBukkitLocation(mayor.getLocation());
        worldName = mayor.getWorld().getName();

        bank = new BlockBank();

        residents = new HashSet<>();
        assistants = new HashSet<>();
        territories = new HashSet<>();

        buyablePlots = false;
        economyJoins = false;
        defaultPlotPrice = BigDecimal.TEN;
        friendlyFire = false;



        residents.add(mayor.getName());

        motdColor = ChatColor.GOLD;
        
        this.saveLocation = saveLocation;
    }

    private YamlTown() {}

    /**
     *
     * @return
     */
    @Override
    public BlockBank getBank() {
        return bank;
    }

    /**
     * Sets the motd to the specified MOTD
     *
     * @param townMOTD - the new MOTD
     */
    public void setTownMOTD(String townMOTD) {
        this.townMOTD = townMOTD;
    }

    /**
     * Gets the name of the mayor of the town.
     *
     * @return the name of the town's mayor
     */
    @Override
    public String getMayor() {
        return mayor;
    }

    /**
     * Sets the town's mayor to the given name
     *
     * @param mayor the new mayor's name
     */
    @Override
    public void setMayor(String mayor) {
        this.mayor = mayor;
    }

    /**
     * Returns the town MOTD, with color formatting
     *
     * @return the town MOTD
     */
    @Override
    public String getTownMOTD() {
        return motdColor + townMOTD;
    }

    /**
     * Returns the town's name
     *
     * @return the town's name
     */
    @Override
    public String getTownName() {
        return townName;
    }

    /**
     * Returns the name of the world in which this town resides
     *
     * @return the name of the world
     */
    @Override
    public String getWorldName() {
        return worldName;
    }

    /**
     * Adds a player as a resident of the town
     *
     * @param p the player to be added
     * @return false if player was not added because player is already added,
     * true otherwise
     */
    @Override
    public boolean addPlayer(Player p) {
        return addPlayer(p.getName());
    }

    @Override
    public boolean addPlayer(String playerName) {
        if (residents.contains(playerName)) {
            return false;
        }

        residents.add(playerName);
        return true;
    }

    /**
     * Removes a player from the town. Postcondition: Player is not a resident
     * of the town, regardless of whether or not they were before. Note: Player
     * must still be removed from the WG regions associated with the town
     *
     * @param p - the player to be removed
     */
    @Override
    public void removePlayer(Player p) {
        removePlayer(p.getName());
    }

    /**
     * Removes the player from the town's list of residents and assistants. Does
     * not remove them from regions.
     *
     * @param playerName
     */
    @Override
    public void removePlayer(String playerName) {
        residents.remove(playerName);
        assistants.remove(playerName);
    }

    /**
     * Adds the territory to the town. The region of the territory will need to
     * be handled separately.
     *
     * @param territ the territory to be added
     * @return false if territ was not added because it is already added, true
     * otherwise
     */
    @Override
    public boolean addTerritory(Territory territ) {
        if (territories.contains(territ.getName())) {
            return false;
        }

        territories.add(territ.getName());
        return true;
    }

    /**
     * Removes the territory from the town.
     *
     * @param territName the name of the territory to remove
     * @return the removed territory
     */
    @Override
    public boolean removeTerritory(String territName) {
        return territories.remove(territName);
    }

    /**
     * Adds a player as an assistant to the town
     *
     * @param player the player to be added
     * @return false if player was not added because player was already added,
     * true otherwise
     */
    @Override
    public boolean addAssistant(Player player) {
        return addAssistant(player.getName());
    }

    /**
     * Promotes the resident to an assistant.
     *
     * @param playerName
     * @return true if player was added as assistant, false if they're already
     * an assistant or they're not a resident of the town.
     */
    @Override
    public boolean addAssistant(String playerName) {
        if (assistants.contains(playerName) || !residents.contains(playerName)) {
            return false;
        }

        assistants.add(playerName);
        return true;
    }

    /**
     * Removes the assistant from his position as an assistant
     *
     * @param player the player to be demoted
     * @return false if the player was not removed because the player is not an
     * assistant, true otherwise
     */
    @Override
    public boolean removeAssistant(Player player) {
        if (!assistants.contains(player.getName())) {
            return false;
        }

        assistants.remove(player.getName());
        return true;

    }

    /**
     * Returns the territories this town has.
     *
     * @return the town's territories
     */
    @Override
    public Collection<String> getTerritoriesCollection() {
        return (Collection<String>) territories.clone();
    }

    /**
     * Returns whether the player is the mayor or not
     *
     * @param p the player to be checked
     * @return whether the player is mayor or not
     */
    @Override
    public boolean playerIsMayor(Player p) {
        return p.getName().equals(mayor);
    }

    /**
     * Returns whether the player is the mayor of the town.
     *
     * @param playerName
     * @return
     */
    @Override
    public boolean playerIsMayor(String playerName) {
        return playerName.equals(mayor);
    }

    /**
     * Returns the list of all
     *
     * @return
     */
    @Override
    public String[] getResidentNames() {
        return residents.toArray(new String[residents.size()]);
    }

    /**
     *
     * @return
     */
    @Override
    public boolean allowsFriendlyFire() {
        return friendlyFire;
    }

    /**
     *
     * @param friendlyFire
     */
    @Override
    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    /**
     * Returns whether or not the player is an assistant in the town
     *
     * @param p the player to be checked
     * @return if the player is an assistant or not
     */
    @Override
    public boolean playerIsAssistant(Player p) {
        return assistants.contains(p.getName());
    }

    /**
     * Returns whether or not the player is a resident of the town
     *
     * @param p the player to be checked
     * @return if the player is a resident or not
     */
    @Override
    public boolean playerIsResident(Player p) {
        return residents.contains(p.getName());
    }
    
     /**
     * Returns whether or not the player is a resident of the town
     *
     * @param p the name of the player to be checked
     * @return if the player is a resident or not
     */
    @Override
    public boolean playerIsResident(String pName) {
        return residents.contains(pName);
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final YamlTown other = (YamlTown) obj;
        if ((this.townName == null) ? (other.townName != null) : !this.townName.equals(other.townName)) {
            return false;
        }
        if ((this.worldName == null) ? (other.worldName != null) : !this.worldName.equals(other.worldName)) {
            return false;
        }
        return true;
    }

    /**
     *
     * @param s
     * @return
     */
    @Override
    public org.bukkit.Location getTownSpawn(Server s) {
        return Location.convertToBukkitLocation(s, townSpawn);
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.townName != null ? this.townName.hashCode() : 0);
        hash = 67 * hash + (this.worldName != null ? this.worldName.hashCode() : 0);
        return hash;
    }

    /**
     *
     * @param playerExactName
     * @return
     */
    @Override
    public boolean playerIsAssistant(String playerExactName) {
        return assistants.contains(playerExactName);
    }

    /**
     * Returns the current number of residents in the town.
     *
     * @return the number of residents in the town
     */
    @Override
    public int getSize() {
        return residents.size();
    }

    /**
     *
     * @param server
     * @param message
     */
    @Override
    public void broadcastMessageToTown(Server server, String message) {
        Player temp;
        message = ChatColor.GOLD + message;

        for (String playerName : residents) {
            temp = server.getPlayerExact(playerName);
            if (temp != null) {
                temp.sendMessage(message);
            }
        }
    }

    /**
     *
     * @param wgp
     * @param p
     * @return
     */
    @Override
    public boolean playerIsInsideTownBorders(Player p) {
        org.bukkit.Location playerLoc = p.getLocation();
        RegionManager regMan = MCTowns.getWgp().getRegionManager(p.getWorld());

        ProtectedRegion tempReg;
        for (MCTRegion mctReg : MCTowns.getTownManager().getRegionsCollection()) {
            if(mctReg instanceof YamlTerritory) {
                tempReg = regMan.getRegion( ((YamlTerritory)mctReg).getName());
                if (tempReg != null) {
                    if (tempReg.contains(new Vector(playerLoc.getBlockX(), playerLoc.getBlockY(), playerLoc.getBlockZ()))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     *
     * @param loc
     */
    @Override
    public void setSpawn(org.bukkit.Location loc) {
        townSpawn = Location.convertFromBukkitLocation(loc);
    }

    /**
     *
     * @param s
     * @return
     */
    @Override
    public org.bukkit.Location getSpawn(Server s) {
        return Location.convertToBukkitLocation(s, townSpawn);
    }

    /**
     *
     * @return
     */
    @Override
    public boolean usesBuyablePlots() {
        return buyablePlots;
    }

    /**
     *
     * @param buyablePlots
     */
    @Override
    public void setBuyablePlots(boolean buyablePlots) {
        this.buyablePlots = buyablePlots;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean usesEconomyJoins() {
        return economyJoins;
    }

    /**
     *
     * @param economyJoins
     */
    @Override
    public void setEconomyJoins(boolean economyJoins) {
        this.economyJoins = economyJoins;
    }

    /**
     *
     * @return
     */
    @Override
    public BigDecimal getDefaultPlotPrice() {
        return defaultPlotPrice;
    }

    /**
     *
     * @param defaultPlotPrice
     */
    @Override
    public void setDefaultPlotPrice(BigDecimal defaultPlotPrice) {
        this.defaultPlotPrice = defaultPlotPrice;
    }

    @Override
    public String toString() {
        return this.townName;
    }

    public void writeYAML(FileConfiguration f) {
        f.set("townName", townName);
        f.set("worldName", worldName);
        f.set("motd", townMOTD);
        f.set("motdColor", motdColor.name());
        f.set("spawnLocation", townSpawn.toList());
        f.set("mayor", mayor);
        f.set("territs", getTerritoryNames());

        List<String> list = new LinkedList<>();
        list.addAll(assistants);
        f.set("assistants", list);
        
        List<String> resList = new LinkedList<>();
        resList.addAll(residents);
        f.set("residents", resList);

        f.set("friendlyFire", friendlyFire);
        f.set("defaultPlotPrice", defaultPlotPrice.toString());
        f.set("economyJoins", economyJoins);
        f.set("buyablePlots", buyablePlots);
        
        bank.writeYAML(f);
    }

    public static YamlTown readYAML(FileConfiguration f, YamlMCTFactory factory) {
        YamlTown t = new YamlTown();

        t.townName = f.getString("townName");
        t.worldName = f.getString("worldName");
        t.townMOTD = f.getString("motd");
        t.motdColor = ChatColor.valueOf(f.getString("motdColor"));
        t.townSpawn = Location.fromList(f.getStringList("spawnLocation"));
        t.mayor = f.getString("mayor");
        t.territories = parseListToHashSet(f.getStringList("territs"));

        t.assistants = new HashSet<>();
        t.assistants.addAll(f.getStringList("assistants"));
        
        t.residents = new HashSet<>();
        t.residents.addAll(f.getStringList("residents"));

        t.friendlyFire = f.getBoolean("friendlyFire");
        t.defaultPlotPrice = new BigDecimal(f.getString("defaultPlotPrice"));
        t.economyJoins = f.getBoolean("economyJoins");
        t.buyablePlots = f.getBoolean("buyablePlots");

        t.bank = BlockBank.readYAML(f);
        
        t.saveLocation = factory.getTownSavePath(t.townName);
        return t;
    }
    
    public static void recursivelyRemovePlayerFromTown(Player p, Town t) {
        YamlTownManager tMan = MCTowns.getTownManager();
        
        for(String teName : t.getTerritoriesCollection()) {
            Territory te = tMan.getTerritory(teName);
            for(String plName : te.getPlotsCollection()) {
                YamlPlot pl = tMan.getPlot(plName);
                pl.removePlayer(p);
            }
            te.removePlayer(p);
        }
        
        t.removePlayer(p);
    }

    private List<String> getTerritoryNames() {
        LinkedList<String> ret = new LinkedList<>();

        ret.addAll(this.territories);

        return ret;

    }

    private static HashSet<String> parseListToHashSet(List<String> s) {
        HashSet<String> ret = new HashSet<>();

        ret.addAll(s);

        return ret;
    }

    @Override
    public void save() throws IOException {
        FileConfiguration f = new YamlConfiguration();
        writeYAML(f);
        f.save(saveLocation);
    }
}