/*	AMazeBot 2011 Sample Bot
 *
 *	This is a starting point for the Amazebot 2011 competition.
 *	The source file shows the basic functionality of the major methods
 *	in the Amazebot API.
 *
 *	Read through the documentation to get a better understanding of the program.
 *	See http://amazebot.mohawkcollege.ca/
 *
 *	Original developed by C. Mark Yendt (Mohawk College, Jan 2005)
 *	Updated by Aravin Duraikannan (Dec 2010)
 *
 */

// Your class must exist in a package that is the public name of
// your bot (for example in the competition it would be your
// assigned ID, like AMB9_042). Your package must in turn exist
// inside the "bots" package to be recognized by the system.
// In NetBeans you can use the Refactor command to easily change this.
package bots.MediumGoodBot;


// The following imports are required.
import VisualMap.VisualMap;
import amazebot2011.*;
import amazebot2011.BotInterface.*;

// These are optional, depending on your needs.
import static amazebot2011.utils.PointHelper.*;
import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JTable;

/**
 * This bot will be a basic bot just basic enought to have good logic to solve,
 * but not the best. There is the strategy he will follow:
 * - keep track of were he gone in order he gone in a stack
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
    VisualMap visualMapFrame = new VisualMap();
    JTable visualMap = visualMapFrame.jTable1;
    List<Point> possibilities = new ArrayList<Point>();
    List<Point> goalPoints = new ArrayList<Point>();
    boolean charged = false;

    int FREECHECKED = -8;
    int CHECKED = -7;
    int UNREVELENT = -6;
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
            return "scorpiobot";
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
        for (int j = 0; j < 4; j++)
            for (int i = 0; i < 4; i++)
            {
                setMap(new Point(finalGoal.x+i, finalGoal.y+j),GOAL);
                goalPoints.add(new Point(finalGoal.x+i, finalGoal.y+j));
            }

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

            // TODO run this only when usefull
            // find a way like run it when close to borders or when meeting an old path
            removeUnusefullMap();

            // TODO Implement the detection of the charger and pick it up
            checkForCharger();

            //if goal == last possibility, then set it to the final goal
            if (pos == goal)
            {
                while(possibilities.remove(goal))
                    possibilities.remove(goal);
                goal = finalGoal;
            }

            //Used for debug, will stop the bot at a particular point
            Point breakAt = new Point(-1,-1);
            if (pos.distance(breakAt) == 0)
                breakAt = pos;

            int canNorth = canGo(Compass.NORTH);
            int canSouth = canGo(Compass.SOUTH);
            int canEast = canGo(Compass.EAST);
            int canWest = canGo(Compass.WEST);

            boolean north = canNorth == FREE || canNorth == GOAL;
            boolean south = canSouth == FREE || canSouth == GOAL;
            boolean east = canEast == FREE || canEast == GOAL;
            boolean west = canWest == FREE || canWest == GOAL;

            List<Point> possDir = new ArrayList<Point>();
            if (north)
                possDir.add(addPoints(pos, Compass.NORTH.getVector()));
            if (south)
                possDir.add(addPoints(pos, Compass.SOUTH.getVector()));
            if (east)
                possDir.add(addPoints(pos, Compass.EAST.getVector()));
            if (west)
                possDir.add(addPoints(pos, Compass.WEST.getVector()));

            // TODO Enhence the algorythm to check if we are close of a wall or another visited way
            //      that would allow us to remove a part of the maze

            Point closest = null;

            if (possDir.size() == 1)
            {
                closest = possDir.get(0);
            } else if (possDir.size() > 1)
            {
                //Trim the possibilities that wont bring us closer
                List<Point> possDirCloser = wichBringCloserFromGoal(possDir);

                //if any point is goal, go to it!!!
                closest = getPointThatAreGoal(possDir);

                //if any point are adjacent to goal, go to it!
                if (closest == null)
                    closest = getPointAdjacentToGoal(possDir);

                //if the next point in the same direction bring me closer then go for it
                // except if it's 1 square farther to the goal than an other
                boolean tooFar;
                if (!possDirCloser.isEmpty()){
                    double dist = closestDistanceFromGoal(getNextPoint(dir));
                    Point closestD = closestFromGoal(possDirCloser);
                    double dist2 = closestDistanceFromGoal(closestD);
                    tooFar = Math.abs(dist - dist2) == 1;
                } else 
                    tooFar = false;

                if (closest == null && possDirCloser.contains(getNextPoint(dir)) && !tooFar)
                    closest = getNextPoint(dir);

                //else get the closest from whom bring you closer
                if (closest == null)
                    closest = closestFromGoal(possDirCloser);

                //else if we can go strait forward then go
                if (closest == null && possDir.contains(getNextPoint(dir)))
                    closest = getNextPoint(dir);

                //else go for the one that bring me closer
                if (closest == null)
                    closest = closestFromGoal(possDir);
            }

            if (closest != null)
            {
                int horizontal = pos.x - closest.x;
                int vertical = pos.y - closest.y;
                //TODO optimise the energy consumption: make it go backward if less consuming
                if (horizontal == 1)
                {
                    if (Compass.WEST == dir.getOpposite())
                        move(MOVE.BACKWARD);
                    else {
                        //go west
                        turn(Compass.WEST);
                        move(MOVE.FORWARD);
                    }
                }   
                else if (horizontal == -1)
                {
                    if (Compass.EAST == dir.getOpposite())
                        move(MOVE.BACKWARD);
                    else {
                        //go east
                        turn(Compass.EAST);
                        move(MOVE.FORWARD);
                    }
                }
                else if (vertical == 1)
                {
                    if (Compass.NORTH == dir.getOpposite())
                        move(MOVE.BACKWARD);
                    else {
                        //go north
                        turn(Compass.NORTH);
                        move(MOVE.FORWARD);
                    }
                }
                else if (vertical == -1)
                {
                    if (Compass.SOUTH == dir.getOpposite())
                        move(MOVE.BACKWARD);
                    else {
                        //go south
                        turn(Compass.SOUTH);
                        move(MOVE.FORWARD);
                    }
                }
                continue;
            }

            // There is no new path possible, then try old path
            // Fin the less used path

            // set the new goal at the last possibility
            // set the goal at the closest possibility from the goal, but that possibility should be a close range
            // TODO Make the close range also close in travel path! a case of a distance of 2 can take much more time to get to!!!
            closest = null;
            List<Point> smallestPath = null;
            int maxDistance = 0;
            while (closest == null || smallestPath == null){
                //TODO set the best augmentation for maxDistance
                maxDistance += 10;
                for (int index = possibilities.size()-1; index >= 0; index--) {
                    double distPossibilityToPosition = possibilities.get(index).distance(pos);
                    if (distPossibilityToPosition < maxDistance){
                        if (closest == null)
                        {
                            goal = closest = possibilities.get(index);
                            List<Point> smallPath = getSmallestPath();
                            if (smallestPath == null)
                                smallestPath = smallPath;
                            else if (smallPath.size() < smallestPath.size())
                                smallestPath = smallPath;
                        } else {
                            double distPossibilityToFinalGoal = possibilities.get(index).distance(finalGoal);
                            double distClosestToFinalGoal = closest.distance(finalGoal);
                            if (distPossibilityToFinalGoal < distClosestToFinalGoal)
                            {
                                goal = possibilities.get(index);
                                List<Point> smallPath = getSmallestPath();
                                if (smallestPath == null)
                                    smallestPath = smallPath;
                                else if (smallPath.size() < smallestPath.size())
                                    smallestPath = smallPath;
                            }
                        }
                    }
                }
            }

            if (smallestPath.size() > 0)
            {
                for (Point point : smallestPath) {
                    if (pos.distance(point) != 1)
                        bot.println("We have serious problem about aplying smallestPath! Next path isn't the next square!");
                    int horizontal = pos.x - point.x;
                    int vertical = pos.y - point.y;
                    //TODO optimise the energy consumption: make it go backward if less consuming
                    if (horizontal == 1)
                    {
                        //go west
                        turn(Compass.WEST);
                        move(MOVE.FORWARD);
                    }   
                    else if (horizontal == -1)
                    {
                        //go east
                        turn(Compass.EAST);
                        move(MOVE.FORWARD);
                    }
                    else if (vertical == 1)
                    {
                        //go north
                        turn(Compass.NORTH);
                        move(MOVE.FORWARD);
                    }
                    else if (vertical == -1)
                    {
                        //go south
                        turn(Compass.SOUTH);
                        move(MOVE.FORWARD);
                    }
                }
            }

            goal = finalGoal;
        }
    }
    
    /**
     * This will remove the possibilities where we know there's no issues.
     */
    private void removeUnusefullMap()
    {
        List<Point> stillPossible = new ArrayList<Point>();
        for (int i = 0; i < mazesize.y; i++)
        for (int j = 0; j < mazesize.x; j++)
            if (map[j][i] == UNKNOWN && !stillPossible.contains(new Point(j,i)))
            {
                //try to find path to the goal
                Point mpos = new Point(j, i);
                setMap(mpos, CHECKED);
                // Contain every points where we already gone
                List<Point> path = new ArrayList<Point>();
                Stack<Point> possibility = new Stack<Point>();
                possibility.add(mpos);
                
                boolean found = false;
                
                //while(!isGoal(mpos))
                while(!possibility.isEmpty())
                {
                    mpos = possibility.pop();
                    if (isNearGoal(mpos))
                        found = true;
                    if (getMap(mpos) == FREE)
                        setMap(mpos, FREECHECKED);
                    else
                        setMap(mpos, CHECKED);
                    
                    if (!path.contains(mpos))
                        path.add(mpos);
                    
                    boolean north = canGo(Compass.NORTH, mpos, map) == UNKNOWN || canGo(Compass.NORTH, mpos, map) == FREE;
                    boolean south = canGo(Compass.SOUTH, mpos, map) == UNKNOWN || canGo(Compass.SOUTH, mpos, map) == FREE;
                    boolean east = canGo(Compass.EAST, mpos, map) == UNKNOWN || canGo(Compass.EAST, mpos, map) == FREE;
                    boolean west = canGo(Compass.WEST, mpos, map) == UNKNOWN || canGo(Compass.WEST, mpos, map) == FREE;
                 
                    List<Point> posDir = new ArrayList<Point>();
                    if (north)
                        posDir.add(addPoints(mpos, Compass.NORTH.getVector()));
                    if (south)
                        posDir.add(addPoints(mpos, Compass.SOUTH.getVector()));
                    if (east)
                        posDir.add(addPoints(mpos, Compass.EAST.getVector()));
                    if (west)
                        posDir.add(addPoints(mpos, Compass.WEST.getVector()));
                    
                    for (Point point : posDir) {
                        if (!possibility.contains(point))
                            possibility.push(point);
                    }
                }
                
                if (found)
                {
                    for (Point point: path) {
                        stillPossible.add(point);
                    }
                } else {
                    //This path lead nowhere, mark it on map
                    for (Point point : path) {
                        setMap(point, UNREVELENT);
                        if (possibilities.contains(point))
                            possibilities.remove(point);
                    }
                }
                mapRemoveChecked();
            } else if (map[j][i] == FREE) {
                Point theCheckedOne = new Point(j,i);
                //if it's found all around then it's not interresting anymore
                Point north = addPoints(theCheckedOne, Compass.NORTH.getVector());
                Point south = addPoints(theCheckedOne, Compass.SOUTH.getVector());
                Point east = addPoints(theCheckedOne, Compass.EAST.getVector());
                Point west = addPoints(theCheckedOne, Compass.WEST.getVector());
                
                if (getMap(north) != UNKNOWN && getMap(south) != UNKNOWN &&
                    getMap(east) != UNKNOWN && getMap(west) != UNKNOWN &&
                    (getMap(north) != GOAL && getMap(south) != GOAL &&
                    getMap(east) != GOAL && getMap(west) != GOAL)){
                    setMap(theCheckedOne, 1);
                    if (possibilities.contains(theCheckedOne))
                        possibilities.remove(theCheckedOne);
                }
            }
    }
    
    private void mapRemoveChecked()
    {
        for (int i = 0; i < mazesize.y; i++)
        for (int j = 0; j < mazesize.x; j++)
            if (map[j][i] == CHECKED)
                setMap(new Point(j,i), UNKNOWN);
            else if (map[j][i] == FREECHECKED)
                setMap(new Point(j,i), FREE);
    }
    
    private Point closestFromGoal(List<Point> possArr)
    {
        //With or without rounding for dist? with allow to get the closest that is in the actual direction
        Point closest = null;
        double distance = 0;
        for (Point toTest : possArr) {
            for (Point goalPoint : goalPoints) {
                if (closest == null)
                {
                    closest = toTest;
                    distance = toTest.distance(goalPoint);
                } else if (toTest.distance(goalPoint) < distance)
                {
                    closest = toTest;
                    distance = toTest.distance(goalPoint);
                }
            }
        }
        
        return closest;
    }
    
    /**
     * Take an array of points and keep only the points that bring you closer to any goal point
     * @param possArr An array of points to test
     * @return The array of point closer from goal
     */
    private List<Point> wichBringCloserFromGoal(List<Point> possArr)
    {
        List<Point> closer = new ArrayList<Point>();
        for (Point toTest : possArr) {
            for (Point point : goalPoints) {
                if (toTest.distance(point) < pos.distance(point) && !closer.contains(toTest))
                    closer.add(toTest);
            }
        }
        return closer;
    }
    
    private boolean isGoal(Point ppos)
    {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Point ptGoal = addPoints(goal, new Point(i,j));
                if (ppos.distance(ptGoal) == 0)
                    return true;
            }
        }
        return false;
    }
    
    private Point getPointThatAreGoal(List<Point> possDir)
    {
        for (Point point : possDir) {
            if (isGoal(point))
                return point;
        }
        return null;
    }
    
    private boolean isNearGoal(Point ppos)
    {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Point ptGoal = addPoints(goal, new Point(i,j));
                if (ppos.distance(ptGoal) == 1)
                    return true;
            }
        }
        return false;
    }

    private Point getPointAdjacentToGoal(List<Point> possDir) {
        for (Point toTest : possDir) {
            if (isNearGoal(toTest))
                return toTest;
        }
        return null;
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
    
    private double closestDistanceFromGoal(Point pt)
    {
        double distance = -1;
        for (Point point : goalPoints) {
            if (distance == -1)
                distance = pt.distance(point);
            else if (pt.distance(point) < distance)
                distance = pt.distance(point);
        }
        return distance;
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
        while(possibilities.contains(pos))
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
            //bot.println("ERROR: bot should know the following coord as he should have looked it: " + next);
        return UNKNOWN;
    }

    private int canGo(Compass direction,Point pPos, int[][] pmap)
    {
        //Can we go? Check on map, if unknown then check with bot
        Point next = addPoints(pPos, direction.getVector());
        if (next.x < 0 || next.y < 0 || next.x >= mazesize.x  || next.y >= mazesize.y)
            return OUTOFMAZE;
        if (getMap(next,pmap) != UNKNOWN)
            //can move
            return getMap(next,pmap);
        else 
            //bot.println("ERROR: bot should know the following coord as he should have looked it: " + next);
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
    
    private Compass getDirection(Point point) {
        int horizontal = pos.x - point.x;
        int vertical = pos.y - point.y;
        
        if (horizontal == 1)
        {
            return Compass.WEST;
        }   
        else if (horizontal == -1)
        {
            return Compass.EAST;
        }
        else if (vertical == 1)
        {
            return Compass.NORTH;
        }
        else if (vertical == -1)
        {
            return Compass.SOUTH;
        }
        //return the actual direction if it couldn't detect wich direction it was
        return dir;
    }

    private void setMap(Point pPos, int value)
    {
        if (pPos.x < 0 || pPos.x >= mazesize.x ||
            pPos.y < 0 || pPos.y >= mazesize.y)
            return;
        map[pPos.x][pPos.y] = value;
        if (value == 0)
            visualMap.setValueAt("", pPos.y, pPos.x);
        else
            visualMap.setValueAt(value, pPos.y, pPos.x);
        if (value == FREE)
            if (!possibilities.contains(pPos))
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
                if (pmap[j][i] != 0)
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
        int[][] smap = new int[mazesize.x][mazesize.y];
        List<Point> spossibilities = new ArrayList<Point>();
        Stack<Point> path = new Stack<Point>();
        
        //Prepare smap
        for (int i = 0; i < mazesize.x; i++) {
            for (int j = 0; j < mazesize.y; j++) {
                if (map[i][j] > 0 || map[i][j] == -1)
                    smap[i][j] = SMAPKNOWN;
                else
                    smap[i][j] = map[i][j];
            }
        }
        
        setMap(spos, 1, smap);
        
        //while (spos != goal)
        while (spos.distance(goal) > 1)
        {
            drawMap(smap);
            // Where can I go that I haven't tryed -> List possible direction
            boolean north = canGo(Compass.NORTH, spos, smap) == SMAPKNOWN;
            boolean south = canGo(Compass.SOUTH, spos, smap) == SMAPKNOWN;
            boolean east = canGo(Compass.EAST, spos, smap) == SMAPKNOWN;
            boolean west = canGo(Compass.WEST, spos, smap) == SMAPKNOWN;
            List<Point> posDir = new ArrayList<Point>();
            if (north)
                posDir.add(addPoints(spos, Compass.NORTH.getVector()));
            if (south)
                posDir.add(addPoints(spos, Compass.SOUTH.getVector()));
            if (east)
                posDir.add(addPoints(spos, Compass.EAST.getVector()));
            if (west)
                posDir.add(addPoints(spos, Compass.WEST.getVector()));
            // Wich bring me closest?
            Point closest = null;
            for (int i = 0; i < posDir.size(); i++) {
                if (closest == null)
                    closest = posDir.get(i);
                if (posDir.get(i).distance(goal) < closest.distance(goal))
                    closest = posDir.get(i);
            }
            if (closest != null)
            {
                // Remove closest to the list
                posDir.remove(closest);
                // put the other to possibility in the list
                for (Point point : posDir) {
                    if (!spossibilities.contains(point))
                        spossibilities.add(point);
                }
                // Try the path
                spos = closest;
                setMap(spos, 1, smap);
                while (spossibilities.contains(spos))
                    spossibilities.remove(spos);
                path.push(spos);
                continue;
            }
            else
            {
                // If I can't go anywhere, get the first possibility
                closest = spossibilities.get(spossibilities.size()-1);
                if (closest != null)
                {
                    // Pop the stack untill I can reach it (wich meet a distance vector of 1)
                    do{
                        spos = path.pop();
                    } while (spos.distance(closest) != 1);
                    path.push(spos);
                    continue;
                } else {
                    bot.println("We are in trouble, we can't find the closest path!");
                }
            }
            // Continue
        }
        //draw back the map
        drawMap(map);
        return path;
    }

    private void checkForCharger() {
//        if (!charged)
        
        //see if you looked for charger before
        //if not, check for it
        //else is the last time you checked is farther than 10
        //check again
        
        //if the charger is found, check if it's closer than goal
        //if it is, put it as the goal
        //else just continue
        //try to get the map coordinate of it
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

