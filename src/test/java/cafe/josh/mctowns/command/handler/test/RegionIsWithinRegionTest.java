/*
 * Copyright (C) 2014 Joshua M Hertlein
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cafe.josh.mctowns.command.handler.test;

import cafe.josh.mctowns.command.CommandHandler;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author joshua
 */
public class RegionIsWithinRegionTest {
    @Test
    public void testPentagonIsWithinPentagon() {
        List<BlockVector2> interiorRegPoints, exteriorRegPoints;
        interiorRegPoints = new LinkedList<>();
        exteriorRegPoints = new LinkedList<>();
        
        BlockVector2 block = BlockVector2.at(0, 0);
        interiorRegPoints.add(BlockVector2.at(-1, 1));
        interiorRegPoints.add(BlockVector2.at(1, 1));
        interiorRegPoints.add(BlockVector2.at(2, 0));
        interiorRegPoints.add(BlockVector2.at(1, -1));
        interiorRegPoints.add(BlockVector2.at(-1, -1));

        exteriorRegPoints.add(BlockVector2.at(-4, 2));
        exteriorRegPoints.add(BlockVector2.at(0, 4));
        exteriorRegPoints.add(BlockVector2.at(4, 2));
        exteriorRegPoints.add(BlockVector2.at(4, -4));
        exteriorRegPoints.add(BlockVector2.at(-5, -3));

        ProtectedPolygonalRegion interiorReg, exteriorReg;

        interiorReg = new ProtectedPolygonalRegion("interiorReg", interiorRegPoints, 0, 256);
        exteriorReg = new ProtectedPolygonalRegion("exterior", exteriorRegPoints, 0, 256);

        assertTrue(CommandHandler.regionIsWithinRegion(interiorReg, exteriorReg));
        assertFalse(CommandHandler.regionIsWithinRegion(exteriorReg, interiorReg));
    }

    @Test
    public void testPolygonIsNotWithinConcavePolygon() {
        List<BlockVector2> interiorRegPoints, exteriorRegPoints;
        interiorRegPoints = new LinkedList<>();
        exteriorRegPoints = new LinkedList<>();

        exteriorRegPoints.add(BlockVector2.at(-5, 5));
        exteriorRegPoints.add(BlockVector2.at(0, 0));
        exteriorRegPoints.add(BlockVector2.at(5, 5));
        exteriorRegPoints.add(BlockVector2.at(5, -5));
        exteriorRegPoints.add(BlockVector2.at(-5, -5));

        interiorRegPoints.add(BlockVector2.at(-3, 1));
        interiorRegPoints.add(BlockVector2.at(4, 1));
        interiorRegPoints.add(BlockVector2.at(4, -3));
        interiorRegPoints.add(BlockVector2.at(-3, -3));

        ProtectedPolygonalRegion interiorReg, exteriorReg;

        interiorReg = new ProtectedPolygonalRegion("interiorReg", interiorRegPoints, 0, 256);
        exteriorReg = new ProtectedPolygonalRegion("exterior", exteriorRegPoints, 0, 256);

        assertFalse(CommandHandler.regionIsWithinRegion(interiorReg, exteriorReg));
        assertFalse(CommandHandler.regionIsWithinRegion(exteriorReg, interiorReg));
    }

    @Test
    public void testPolygonWithPointOutsideIsNotWithinPolygon() {
        List<BlockVector2> interiorRegPoints, exteriorRegPoints;
        interiorRegPoints = new LinkedList<>();
        exteriorRegPoints = new LinkedList<>();

        exteriorRegPoints.add(BlockVector2.at(-5, 5));
        exteriorRegPoints.add(BlockVector2.at(5, 5));
        exteriorRegPoints.add(BlockVector2.at(5, -5));
        exteriorRegPoints.add(BlockVector2.at(-5, -5));

        interiorRegPoints.add(BlockVector2.at(-3, 1));
        interiorRegPoints.add(BlockVector2.at(0, 500));
        interiorRegPoints.add(BlockVector2.at(4, 1));
        interiorRegPoints.add(BlockVector2.at(4, -3));
        interiorRegPoints.add(BlockVector2.at(-3, -3));

        ProtectedPolygonalRegion interiorReg, exteriorReg;

        interiorReg = new ProtectedPolygonalRegion("interiorReg", interiorRegPoints, 0, 256);
        exteriorReg = new ProtectedPolygonalRegion("exterior", exteriorRegPoints, 0, 256);

        assertFalse(CommandHandler.regionIsWithinRegion(interiorReg, exteriorReg));
        assertFalse(CommandHandler.regionIsWithinRegion(exteriorReg, interiorReg));
    }

    @Test
    public void testCuboidIsWithinPolygon() {
        ProtectedRegion interior, exterior;

        interior = new ProtectedCuboidRegion("interior", BlockVector3.at(-1, 0, -1), BlockVector3.at(1, 256, 1));

        List<BlockVector2> exteriorRegPoints = new LinkedList<>();
        exteriorRegPoints.add(BlockVector2.at(-5, 5));
        exteriorRegPoints.add(BlockVector2.at(5, 5));
        exteriorRegPoints.add(BlockVector2.at(5, -5));
        exteriorRegPoints.add(BlockVector2.at(-5, -5));
        exterior = new ProtectedPolygonalRegion("exterior", exteriorRegPoints, 0, 256);

        assertTrue(CommandHandler.regionIsWithinRegion(interior, exterior));
        assertFalse(CommandHandler.regionIsWithinRegion(exterior, interior));
    }

    @Test
    public void testCuboidIsWithinCuboid() {
        ProtectedRegion interior, exterior;

        interior = new ProtectedCuboidRegion("interior", BlockVector3.at(-1, 0, -1), BlockVector3.at(1, 256, 1));
        exterior = new ProtectedCuboidRegion("exterior", BlockVector3.at(-3, 0, -3), BlockVector3.at(3, 256, 3));

        assertTrue(CommandHandler.regionIsWithinRegion(interior, exterior));
        assertFalse(CommandHandler.regionIsWithinRegion(exterior, interior));
    }
}
