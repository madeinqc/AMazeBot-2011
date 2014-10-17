/**
 * This bot will be a basic bot just basic enought to have good logic to solve,
 * but not the best. There is the strategy he will follow:
 * - keep track of were it goes in a stack
 * - keep a map of the maze (next, left and right at each tile) and the number
 *      of time we walked on a tile
 *
 * Here's the pseudocode the bot will face when ready for an other iteration:
 * - Am I looking in a direction that I can walk, that I never walked and that bring me closer to the goal?
 * [YES]
 *      - Go there and add the Point to the trajet
 *      - Scan adjacent tiles to complete map
 * [NO]
 *      - Can I go to the other direction that bring me closer to the goal and that I never been?
 *          [YES]
 *              - Go there and add the Point to the trajet
 *              - Scan adjacent tiles to complete map
 *          [NO]
 *              - Check the possibility that is the closest to the goal
 *              - Do from the begin with the new goal and when reached, continue with the final goal
 *
 * Here's the planned strategies for next evolution:
 * // For a more advanced bot:
 * - If we cross a way that we already visited in a way that
 *      it create a zone in the middle, and that this closed zone does
 *      not contain the goal, then we mark this zone as useless.
 *
 * @author Marc-Antoine Sauvé
 */
package bots.BasicBot;


// The following imports are required.
import VisualMap.VisualMap;
import amazebot2011.*;
import amazebot2011.BotInterface.*;

// These are optional, depending on your needs.
import amazebot2011.developer.DeveloperFrame;
import static amazebot2011.utils.PointHelper.*;
import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JTable;



// Your primary class must be called Brain and must implement BrainInterface.
// You are free to create other classes in other source files if you need.
// Since each Brain exists in its own package, there will never be conflicts.
public class Brain implements BrainInterface
{
    BotInterface	bot;		// a reference to a bot that your brain will control
    Point			finalGoal;      // coordinates of the top-left cell of the 4x4 goal room
    Point			goal;		// coordinates of the temp goal
    Point			mazesize;	// dimensions of the maze
    Point			startpos;	// coordinates of the bot's starting position
    Compass			startdir;	// direction in which the bot faces at start
    Point                   pos;            // current bot's coordinates
    Compass                 dir;            // current bot's direction

    int[][]                 map;            // map representation of the maze
    Stack<Point> trajet = new Stack<Point>(); //Keep traject done strait line IE when going back on traject, pop them out
    JFrame visualMapFrame;
    JTable visualMap = new VisualMap().jTable1;
    List<Point> possibilities = new ArrayList<Point>();
    List<Point> goalPoints = new ArrayList<Point>();

    int SMAPKNOWN = -5;
    int OUTOFMAZE = -4;
    int GOAL = -3;
    int WALL = -2;
    int FREE = -1;
    int UNKNOWN = 0;
    //1 and more is the number of time passed on the case

    //---------------------------------------------------------------------------------------------
    // REQUIRED METHOD: So the system can find out your preferred alias.
    // This name is used in the competition, so make it fun. Don't forget
    // to provide a nice icon too.
    @Override
    public String getName()
    {
            return "madeinqc";
    }

    //---------------------------------------------------------------------------------------------
    // REQUIRED METHOD: The system will hand you a bot, and your code will operate it.
    @Override
    public void runBot(BotInterface inbot)
    {
            bot = inbot;
            // Determine the dimensions of the maze. In this competition the size
            // is fixed at 44 by 44 cells, which is what this method will return.
            // So you could hardcode it, but it's generally better to programmatically
            // discover such things. Note that cell coordinates range from 0 to 43.
            mazesize = bot.getMazeSize();

            // Set the map size
            map = new int[mazesize.x][mazesize.y];

            // Determine the location of the goal room. The goal room is always
            // 4 by 4 cells. This method gives you the top-left cell of the goal room.
            finalGoal = bot.getGoalCorner();
            goal = finalGoal;

            // Set the goal on map
            for (int index = 0; index < 2; index++)
                for (int i = 0; i < 4; i++)
                {
                    setMap(new Point(finalGoal.x+i, finalGoal.y+3*index),GOAL);
                    goalPoints.add(new Point(finalGoal.x+i, finalGoal.y+3*index));
                }
            
            setMap(new Point(finalGoal.x, finalGoal.y+1),GOAL);
            goalPoints.add(new Point(finalGoal.x, finalGoal.y+1));
            setMap(new Point(finalGoal.x, finalGoal.y+2),GOAL);
            goalPoints.add(new Point(finalGoal.x, finalGoal.y+1));
            setMap(new Point(finalGoal.x+3, finalGoal.y+1),GOAL);
            goalPoints.add(new Point(finalGoal.x, finalGoal.y+1));
            setMap(new Point(finalGoal.x+3, finalGoal.y+2),GOAL);
            goalPoints.add(new Point(finalGoal.x, finalGoal.y+1));
            /*
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    setMap(new Point(finalGoal.x+i, finalGoal.y+j),GOAL);
                }
            }
            */

            // Get the starting position and direction.
            startpos = bot.getStartPosition();
            startdir = bot.getStartDirection();

            incMap(startpos);

            // Set the actual position and direction
            pos = startpos;
            dir = startdir;

            //put the 1st traject path
            trajet.push(pos);

            //Dump map and wait
            dumpMap();

            // Get the distance from the start position to the goal
            double distStartToGoal = finalGoal.distance(pos);

            // You can log diagnostic information to your bot's log file (contained in the
            // "logs/" folder; in this case the file is "SampleBot_errors.log". As the filename
            // indicates, this is primarily for tracking errors--any unhandled exceptions
            // in your code will be logged to this file. But you can output any diagnostic
            // information to assist in debugging.
            //bot.println("SampleBot about to start running...");

            // Every bot program has this main loop. Note that this is an infinite loop,
            // but that's ok because once your bot reaches the goal, runs out of energy,
            // or crashes the system will automatically exit your code.
            while (true)
            {

                //update the map for adjacent coord
                updateMapAdjacentCases();

                //if goal == last possibility, then set it to the final goal
                if (pos == goal)
                {
                    possibilities.remove(goal);
                    goal = finalGoal;
                }

                //dumpMap();

                int north = canGo(Compass.NORTH);
                int south = canGo(Compass.SOUTH);
                int east = canGo(Compass.EAST);
                int west = canGo(Compass.WEST);

                //Can I go north?
                if (north == FREE || north == GOAL)
                {
                    //Does it bring me closer?
                    if (closerFromGoal(Compass.NORTH))
                    //if (pos.distance(goal) > addPoints(pos, Compass.NORTH.getVector()).distance(goal))
                    {
                        //then go!
                        turn(Compass.NORTH);
                        move(MOVE.FORWARD);
                        continue;
                    }
                    else
                    {
                        //can I go on 1st side?
                        if (east == FREE || east == GOAL)
                        {
                            //does it bring me closer?
                            if (closerFromGoal(Compass.EAST))
                            //if (pos.distance(goal) > addPoints(pos, Compass.EAST.getVector()).distance(goal))
                            {
                                //then go!
                                turn(Compass.EAST);
                                move(MOVE.FORWARD);
                                continue;
                            }
                        }

                        //can I go on 2nd side?
                        if (west == FREE || west == GOAL)
                        {
                            //does it bring me closer?
                            if (closerFromGoal(Compass.WEST))
                            //if (pos.distance(goal) > addPoints(pos, Compass.WEST.getVector()).distance(goal))
                            {
                                //then go!
                                turn(Compass.WEST);
                                move(MOVE.FORWARD);
                                continue;
                            }
                        }

                        //can I go opposite?
                        if (south == FREE || south == GOAL)
                        {
                            //does it bring me closer?
                            if (closerFromGoal(Compass.SOUTH))
                            //if (pos.distance(goal) > addPoints(pos, Compass.SOUTH.getVector()).distance(goal))
                            {
                                //then go!
                                turn(Compass.SOUTH);
                                move(MOVE.FORWARD);
                                continue;
                            }
                        }

                        //if none of the above bring me closer then go!
                        turn(Compass.NORTH);
                        move(MOVE.FORWARD);
                        continue;
                    }
                }

                if (south == FREE || south == GOAL)
                {
                    //Does it bring me closer?
                    if (closerFromGoal(Compass.SOUTH))
                    //if (pos.distance(goal) > addPoints(pos, Compass.SOUTH.getVector()).distance(goal))
                    {
                        //then go!
                        turn(Compass.SOUTH);
                        move(MOVE.FORWARD);
                        continue;
                    }
                    else
                    {
                        //can I go on 1st side?
                        if (east == FREE || east == GOAL)
                        {
                            //does it bring me closer?
                            if (closerFromGoal(Compass.EAST))
                            //if (pos.distance(goal) > addPoints(pos, Compass.EAST.getVector()).distance(goal))
                            {
                                //then go!
                                turn(Compass.EAST);
                                move(MOVE.FORWARD);
                                continue;
                            }
                        }

                        //can I go on 2nd side?
                        if (west == FREE || west == GOAL)
                        {
                            //does it bring me closer?
                            if (closerFromGoal(Compass.WEST))
                            //if (pos.distance(goal) > addPoints(pos, Compass.WEST.getVector()).distance(goal))
                            {
                                //then go!
                                turn(Compass.WEST);
                                move(MOVE.FORWARD);
                                continue;
                            }
                        }

                        //if none of the above bring me closer then go!
                        turn(Compass.SOUTH);
                        move(MOVE.FORWARD);
                        continue;
                    }
                }

                if (west == FREE || west == GOAL)
                {
                    //Does it bring me closer?
                    if (closerFromGoal(Compass.WEST))
                    //if (pos.distance(goal) > addPoints(pos, Compass.WEST.getVector()).distance(goal))
                    {
                        //then go!
                        turn(Compass.WEST);
                        move(MOVE.FORWARD);
                        continue;
                    }
                    else
                    {

                        //can I go opposite?
                        if (east == FREE || east == GOAL)
                        {
                            //does it bring me closer?
                            if (closerFromGoal(Compass.EAST))
                            //if (pos.distance(goal) > addPoints(pos, Compass.EAST.getVector()).distance(goal))
                            {
                                //then go!
                                turn(Compass.EAST);
                                move(MOVE.FORWARD);
                                continue;
                            }
                        }

                        //if none of the above bring me closer then go!
                        turn(Compass.WEST);
                        move(MOVE.FORWARD);
                        continue;
                    }
                }

                if (east == FREE || east == GOAL)
                {
                    //Does it bring me closer?
                    if (closerFromGoal(Compass.EAST))
                    //if (pos.distance(goal) > addPoints(pos, Compass.EAST.getVector()).distance(goal))
                    {
                        //then go!
                        turn(Compass.EAST);
                        move(MOVE.FORWARD);
                        continue;
                    }
                    else
                    {
                        //if none of the above bring me closer then go!
                        turn(Compass.EAST);
                        move(MOVE.FORWARD);
                        continue;
                    }
                }

                // THERE'S NO NEW PATH POSSIBLE, THEN TRY OLD PATH
                // FIND THE LESS USED PATH

                // set the new goal at the last possibility
                // set the goal at the closest possibility from final goal
                // It might be because there's no check to see if the closest selected is indeed the final goal
                Point closest = possibilities.get(possibilities.size()-1);
                for (int index = possibilities.size()-2; index >= 0; index--) {
                    if (possibilities.get(index).distance(finalGoal) < closest.distance(finalGoal))
                        closest = possibilities.get(index);
                }

                println("Try on already used path, new goal at " + goal);

                //Make the smallest path to the goal
                //List<Point> smallestPath = getSmallestPath();

                // find the smallest of the fourth
                int smallest = 99;
                if (south > 0)
                    smallest = south;
                if (north > 0 && north < smallest)
                    smallest = north;
                if (east > 0 && east < smallest)
                    smallest = east;
                if (west > 0 && west < smallest)
                    smallest = west;

                //and test it
                //Can I go north?
                if (north > 0 && north == smallest)
                {
                    //Does it bring me closer?
                    if (closerFromGoal(Compass.NORTH))
                    //if (pos.distance(goal) > addPoints(pos, Compass.NORTH.getVector()).distance(goal))
                    {
                        //then go!
                        turn(Compass.NORTH);
                        move(MOVE.FORWARD);
                        continue;
                    }
                    else
                    {
                        //can I go on 1st side?
                        if (east > 0 && east == smallest)
                        {
                            //does it bring me closer?
                            if (closerFromGoal(Compass.EAST))
                            //if (pos.distance(goal) > addPoints(pos, Compass.EAST.getVector()).distance(goal))
                            {
                                //then go!
                                turn(Compass.EAST);
                                move(MOVE.FORWARD);
                                continue;
                            }
                        }

                        //can I go on 2nd side?
                        if (west > 0 && west == smallest)
                        {
                            //does it bring me closer?
                            if (closerFromGoal(Compass.WEST))
                            //if (pos.distance(goal) > addPoints(pos, Compass.WEST.getVector()).distance(goal))
                            {
                                //then go!
                                turn(Compass.WEST);
                                move(MOVE.FORWARD);
                                continue;
                            }
                        }

                        //can I go opposite?
                        if (south > 0 && south == smallest)
                        {
                            //does it bring me closer?
                            if (closerFromGoal(Compass.SOUTH))
                            //if (pos.distance(goal) > addPoints(pos, Compass.SOUTH.getVector()).distance(goal))
                            {
                                //then go!
                                turn(Compass.SOUTH);
                                move(MOVE.FORWARD);
                                continue;
                            }
                        }

                        //if none of the above bring me closer then go!
                        turn(Compass.NORTH);
                        move(MOVE.FORWARD);
                        continue;
                    }
                }

                if (south > 0 && south == smallest)
                {
                    //Does it bring me closer?
                    if (closerFromGoal(Compass.SOUTH))
                    //if (pos.distance(goal) > addPoints(pos, Compass.SOUTH.getVector()).distance(goal))
                    {
                        //then go!
                        turn(Compass.SOUTH);
                        move(MOVE.FORWARD);
                        continue;
                    }
                    else
                    {
                        //can I go on 1st side?
                        if (east > 0 && east == smallest)
                        {
                            //does it bring me closer?
                            if (closerFromGoal(Compass.EAST))
                            //if (pos.distance(goal) > addPoints(pos, Compass.EAST.getVector()).distance(goal))
                            {
                                //then go!
                                turn(Compass.EAST);
                                move(MOVE.FORWARD);
                                continue;
                            }
                        }

                        //can I go on 2nd side?
                        if (west > 0 && west == smallest)
                        {
                            //does it bring me closer?
                            if (closerFromGoal(Compass.WEST))
                            //if (pos.distance(goal) > addPoints(pos, Compass.WEST.getVector()).distance(goal))
                            {
                                //then go!
                                turn(Compass.WEST);
                                move(MOVE.FORWARD);
                                continue;
                            }
                        }

                        //if none of the above bring me closer then go!
                        turn(Compass.SOUTH);
                        move(MOVE.FORWARD);
                        continue;
                    }
                }

                if (west > 0 && west == smallest)
                {
                    //Does it bring me closer?
                    if (closerFromGoal(Compass.WEST))
                    //if (pos.distance(goal) > addPoints(pos, Compass.WEST.getVector()).distance(goal))
                    {
                        //then go!
                        turn(Compass.WEST);
                        move(MOVE.FORWARD);
                        continue;
                    }
                    else
                    {   
                        //can I go opposite?
                        if (east > 0 && east == smallest)
                        {
                            //does it bring me closer?
                            if (closerFromGoal(Compass.EAST))
                            //if (pos.distance(goal) > addPoints(pos, Compass.EAST.getVector()).distance(goal))
                            {
                                //then go!
                                turn(Compass.EAST);
                                move(MOVE.FORWARD);
                                continue;
                            }
                        }

                        //if none of the above bring me closer then go!
                        turn(Compass.WEST);
                        move(MOVE.FORWARD);
                        continue;
                    }
                }

                if (east > 0 && east == smallest)
                {
                    //Does it bring me closer?
                    if (closerFromGoal(Compass.EAST))
                    //if (pos.distance(goal) > addPoints(pos, Compass.EAST.getVector()).distance(goal))
                    {
                        //then go!
                        turn(Compass.EAST);
                        move(MOVE.FORWARD);
                        continue;
                    }
                    else
                    {   
                        //if none of the above bring me closer then go!
                        turn(Compass.EAST);
                        move(MOVE.FORWARD);
                        continue;
                    }
                }

                //THERE'S NO OLD PATH THAT BRING ME CLOSER... DESESPER
                println("what the hell??");
                //to use up energy
                Random ran = new Random();
                Compass newDir = Compass.EAST;
                switch(ran.nextInt(4))
                {
                    case 0:
                        newDir = newDir.EAST;
                        break;
                    case 1:
                        newDir = newDir.NORTH;
                        break;
                    case 2:
                        newDir = newDir.SOUTH;
                        break;
                    case 3:
                        newDir = newDir.WEST;
                        break;
                }
                turn(newDir);
                move(MOVE.FORWARD);
            }
    }
    
    private boolean closerFromGoal(Compass pdir)
    {
        boolean closer = false;
        Point nextPoint = addPoints(pos, pdir.getVector());
        for (Point point : goalPoints) {
            double distPos = pos.distance(point);
            double distNext = nextPoint.distance(point);
            if (distPos > distNext)
                return true;
        }
        return false;
    }

    private void turn(TURN turn)
    {
        bot.turn(turn);
        dir = dir.getTurn(turn);
    }

    private void turn(Compass pDir)
    {
        bot.turn(getTurnDirection(pDir));
        dir = pDir;
    }

    private void move(MOVE move)
    {
        bot.move(move);
        Compass newDir;
        if (move == MOVE.FORWARD)
            newDir = dir;
        else
            newDir = dir.getOpposite();
        pos = addPoints(pos, newDir.getVector());
        incMap(pos);
        possibilities.remove(pos);
    }

    private void move(MOVE move, int count)
    {
        for (int i = 0; i < count; i++)
            move(move);
    }

    private int canGo(Compass direction)
    {
        //Can we go? Check on map, if unknown then check with bot
        Point next = addPoints(pos, direction.getVector());
        if (next.x < 0 || next.y < 0 || next.x >= mazesize.x || next.y >= mazesize.y)
            return OUTOFMAZE;
        if (getMap(next) != UNKNOWN)
            //can move
            return getMap(next);
        else 
            bot.println("ERROR: bot should know the following coord as he should have looked it: " + next);
        return UNKNOWN;
    }

    private int canGo(Compass direction,Point pPos)
    {
        //Can we go? Check on map, if unknown then check with bot
        Point next = addPoints(pPos, direction.getVector());
        if (next.x < 0 || next.y < 0 || next.x >= mazesize.x  || next.y >= mazesize.y)
            return OUTOFMAZE;
        if (getMap(next) != UNKNOWN)
            //can move
            return getMap(next);
        else 
            bot.println("ERROR: bot should know the following coord as he should have looked it: " + next);
        return UNKNOWN;
    }

    private TURN getTurnDirection(Compass direction)
    {
        int turn = dir.getTurnsTo(direction);
        switch (turn)
        {
            case -1:
                return TURN.LEFT;
            case 1:
                return TURN.RIGHT;
            case 2:
                return TURN.AROUND;
            default:
                return TURN.NONE;
        }
    }

    private LOOK getLookDirection(Compass direction)
    {
        int look = dir.getTurnsTo(direction);
        switch (look)
        {
            case -1:
                return LOOK.LEFT;
            case 1:
                return LOOK.RIGHT;
            case 0:
                return LOOK.AHEAD;
            default:
                return null;
        }
    }

    private void updateMapAdjacentCases()
    {
        //Look ahead
        Point point = getNextPoint(LOOK.AHEAD);
        if (getMap(point) == UNKNOWN)
        {
            if (bot.look(LOOK.AHEAD))
                setMap(point,FREE);
            else
                setMap(point,WALL);
        }

        //Look left
        point = getNextPoint(LOOK.LEFT);
        if (getMap(point) == UNKNOWN)
        {
            if (bot.look(LOOK.LEFT))
                setMap(point,FREE);
            else
                setMap(point,WALL);
        }

        //Look right
        point = getNextPoint(LOOK.RIGHT);
        if (getMap(point) == UNKNOWN)
        {
            if (bot.look(LOOK.RIGHT))
                setMap(point,FREE);
            else
                setMap(point,WALL);
        }
    }

    private void setMap(Point pPos, int value)
    {
        if (pPos.x < 0 || pPos.x >= mazesize.x ||
            pPos.y < 0 || pPos.y >= mazesize.y)
            return;
        map[pPos.x][pPos.y] = value;
        visualMap.setValueAt(value, pPos.y, pPos.x);
        if (value == FREE)
            possibilities.add(pPos);
    }

    private void incMap(Point pPos)
    {
        if (pPos.x < 0 || pPos.x >= mazesize.x ||
            pPos.y < 0 || pPos.y >= mazesize.y)
            return;
        if (map[pPos.x][pPos.y] == FREE)
            map[pPos.x][pPos.y] = 1;
        else
            map[pPos.x][pPos.y]++;
        visualMap.setValueAt(map[pPos.x][pPos.y], pPos.y, pPos.x);
    }

    private void decMap(Point pPos)
    {
        if (pPos.x < 0 || pPos.x >= mazesize.x ||
            pPos.y < 0 || pPos.y >= mazesize.y)
            return;
        map[pPos.x][pPos.y]--;
        visualMap.setValueAt(map[pPos.x][pPos.y], pPos.y, pPos.x);
    }

    private int getMap(Point pPos)
    {
        if (pPos.x < 0 || pPos.x >= mazesize.x ||
            pPos.y < 0 || pPos.y >= mazesize.y)
            return OUTOFMAZE;
        return map[pPos.x][pPos.y];
    }

    private void setMap(Point pPos, int value, int[][] pmap)
    {
        if (pPos.x < 0 || pPos.x >= mazesize.x ||
            pPos.y < 0 || pPos.y >= mazesize.y)
            return;
        pmap[pPos.x][pPos.y] = value;
    }

    private void incMap(Point pPos, int[][] pmap)
    {
        if (pPos.x < 0 || pPos.x >= mazesize.x ||
            pPos.y < 0 || pPos.y >= mazesize.y)
            return;
        if (pmap[pPos.x][pPos.y] == FREE)
            pmap[pPos.x][pPos.y] = 1;
        else
            pmap[pPos.x][pPos.y]++;
    }

    private void decMap(Point pPos, int[][] pmap)
    {
        if (pPos.x < 0 || pPos.x >= mazesize.x ||
            pPos.y < 0 || pPos.y >= mazesize.y)
            return;
        pmap[pPos.x][pPos.y]--;
    }

    private int getMap(Point pPos, int[][] pmap)
    {
        if (pPos.x < 0 || pPos.x >= mazesize.x ||
            pPos.y < 0 || pPos.y >= mazesize.y)
            return OUTOFMAZE;
        return pmap[pPos.x][pPos.y];
    }

    /**
     * Get the point where the bot would be if he goes the
     * direction indicated by the TURN.
     * @param pTurn The direction to get the point.
     * @return The Point where the bot would be.
     */
    private Point getNextPoint(TURN pTurn)
    {
        return getNextPoint(dir.getTurn(pTurn));
    }

    /**
     * Get the point where the bot would be if he goes the
     * direction indicated by the LOOK.
     * @param pLook The direction to get the point.
     * @return The Point where the bot would be.
     */
    private Point getNextPoint(LOOK pLook)
    {
        if (pLook == LOOK.LEFT)
            return getNextPoint(dir.getLeft());
        if (pLook == LOOK.RIGHT)
            return getNextPoint(dir.getRight());
        return getNextPoint(dir);
    }

    /**
     * Get the point where the bot would be if he goes the
     * direction indicated by the LOOK.
     * @param pMove The direction to get the point.
     * @return The Point where the bot would be.
     */
    private Point getNextPoint(MOVE pMove)
    {
        if (pMove == MOVE.FORWARD)
            return getNextPoint(dir);
        return getNextPoint(dir.getOpposite());
    }

    /**
     * Get the point where the bot would be if he goes the
     * direction indicated by the Compass.
     * @param pDir The direction to get the point.
     * @return The Point where the bot would be.
     */
    private Point getNextPoint(Compass pDir)
    {
        return addPoints(pos, pDir.getVector());
    }

    ////////////////////////////////////////////////////////////////////////
    private void dumpMap()
    {
        for (int i = 0; i < mazesize.y; i++) {
            for (int j = 0; j < mazesize.x; j++) {
                print(map[j][i] + ",");
            }
            println();
        }
        println();
        println();
    }
    
    private void drawMap(int[][] pmap)
    {
        for (int i = 0; i < mazesize.y; i++) {
            for (int j = 0; j < mazesize.x; j++) {
                visualMap.setValueAt(pmap[j][i], i, j);
            }
        }
    }

    private void println()
    {
        System.out.println();
    }

    private void println(String text)
    {
        System.out.println(text);
    }

    private void print(String text)
    {
        System.out.print(text);
    }

    private List<Point> getSmallestPath() {
        Compass sdir = dir;
        Point spos = pos;
        int[][] smap = map.clone();
        Stack<Point> spossibilities = new Stack<Point>();
        List<Point> path = new ArrayList<Point>();
        
        //Prepare smap
        for (int i = 0; i < mazesize.x; i++) {
            for (int j = 0; j < mazesize.y; j++) {
                if (smap[i][j] > 0)
                    smap[i][j] = SMAPKNOWN;
            }
        }
        
        while (spos != goal)
        {
            drawMap(smap);
            break;
        }
        return path;
    }

    //---------------------------------------------------------------------------------------------
    // EXTRA! Here is a useful convenience class. Please use it for all your directional needs!
    // We already used it to find out the bot's starting direction, and you can use it to keep track
    // of which way your bot is pointing. It has methods covering all the important operations.
    private void funWithCompasses()
    {
            Compass c = Compass.EAST;	// Let's create a new compass and make it point EAST.

            // To turn the compass you have to reassign (that's just the way Java's enums work):
            c = c.getRight();	// Result is Compass.SOUTH.

            // You can get the unit vector equivalent to this direction:
            Point vector = c.getVector();

            // What's the vector useful for? Well suppose you have the bot's current position, and
            // you want to know what the position would be if it were to move in a particular direction.

            Point pos = bot.getPosition();		// Note that this is not a free method!

            // You can use a method provided by the PointHelper class (which has been statically
            // imported so you don't have to use the class name here) to easily add the points:
            Point nextpos = addPoints(pos, vector);

            // Say wouldn't it be nice if Java had operator overloading? Yeah.

            // You can also perform a specified number of 90-degree turns:
            // Positive values are right turns, negative values are left turns.
            c = c.plus(2);		// Result is Compass.NORTH.

            // Often you want to find the opposite direction and that is done thusly:
            c = c.getOpposite();		// Result is Compass.SOUTH again.

            // Or when you need to compare 2 directions you can do it like this:

            // Find how many 90-degree turns are needed to change direction from WEST to whatever c is facing:
            int turns = c.getTurnsFrom(Compass.WEST);	// Result is -1.

            // Or you can go the other way, whatever is most intuitive for you:
            turns = c.getTurnsTo(Compass.EAST);		// Result is 2.
    }

    //---------------------------------------------------------------------------------------------
    // REQUIRED METHOD: Don't touch this! This is needed because there seems to be no way
    // to tell NetBeans to use a jar's main class to startup.
    public static void main(String[] args)
    {
        new amazebot2011.developer.DeveloperFrame();
    }
}

