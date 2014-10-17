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
package bots.SampleBot;


// The following imports are required.
import amazebot2011.*;
import amazebot2011.BotInterface.*;

// These are optional, depending on your needs.
import static amazebot2011.utils.PointHelper.*;
import java.awt.Point;


// Your primary class must be called Brain and must implement BrainInterface.
// You are free to create other classes in other source files if you need.
// Since each Brain exists in its own package, there will never be conflicts.
public class Brain implements BrainInterface
{
	BotInterface	bot;		// a reference to a bot that your brain will control
	Point			goal;		// coordinates of the top-left cell of the 4x4 goal room
	Point			mazesize;	// dimensions of the maze
	Point			startpos;	// coordinates of the bot's starting position
	Compass			startdir;	// direction in which the bot faces at start
        Point                   pos;            // current bot's coordinates
        Compass                 dir;            // current bot's direction
        
        int[][]                 map;            // map representation of the maze

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
		goal = bot.getGoalCorner();
                
                // Set the goal on map
                map[goal.x][goal.y] = -1;
                map[goal.x][goal.y+1] = -1;
                map[goal.x+1][goal.y] = -1;
                map[goal.x+1][goal.y+1] = -1;

		// Get the starting position and direction.
		startpos = bot.getStartPosition();
		startdir = bot.getStartDirection();
                
                map[startpos.x][startpos.y] = 0;
                
                // Set the actual position and direction
                pos = startpos;
                dir = startdir;
                
                // Get the distance from the start position to the goal
                double distStartToGoal = goal.distance(pos);
                bot.println("Distance from Start to Goal: " + distStartToGoal);

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
                    // Get the direction that'll cost the least energy to bring us closer to the goal
                    Compass goalYDir;
                    Compass goalXDir;
                    
                    //double dist = goal.distance(pos);
                    
                    if (pos.x > goal.x)
                        goalXDir = dir.WEST;
                    else if (pos.x < goal.x)
                        goalXDir = dir.EAST;
                    else
                        goalXDir = null;
                    
                    if (pos.y > goal.y)
                        goalYDir = dir.NORTH;
                    else if (pos.y < goal.y)
                        goalYDir = dir.SOUTH;
                    else
                        goalYDir = null;
                    
                    if (dir == goalXDir)
                    {
                        //We are in the same direction that we have to go! That's ok!
                        //Can we go?
                        if (canGo(dir))
                        {
                            move(MOVE.FORWARD);
                            //go on
                            continue;
                        }
                    }
                    
                    if (dir == goalYDir)
                    {
                        //We are in the same direction that we have to go! That's ok!
                        //Can we go?
                        if (canGo(dir))
                        {
                            move(MOVE.FORWARD);
                            //go on
                            continue;
                        }
                    }
                    
                    
                    
		/*
                        // This tries to move the bot forward one cell. If it hits a wall, it returns false.
			boolean success = bot.move(MOVE.FORWARD);;

			if (!success)
			{
				// The bot can check if the cell ahead or to either side is clear:
				boolean clear = bot.look(LOOK.LEFT);

				// The bot can turn left or right.
				if (clear) bot.turn(TURN.LEFT);
				else bot.turn(TURN.RIGHT);
			}

			// As you can see if you run it, this is a very stupid bot.
                 * 
                 */
		}
	}
        
        private void move(MOVE move)
        {
            bot.move(move);
            if (move == MOVE.FORWARD)
                pos = addPoints(pos, dir.getVector());
            else
                pos = subPoints(pos, dir.getVector());
        }
        
        private void move(MOVE move, int count)
        {
            for (int i = 0; i < count; i++)
                move(move);
        }
        
        private boolean canGo(Compass direction)
        {
            //Can we go? Check on map, if unknown then check with bot
            Point next = addPoints(pos, direction.getVector());
            if (map[next.x][next.y] == 1)
                //can move
                return true;
            else if (map[next.x][next.y] == 2)
            {
                //can't move
                return false;
            } else if (bot.look(LOOK.AHEAD))
            {
                //can move
                //Update map
                map[next.x][next.y] = 1;
                return true;
            }
            //can't move
            //Update map
            map[next.x][next.y] = 2;
            return false;
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

