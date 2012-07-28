/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.everdras.mctowns.util;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import java.util.LinkedList;
import java.util.List;
import me.everdras.mctowns.MCTowns;
import org.bukkit.Location;
import org.bukkit.Material;

/**
 *
 * @author joshua
 */
public class ProtectedFenceRegion extends ProtectedPolygonalRegion {

    private static final int NORTH = 0, SOUTH = 1, EAST = 2, WEST = 3, NONE = -1;

    public ProtectedFenceRegion(String id, List<BlockVector2D> points, int minY, int maxY) {
        super(id, points, minY, maxY);
    }

    public static final ProtectedFenceRegion assembleSelectionFromFenceOrigin(String id, Location l) throws IncompleteFenceException {
        LinkedList<BlockVector2D> points = new LinkedList<>();

        Location cur;
        int dirToNext, cameFrom;

        cur = l.clone();
        cameFrom = NONE;
        do {
            dirToNext = getDirToNextFence(cameFrom, cur);
            System.out.println("Dir to next:" + dirToNext);

            //if there was a corner in the fence...
            if(getOppositeDir(cameFrom) != dirToNext) {
                //add it to the polygon
                points.add(new BlockVector2D(cur.getBlockX(), cur.getBlockZ()));
                MCTowns.logDebug("Added a new point: " + "(" + cur.getBlockX() + "," + cur.getBlockZ() + ")");
            }


            switch (dirToNext) {
                case NORTH:
                    cur.add(0, 0, 1);
                    cameFrom = SOUTH;
                    break;
                case SOUTH:
                    cur.add(0, 0, -1);
                    cameFrom = NORTH;
                    break;
                case EAST:
                    cur.add(1, 0, 0);
                    cameFrom = WEST;
                    break;
                case WEST:
                    cur.add(-1, 0, 0);
                    cameFrom = EAST;
                    break;
                case NONE:
                    throw new IncompleteFenceException();
            }
        } while (!cur.equals(l));


        return new ProtectedFenceRegion(id, points, 0, l.getWorld().getMaxHeight()-1);
    }

    private static final int getDirToNextFence(int cameFrom, Location l) {
        System.out.println("Came from: " + cameFrom);

        if (cameFrom != SOUTH) {
            if (l.clone().add(0, 0, -1).getBlock().getType() == Material.FENCE) {
                return SOUTH;
            }
        }

        if (cameFrom != NORTH) {
            if (l.clone().add(0, 0, 1).getBlock().getType() == Material.FENCE) {
                return NORTH;
            }
        }

        if (cameFrom != EAST) {
            if (l.clone().add(1, 0, 0).getBlock().getType() == Material.FENCE) {
                return EAST;
            }
        }

        if (cameFrom != WEST) {
            if (l.clone().add(-1, 0, 0).getBlock().getType() == Material.FENCE) {
                return WEST;
            }
        }

        return NONE;
    }

    public static class IncompleteFenceException extends Exception {

        public IncompleteFenceException() {
            super("The fence was not a complete loop.");
        }
    }

    private static int getOppositeDir(int dir) {
        switch(dir) {
            case NORTH:
                return SOUTH;
            case SOUTH:
                return NORTH;
            case EAST:
                return WEST;
            case WEST:
                return EAST;
            default:
                return -1;
        }
    }
}
