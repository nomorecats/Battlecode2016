package zombomb; //So the program knows what to call our bot

import battlecode.common.*; //imports Battlecode UI
import java.util.Random;	//Use this instead of Math.random(); seeded by the robot's id so more likely to be random than Math.random
import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;

public class RobotPlayer{
	/**
	 * Useful members
	 *
	 * $rc: the RobotController for this robot. Static so all methods can use it
	 * $randall: our source of all randomness
	 * $ourTeam: our Team, to save bytecodes
	 * $opponentTeam: Opponent's (NOT ZOMBIES) team
	 *
	 * $zombieDenLocations: locations of the zombie dens
	 * $enemyArchonIDs: the IDs of enemy archons
	 */
	static RobotController rc;
	static Random randall;
	static Team ourTeam;
	static Team opponentTeam;
	static int[] zombieRounds;
	static MapLocation[] zombieDenLocations;
	static Collection<Integer> enemyArchonIDs;
	// Collection of <Archon ID, Archon Location, Round that measurement was taken>
	static Collection<Triple<Integer,MapLocation,Integer>> mostRecentEnemyArchonLocations;

	/**
	 * run
	 *
	 * @param r: the RobotController passed in by the battlecode software. stored in $rc
	 * @selftype: stores our type so we don't have to call getType() each time, saving bytecodes
	 * @s: the innerclass instance of our robot
	 *
	 */
	public static void run(RobotController r){
		rc = r;
		randall = new Random(rc.getID());
		RobotType selftype = rc.getType();
		ourTeam = rc.getTeam();
		opponentTeam = ourTeam.opponent();
		if(selftype == RobotType.ARCHON){
			Archon s = new RobotPlayer().new Archon();
			s.run();
		}else if(selftype == RobotType.SCOUT){
			Scout s = new RobotPlayer().new Scout();
			s.run();
		}
		else if(selftype == RobotType.GUARD) {
			Guard s = new RobotPlayer().new Guard();
			s.run();
		}
		else if(selftype == RobotType.SOLDIER) {
			Soldier s = new RobotPlayer().new Soldier();
			s.run();
		}
	}
	
	/**
	 * Class Soldier
	 * 
	 * The class outlining our soldier bots
	 * 
	 */
	private class Soldier {
		
		public Soldier() {
		}
		
		public void run() {
			while(true) {
				try {
					// Use Guard AI (move out) until there are enough soldiers ammassed around, then go towards enemy archon and attack
					Signal[] signals = rc.emptySignalQueue();
					if (signals.length > 0) {
						for (Signal s : signals) {
							// receive a message containing enemy archon ID
							if (s.getTeam() == ourTeam) {
								FancyMessage f = FancyMessage.getFromRecievedSignal(s);
								
							}
							// intercept a message containing enemy archon location
							if (s.getTeam() == opponentTeam && enemyArchonIDs.contains(s.getID())) {
								FancyMessage f = FancyMessage.getFromRecievedSignal(s);
								
							}
							if (s.getTeam() == ourTeam && rc.senseRobot(s.getID()).type == RobotType.ARCHON) {
								FancyMessage f = FancyMessage.getFromRecievedSignal(s);
								MapLocation archonLocation = f.senderLocation;
								Direction archonDirection = rc.getLocation().directionTo(archonLocation);
								Direction oppositeDirection = RESOURCE_FUNCTIONS.getOpposite(archonDirection);
								if (rc.isCoreReady() && rc.canMove(oppositeDirection)) {
									rc.move(oppositeDirection);
								}
							}
						}
					}
					if(rc.isWeaponReady()){
						RobotInfo[] robots = rc.senseNearbyRobots();
						for(RobotInfo robot: robots) {
							if((robot.team == Team.ZOMBIE || robot.team == opponentTeam) && rc.canAttackLocation(robot.location)) {
								rc.attackLocation(robot.location);
								break;
							}
						}
					}
					// If there are enough scouts around, move out towards enemy Archon
					if (mostRecentEnemyArchonLocations.size() > 0 && RESOURCE_FUNCTIONS.numberOfRobotsInRadiusAndThoseRobots(RobotType.SOLDIER, RobotType.SOLDIER.sensorRadiusSquared, rc.getTeam()).first > 5) {
						RESOURCE_FUNCTIONS.BUG(RESOURCE_FUNCTIONS.mostRecentEnemyArchonLocation());
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/** 
	 * Class Guard
	 * 
	 * The class outlining our Guard bots
	 * 
	 */
	private class Guard {

		public Guard() {
		}

		public void run() {
			while(true){
				try{
					Signal[] signals = rc.emptySignalQueue();
					if(signals.length > 0){ //if == 0, no signals, so not ready
						for(Signal s: signals){
							if(s.getTeam() == ourTeam && rc.senseRobot(s.getID()).type == RobotType.ARCHON){
								FancyMessage f = FancyMessage.getFromRecievedSignal(s);
								MapLocation archonLocation = f.senderLocation;
								Direction archonDirection = rc.getLocation().directionTo(archonLocation);
								Direction oppositeDirection = RESOURCE_FUNCTIONS.getOpposite(archonDirection);
								if(rc.isCoreReady()){
									if(rc.canMove(oppositeDirection)){
										rc.move(oppositeDirection);
									}
								}
							}
						}
					}
					if(rc.isWeaponReady()){
						RobotInfo[] robots = rc.senseNearbyRobots();
						for(RobotInfo robot: robots) {
							if((robot.team == Team.ZOMBIE || robot.team == opponentTeam) && rc.canAttackLocation(robot.location)) {
								rc.attackLocation(robot.location);
								break;
							}
						}
						//rc.move(Direction.EAST);
					}
					Clock.yield();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * class Archon
	 *
	 * The class outlining our Archon bots
	 *
	 */
	private class Archon{

		/**
		 * Constructor
		 *
		 */
		public Archon(){
			zombieRounds = rc.getZombieSpawnSchedule().getRounds();
		}

		/**
		 * run
		 *
		 * @while loop: prevents this method from ending. method ending == robot dies D:
		 * @try-catch: catches any errors, prints stack trace, but keeps running.
		 *
		 */
		public void run(){
			while(true){
				try{
					//If it can, always tries to build Scouts.
					if(rc.isCoreReady()){
						if(rc.getRoundNum() % 100 == 0){
							FancyMessage.sendMessage(1, 1, 1, 3);
						}
						RobotType type = RESOURCE_FUNCTIONS.chooseRobotType();
						if(RESOURCE_FUNCTIONS.tryBuild(type)){ //See function in RESOURCE_FUNCTIONS to know what it does
							//After building scout, waits a turn, then signals it the location, so it has a good idea of where base is
							Clock.yield();
							FancyMessage.sendMessage(0,0b11101,31,30);
						}
					}
					Clock.yield();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * class Scout
	 *
	 * The class outlining our Scout bots
	 *
	 * @base: where the Archon that spawned it is located. This is how we know where to herd zombies away from
	 * @disciples: the number of zombies that are within sight range and following us.
	 * @last: the last tile the robot was on
	 *
	 */
	private class Scout{
		MapLocation base;
		int disciples;
		MapLocation last;

		/**
		 * Constructor
		 *
		 * initializes disciples value.
		 *
		 */
		public Scout(){
			disciples = 0;
		}

		/**
		 * run
		 *
		 * @while,try: same purpose as Archon.run
		 * @signals: all signals in queue. should be updated to make sure its just from our archons, but basically averages all this stuff out so incase several archons tell it where they are it will avoid all of them
		 * @approxxCoordinates: averages all (assumed) archon coordinates
		 *
		 */
		public void run(){
			while(true){
				try{
					//Do nothing until base-line information is gathered: ie, where are the archons. in future, archons may also give message assigning role
					if(base == null){
						Signal[] signals = rc.emptySignalQueue();
						if(signals.length > 0){ //if == 0, no signals, so not ready
							Tuple<Integer,Integer> approxxCoordinates = new Tuple<Integer,Integer>(0,0);
							//averages the signal's origins
							int counted = 0;
							for(int i = 0; i < signals.length; i++){
								if(signals[i].getTeam() == rc.getTeam() && rc.senseRobot(signals[i].getID()).type == RobotType.ARCHON){
									FancyMessage f = FancyMessage.getFromRecievedSignal(signals[i]);
									rc.setIndicatorString(0,"Type:" + f.type + "::Key:" + f.key);
									rc.setIndicatorString(1,"Info:" + Arrays.toString(f.bits));
									approxxCoordinates.first += f.senderLocation.x;
									approxxCoordinates.second += f.senderLocation.y;
									counted++;
								}
							}
							if(counted > 0){
								approxxCoordinates.first /= counted;
								approxxCoordinates.second /= counted;

								//sets @base to this
								base = new MapLocation(approxxCoordinates.first,approxxCoordinates.second);
								//rc.setIndicatorString(0,"x:" + base.x + "::y:" + base.y);
							}
						}
					}else{
						//If initialized, checks to make sure it isn't losing its herd
						if(stillHerding()){
							//if they're still following, it tries to move
							if(rc.isCoreReady()){
								MapLocation temp = rc.getLocation();
								if(RESOURCE_FUNCTIONS.moveAsFarAwayAsPossibleFrom(base)){//See RESOURCE_FUNCTIONS for details
									last = temp;
								}
							}
							//Otherwise, moves back to where it last was to try to regain them
						}else if(last != null){
							if(rc.isCoreReady()){
								if(rc.canMove(rc.getLocation().directionTo(last))){
									MapLocation temp = rc.getLocation();
									rc.move(temp.directionTo(last));
									last = temp;
								}
							}
						}
					}
					Clock.yield();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}

		/**
		 * boolean stillHerding
		 *
		 * makes sure the bot doesn't lose its herd
		 *
		 * @zombos: array of all zombies within sight
		 * @return true if there's at least 3 zombies, or at least as many as there used to be, false otherwise
		 *
		 */
		public boolean stillHerding(){
			RobotInfo[] zombos = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared,Team.ZOMBIE);
			if(zombos.length >= 3 || zombos.length >= disciples){
				disciples = zombos.length;
				return true;
			}
			return false;
		}
	}

	/**
	 * $class RESOURCE_FUNCTIONS
	 *
	 * Not a class to instantiate, but rather one with some useful functions to use in various robots
	 *
	 */
	public static class RESOURCE_FUNCTIONS{

		/**
		 * Direction intToDir
		 *
		 * Simplifies choosing a random direction
		 *
		 * @param i: the integer to convert
		 * @return: the Direction corresponding to that integer
		 *
		 */
		public static Direction intToDir(int i){
			Direction d = Direction.NORTH;
			for(int j = 0; j < i; j++){
				d = d.rotateRight();
			}
			return d;
		}

		/**
		 * MapLocation scanArchonLocation
		 *
		 * @robots: list of all visible enemy robots
		 * @pos: tracks position of Archon in @robots
		 * @return MapLocation of last Archon in list if it exists, null if no Archon is seen.
		 *
		 */
		public static MapLocation scanArchonLocation() {
			RobotInfo[] robots;
			robots = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, opponentTeam);
			for(int i = 0; i < robots.length; i++) {
				if(robots[i].type == RobotType.ARCHON) {
					return robots[i].location;
				}
			}
			return null;
		}

		/**
		 * int dirToInt
		 *
		 * Reverses intToDir
		 *
		 * @param d: Direction to convert
		 * @return: the integer corresponding to the Direction
		 *
		 */
		public static int dirToInt(Direction d){
			if(d.equals(Direction.NONE)){
				return -1;
			}
			int i = 0;
			while(!d.equals(Direction.NORTH)){
				d = d.rotateLeft();
				i++;
			}
			return i;
		}

		/**
		 * boolean tryBuild
		 *
		 * failable build method. Attempts to construct a robot in a space adjacent to the caller
		 *
		 * @param rt: the RobotType of the bot to be built
		 * @param startDirection: optional parameter, inferred to be a random direction if not given. First direction to be checked
		 * @return true if robot is constructed, false otherwise
		 *
		 */
		public static boolean tryBuild(RobotType rt) throws GameActionException{
			return tryBuild(rt,intToDir(randall.nextInt(8)));
		}
		public static boolean tryBuild(RobotType rt,Direction startDirection) throws GameActionException{
			for(int i = 0; i < 8; i++){
				if(rc.canBuild(startDirection,rt)){
					rc.build(startDirection,rt);
					//System.out.println("BUILT!");
					return true;
				}
				startDirection = startDirection.rotateRight();
				//System.out.println(startDirection);
			}
			return false;
		}

		/**
		 * boolean tryAttackLocation
		 * 
		 * failable attack method. Attempts to attack a robot at given map location.
		 * 
		 * @param loc	the location on the map to attack
		 * @return the success of attacking (true if successful, false if unsuccessful)
		 * 
		 */
		public static boolean tryAttackLocation(MapLocation loc) throws GameActionException {
			boolean canAttack = rc.canAttackLocation(loc);
			if (canAttack) {
				rc.attackLocation(loc);
			}
			return canAttack;
		}

		/**
		 * MapLocation findLargestPileOfParts
		 * 
		 * finds the largest pile of parts that is visible to the robot.
		 * 
		 * @return a Tuple containing the location of the largest pile of parts and the size of that pile
		 */
		public static Tuple<MapLocation, Double> findLargestPileOfParts() {
			// create an array of map locations which are visible to the robot
			MapLocation myLocation = rc.getLocation();
			int sensingRadiusSquared = rc.getType().sensorRadiusSquared;
			MapLocation[] visibleLocations = MapLocation.getAllMapLocationsWithinRadiusSq(myLocation, sensingRadiusSquared);

			// find the largest pile of parts
			double maxPileSize = 0;
			MapLocation maxPileLocation = myLocation;
			for (MapLocation loc : visibleLocations) {
				double currentPileSize = rc.senseParts(loc);
				if (currentPileSize > maxPileSize) {
					maxPileSize = currentPileSize;
					maxPileLocation = loc;
				}
			}

			// create Tuple
			Tuple<MapLocation, Double> locationAndSize = new Tuple<MapLocation, Double>(maxPileLocation, maxPileSize);

			return locationAndSize;
		}

		/**
		 * boolean moveAsFarAwayAsPossibleFrom
		 *
		 * Does what it says on the tin. failable. attempts to move to the furthest viable spot from a location
		 *
		 * @param epicenter: the MapLocation to move away from
		 * @current: the current location of the bot
		 * @choices: an array of all possible adjacent choices
		 * @distances: an array of ints that corresponds to @choices, has the squared distance of each choice from @epicenter
		 * @ranked: an array of indexes for @choices, ranked from furthest to closest by a kind of insertion sort
		 * @return true if robot moves, false if not
		 *
		 */
		public static boolean moveAsFarAwayAsPossibleFrom(MapLocation epicenter) throws GameActionException{
			MapLocation current = rc.getLocation();
			MapLocation[] choices = MapLocation.getAllMapLocationsWithinRadiusSq(current,3);
			int[] distances = new int[choices.length];
			int[] ranked = new int[choices.length];
			//populates @distances
			for(int i = 0; i < choices.length;i++){
				distances[i] = choices[i].distanceSquaredTo(epicenter);
			}
			//populates @ranked
			for(int i = 0; i < choices.length; i++){
				int max = -1;
				int accmax = -1;
				//finds largest remaining value in @distances, makes @ranked[i] that index
				for(int j = 0; j < choices.length; j++){
					if(distances[j] > accmax){
						max = j;
						accmax = distances[j];
					}
				}
				ranked[i] = max;
				distances[max] = -2; //prevents chosing the same location several times
			}
			//attempts to move to each space, starting at the furthest, until it does move, upon which it returns true.
			for(int i = 0; i < ranked.length; i++){
				if(rc.canMove(current.directionTo(choices[ranked[i]]))){
					rc.move(current.directionTo(choices[ranked[i]]));
					return true;
				}
			}
			//returns false if it can't move
			//rc.setIndicatorString(1,"max:none");
			return false;
		}

		/**
		 * RobotType chooseRobotType
		 * @param none
		 * @return RobotType that will be produced
		 */
		public static RobotType chooseRobotType() {
			for(int i: zombieRounds){
				int currentRound = rc.getRoundNum();
				if(i-currentRound<=15 && i-currentRound>=0){
					return RobotType.SCOUT;
				}
			}
			if(Math.random()*3>1) {
				return RobotType.SCOUT;
			}
			if(numberOfRobotsInRadiusAndThoseRobots(RobotType.GUARD,3,ourTeam).first == 7){
				return RobotType.SCOUT;
			}
			return RobotType.GUARD;
		}

		/**
		 * Returns the number of robots within a given radius squared
		 * @param type the type of robot to look for
		 * @param radiusSqr the squared radius
		 * @param team the team the robot should be on
		 * @return a tuple containing the number of robots nearby and the array of all robots nearby
		 */
		public static Tuple<Integer, RobotInfo[]> numberOfRobotsInRadiusAndThoseRobots(RobotType type,int radiusSqr,Team team){
			int count = 0;
			RobotInfo[] robats = rc.senseNearbyRobots(radiusSqr,team);
			if(type == null){
				Tuple<Integer, RobotInfo[]> thingToReturn = new Tuple<Integer, RobotInfo[]>(robats.length, robats);
				return thingToReturn;
			}
			for(int i = 0; i < robats.length; i++){
				if(robats[i].type.equals(type)){
					count++;
				}
			}
			Tuple<Integer, RobotInfo[]> returnThing = new Tuple<Integer, RobotInfo[]>(count, robats);
			return returnThing;
		}

		/**
		 * Collects the ID of enemy archons within sight range
		 * adds the IDs to the static collection enemyArchonIDs
		 */
		public static void collectEnemyArchonID() {
			Tuple<Integer, RobotInfo[]> robs = numberOfRobotsInRadiusAndThoseRobots(RobotType.ARCHON, rc.getType().sensorRadiusSquared, opponentTeam);
			if (robs.first > 0) {
				for (RobotInfo rob : robs.second) {
					if (!enemyArchonIDs.contains(rob.ID)) {
						enemyArchonIDs.add(rob.ID);
					}
				}
			}
		}
		
		/**
		 * Get most recent enemy archon location
		 */
		public static MapLocation mostRecentEnemyArchonLocation() {
			MapLocation latestArchonLocation = new MapLocation(0,0);
			int latestRound = 0;
			for (Triple<Integer, MapLocation, Integer> trip : mostRecentEnemyArchonLocations) {
				if (trip.third > latestRound) {
					latestRound = trip.third;
					latestArchonLocation = trip.second;
				}
			}
			return latestArchonLocation;
		}

		/**
		 * MapLocation[] inSightButOffMap
		 *
		 * Looks at all tiles in sight range, returns all those that are off the map
		 *
		 * @return array of MapLocations in sight but not on the map
		 *
		 */
		public static MapLocation[] inSightButOffMap() throws GameActionException{
			MapLocation[] allInSight = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(),rc.getType().sensorRadiusSquared);
			int numOffMap = 0;
			for(int i = 0; i < allInSight.length; i++){
				if(rc.onTheMap(allInSight[i])){
					allInSight[i] = null;
				}else{
					numOffMap++;
				}
			}
			MapLocation[] ret = new MapLocation[numOffMap];
			int count = 0;
			for(int i = 0; i < allInSight.length && count < ret.length; i++){
				if(allInSight[i] != null){
					ret[count] = allInSight[i];
					count++;
				}
			}
			return ret;
		}

		/**
		 * boolean BUG
		 *
		 * Pathfinding method. Here's how it basically works:
		 * * * If we can move directly towards target, do that.
		 * * * Otherwise, we move along side of obstacle until we can move directly towards target
		 * * * * * Remember Branches (where we start/stop following walls. If we return to one, obviously following the one way didn't help, so we try the other one. If both are tried, we run the first way again for all following runs)
		 *
		 * @param target: where we are trying to get to
		 * @return true if we move, false if we don't
		 *
		 */
		public static boolean BUG(MapLocation target) throws GameActionException{
			rc.setIndicatorString(1,target.toString());
			if(!rc.isCoreReady()) return false;
			MapLocation current = rc.getLocation();
			Direction directionToTarget = current.directionTo(target);
			if(rc.canMove(directionToTarget) && (Branch.last == null || !Branch.last.contains(current.add(directionToTarget)))){
				rc.setIndicatorString(2,"Starting no branch // ");
				rc.setIndicatorString(0,current.add(directionToTarget).toString() + " from no branch");
				Branch.last = new ArrayList<MapLocation>();
				Branch.last.add(current);
				rc.move(directionToTarget);
				Branch.lastStatus = 0;
				if(current.add(directionToTarget).equals(target)){
					Branch.resetPath();
				}
				return true;
			}else if(Branch.lastStatus == 0){
				rc.setIndicatorString(2,"Starting choosing branch");
				Branch decision = Branch.fork(current,target);
				rc.setIndicatorString(1,"Made it past $fork");
				MapLocation bestChoice = decision.bestBranch();
				rc.setIndicatorString(1,"Made it past @bestBranch");
				if(bestChoice != null){
					rc.setIndicatorString(0,bestChoice.toString() + " from choosing branch");
					Branch.last.add(current);
					rc.move(current.directionTo(bestChoice));
					return true;
				}
				rc.setIndicatorString(0,"Failed in choosing branch");
				return false;
			}else if(Branch.lastStatus == 1){
				rc.setIndicatorString(2,"Starting right branch");
				Branch currentStep = new Branch(current,target);
				Branch temp = Branch.getFromEarlier(currentStep);
				if(temp != null){
					currentStep = temp;
				}
				MapLocation nextChoice = currentStep.getRightCanditate();
				if(nextChoice != null){
					rc.setIndicatorString(0,nextChoice.toString() + " from right branch");
					Branch.last.add(current);
					rc.move(current.directionTo(nextChoice));
					return true;
				}
				rc.setIndicatorString(0,"Failed in right branch");
				return false;
			}else if(Branch.lastStatus == -1){
				rc.setIndicatorString(2,"Starting left branch");
				Branch currentStep = new Branch(current,target);
				Branch temp = Branch.getFromEarlier(currentStep);
				if(temp != null){
					currentStep = temp;

				}
				MapLocation nextChoice = currentStep.getLeftCanditate();
				if(nextChoice != null){
					rc.setIndicatorString(0,nextChoice.toString() + " from left branch");
					Branch.last.add(current);
					rc.move(current.directionTo(nextChoice));
					return true;
				}
				rc.setIndicatorString(0,"Failed in choosing branch");
				return false;
			}
			rc.setIndicatorString(0,"Failed outside of branch");
			return false;
		}

		/**
		 * 
		 * MapLocation getOpposite
		 * @param A direction
		 * @return Returns the opposite direction of the given direction
		 * 
		 */
		public static Direction getOpposite(Direction archonDirection){
			if(archonDirection.equals(Direction.NORTH)){
				return Direction.SOUTH;
			}
			if(archonDirection.equals(Direction.SOUTH)){
				return Direction.NORTH;
			}
			if(archonDirection.equals(Direction.EAST)){
				return Direction.WEST;
			}
			if(archonDirection.equals(Direction.WEST)){
				return Direction.EAST;
			}
			if(archonDirection.equals(Direction.NORTH_EAST)){
				return Direction.SOUTH_WEST;
			}
			if(archonDirection.equals(Direction.NORTH_WEST)){
				return Direction.SOUTH_EAST;
			}
			if(archonDirection.equals(Direction.SOUTH_EAST)){
				return Direction.NORTH_WEST;
			}
			return Direction.NORTH_EAST;
		}
	}

	/**
	 * Tuple
	 * 
	 * a simple tuple class so that tuples can be used.
	 */
	public static class Tuple<X, Y> { 
		public X first; 
		public Y second; 
		public Tuple(X first, Y second) { 
			this.first = first; 
			this.second = second; 
		} 
	}
	
	/**
	 * Triple
	 * 
	 * a simple triple class so that triples can be used.
	 */
	public static class Triple<X, Y, Z> {
		public X first;
		public Y second;
		public Z third;
		public Triple(X first, Y second, Z third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}
	}

	/**
	 * class FancyMessage
	 * 
	 * To SEND a message, prepare data (either as two (24 and 32 bit maximum size respectively) integers or boolean[] of bits (max size 56)) and pass to appropriate @sendMessage with a type and radiusSqr
	 * To RECIEVE a message, pass the recieved signal (ONLY do with a signal from a friendly Archon/Scout) to @getFromRecievedSignal which returns a FancyMessage object, where you can access its various fields
	 *
	 * @senderID: the id of the robot that sent the Signal
	 * @senderLocation: the location of the robot that sent the signal
	 * @bits: an array of bits representing the information sent in the message
	 * @type: the type of the message
	 * @key: the key of the message
	 *
	 */
	public static class FancyMessage{
		public int senderID;
		public MapLocation senderLocation;
		public boolean[] bits;
		public Tuple<Integer,Integer> ints;
		private static Tuple<Integer,Integer> hiddenInts;
		public int type;
		public int key;
		public FancyMessage(){
		}

		/**
		 * FancyMessage getFromRecievedSignal
		 *
		 * Handles recieving of FancyMessages
		 *
		 * @param s: the Signal to decode
		 * @return FancyMessage that was recieved
		 * Also sets @key, @bits, @type, @senderID, @senderLocation, @ints
		 *
		 */
		public static FancyMessage getFromRecievedSignal(Signal s){
			FancyMessage ret = new FancyMessage();
			ret.senderID = s.getID();
			ret.senderLocation = s.getLocation();
			int[] is = s.getMessage();
			Tuple<Integer,boolean[]> info = decrypt(new Tuple<Integer,Integer>(is[0],is[1]));
			ret.type = info.first;
			ret.bits = info.second;
			ret.ints = hiddenInts;
			hiddenInts = null;
			ret.key = (is[0] & 0b11110000) >> 4;
			return ret;
		}

		/**
		 * boolean sendMessage
		 *
		 * OVERLOADED
		 *
		 * handles sending of FancyMessages
		 *
		 * @param type: the type of message being sent (4 bits)
		 * @param data: an array of booleans (only up to 56 elements) to be encoded
		 * @param first,second: the two ints to be encoded (24 bit max for first, 32 for second)
		 * @param encodeddata: for if your data is already encoded before sending
		 * @param radiusSqr: how far to send messsage
		 * @return true unless some sort of failure
		 *
		 */
		public static boolean sendMessage(int type,boolean[] data,int radiusSqr) throws GameActionException{
			Tuple<Integer,Integer> encoded = encrypt(type,data);
			if(encoded == null){
				return false;
			}
			return sendMessage(encoded,radiusSqr);
		}
		public static boolean sendMessage(int type,int first,int second,int radiusSqr) throws GameActionException{
			Tuple<Integer,Integer> encoded = encrypt(type,first,second);
			return sendMessage(encoded,radiusSqr);
		}
		public static boolean sendMessage(Tuple<Integer,Integer> encodeddata, int radiusSqr) throws GameActionException{
			rc.broadcastMessageSignal(encodeddata.first,encodeddata.second,radiusSqr);
			return true;
		}

		/**
		 * Tuple<Integer,boolean[]> decrypt
		 *
		 * Decodes signal to FancyMessage
		 *
		 * @param inputs: the two ints from the message
		 * @return Tuple containing the type of the message and the boolean[] containing its data
		 * also sets @ints
		 *
		 */
		public static Tuple<Integer,boolean[]> decrypt(Tuple<Integer,Integer> inputs){
			int typeIn = inputs.first & 0b1111;
			int keyIn = (inputs.first & 0b11110000) >> 4;
			int encryptor = 0;
			for(int i = 0; i < 8; i++){
				encryptor |= (keyIn & 0b1111) << (i * 4);
			}
			int first = (inputs.first ^ encryptor) >> 8;
				int second = (inputs.second ^ encryptor);
				hiddenInts = new Tuple<Integer,Integer>(first,second);
				boolean[] bit = new boolean[56];
				for(int i = 0; i < 24; i++){
					bit[i] = (first & (1 << i)) != 0;
				}
				for(int i = 0; i < 32; i++){
					bit[i + 24] = (second & (1 << i)) != 0;
				}
				return new Tuple<Integer,boolean[]>(typeIn,bit);
		}

		/**
		 * Tuple<Integer,Integer> encrypt
		 *
		 * OVERLOADED
		 *
		 * encodes data to send
		 *
		 * @param type: the type of message being sent
		 * @param data: array containing bits to be sent
		 * @param first,second: two ints to be sent (24b,32b)
		 * @return Tuple of ints containing encoded version of parameters
		 *
		 */
		public static Tuple<Integer,Integer> encrypt(int type,boolean[] data){
			if(data.length > 56){
				return null;
			}
			int first = 0;
			int second = 0;
			for(int i = 0; i < 24 && i < data.length; i++){
				if(data[i]){
					first |= (1 << i);
				}
			}
			for(int i = 0; i < 32 && i + 24 < data.length; i++){
				if(data[i + 24]){
					second |= (1 << i);
				}
			}
			return encrypt(type,first,second);
		}
		public static Tuple<Integer,Integer> encrypt(int type,int first,int second){
			int key = randall.nextInt(0b10000);
			int enc = 0;
			for(int i = 0; i < 8; i++){
				enc |= (key & 0b1111) << (i * 4);
			}
			first ^= enc;
			second ^= enc;
			first = first << 8;
			first |= (key & 0b1111) << 4;
			first |= type;
			return new Tuple<Integer,Integer>(first,second);
		}
	}

	/**
	 * class Branch
	 *
	 * Represents a branch in the BUG pathfinding
	 *
	 * 
	 *
	 */
	public static class Branch{
		public MapLocation branchPoint;
		public boolean branchedLeft = false,branchedRight = false;
		public MapLocation target;
		public static ArrayList<Branch> branchesInCurrentPath = new ArrayList<Branch>();
		public static int lastStatus = 0;
		public Direction lastDirection = null;
		private MapLocation leftCanditate;
		private MapLocation rightCanditate;
		private MapLocation bestCanditate;
		public static ArrayList<MapLocation> last = new ArrayList<MapLocation>();
		public Branch(MapLocation whereWeAre,MapLocation targe){
			branchPoint = whereWeAre;
			target = targe;
		}
		public static Branch fork(MapLocation here,MapLocation targe){
			Branch n = new Branch(here,targe);
			Branch e = getFromEarlier(n);
			if(e == null){
				return n;
			}
			return e;
		}
		public static Branch getFromEarlier(Branch n){
			for(int i = 0; i < branchesInCurrentPath.size(); i++){
				System.out.println(n.branchPoint.toString() + "::" + branchesInCurrentPath.get(i).branchPoint.toString());
				if(n.equal(branchesInCurrentPath.get(i))){
					return branchesInCurrentPath.get(i);
				}
			}
			return null;
		}
		public static void resetPath(){
			branchesInCurrentPath = new ArrayList<Branch>();
		}
		public MapLocation bestBranch(){
			if(branchedLeft && !branchedRight){
				lastStatus = 1;
				return getRightCanditate();
			}
			if(branchedRight && !branchedLeft){
				lastStatus = -1;
				return getLeftCanditate();
			}
			if(bestCanditate != null){
				lastStatus = bestCanditate.equals(leftCanditate) ? -1 : 1 ;
				return bestCanditate;
			}
			MapLocation left = getLeftCanditate();
			MapLocation right = getRightCanditate();
			if(left == null && right != null){
				lastStatus = 1;
				bestCanditate = right;
				return right;
			}else if(right == null && left != null){
				lastStatus = -1;
				bestCanditate = left;				return left;
			}else if(right == null && left == null){
				return null;
			}
			if(left.distanceSquaredTo(target) < right.distanceSquaredTo(target)){
				lastStatus = -1;
				bestCanditate = left;
				return left;
			}
			lastStatus = 1;
			bestCanditate = right;
			return right;
		}
		public MapLocation getLeftCanditate(){
			if(leftCanditate != null){
				return leftCanditate;
			}
			ArrayList<MapLocation> lefts = getAllLeftCanditates();
			if(lefts.size() == 0){
				return null;
			}
			int min = 0;
			int accmin = lefts.get(0).distanceSquaredTo(target);
			for(int i = 1; i < lefts.size(); i++){
				int newMin = lefts.get(i).distanceSquaredTo(target);
				if(accmin > newMin){
					min = i;
					accmin = newMin;
				}
			}
			return lefts.get(min);
		}
		public MapLocation getRightCanditate(){
			if(rightCanditate != null){
				return rightCanditate;
			}
			ArrayList<MapLocation> rights = getAllRightCanditates();
			if(rights.size() == 0){
				return null;
			}
			int min = 0;
			int accmin = rights.get(0).distanceSquaredTo(target);
			for(int i = 1; i < rights.size(); i++){
				int newMin = rights.get(i).distanceSquaredTo(target);
				if(accmin > newMin){
					min = i;
					accmin = newMin;
				}
			}
			return rights.get(min);
		}
		public ArrayList<MapLocation> getAllRightCanditates(){
			ArrayList<MapLocation> base = new ArrayList<MapLocation>();
			MapLocation[] options = MapLocation.getAllMapLocationsWithinRadiusSq(branchPoint,3);
			try{
				for(int i = 0; i < options.length; i++){
					boolean isRight = false;
					Direction forward = branchPoint.directionTo(options[i]);
					Direction[] rights = new Direction[]{RESOURCE_FUNCTIONS.intToDir((RESOURCE_FUNCTIONS.dirToInt(forward) + 5) % 8),RESOURCE_FUNCTIONS.intToDir((RESOURCE_FUNCTIONS.dirToInt(forward) + 6) % 8),RESOURCE_FUNCTIONS.intToDir((RESOURCE_FUNCTIONS.dirToInt(forward) + 7) % 8)};
					if(rc.senseRubble(options[i]) < 100 && rc.onTheMap(options[i]) && !branchPoint.equals(options[i])){
						for(int j = 0; j < rights.length && !isRight; j++){
							if(rc.senseRubble(options[i].add(rights[j])) > 100 && !last.contains(options[i])){
								isRight = true;
							}
						}
					}
					if(isRight){
						base.add(options[i]);
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			return base;
		}
		public ArrayList<MapLocation> getAllLeftCanditates(){
			ArrayList<MapLocation> base = new ArrayList<MapLocation>();
			try{
				MapLocation[] options = MapLocation.getAllMapLocationsWithinRadiusSq(branchPoint,3);
				for(int i = 0; i < options.length; i++){
					boolean isLeft = false;
					Direction forward = branchPoint.directionTo(options[i]);
					Direction[] lefts = new Direction[]{RESOURCE_FUNCTIONS.intToDir((RESOURCE_FUNCTIONS.dirToInt(forward) + 1) % 8),RESOURCE_FUNCTIONS.intToDir((RESOURCE_FUNCTIONS.dirToInt(forward) + 2) % 8),RESOURCE_FUNCTIONS.intToDir((RESOURCE_FUNCTIONS.dirToInt(forward) + 3) % 8)};
					if(rc.senseRubble(options[i]) < 100 && rc.onTheMap(options[i]) && !branchPoint.equals(options[i])){
						for(int j = 0; j < lefts.length && !isLeft; j++){
							if(rc.senseRubble(options[i].add(lefts[j])) > 100&& !last.contains(options[i])){
								isLeft = true;
							}
						}
					}
					if(isLeft){
						base.add(options[i]);
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			return base;
		}
		public boolean equal(Object other){
			Branch o = (Branch)other;
			System.out.println(this.branchPoint.toString() + "::" + o.branchPoint.toString());
			if(o.branchPoint.equals(this.branchPoint)){
				return true;
			}
			return false;
		}
	}
} 
