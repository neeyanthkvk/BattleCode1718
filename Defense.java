import bc.*;
import java.util.*;
@SuppressWarnings("unchecked")
public class Defense {
   //Basic Info 
   static long startTime = System.currentTimeMillis();
   static int maxRound = 1000;   // the number of rounds
   static Planet earth = Planet.Earth;
   static Planet mars = Planet.Mars;
   static Direction[] directions = Direction.values();
   static GameController gc = new GameController();;
   static PlanetMap eMap = gc.startingMap(earth);
   static PlanetMap mMap = gc.startingMap(mars);
   static int eWidth = (int) eMap.getWidth();
   static int eHeight = (int) eMap.getHeight();
   static int mWidth = (int) mMap.getWidth();
   static int mHeight = (int) mMap.getHeight();
   static Team myTeam = gc.team();
   static Team enemyTeam;
   static Random rand = new Random();
   
   //Unit Info
   static HashSet<Integer> myInit = new HashSet<Integer>();
   static HashMap<Integer, Pair> enemyInit = new HashMap<Integer, Pair>();
   static HashMap<Integer, Integer> initRegion = new HashMap<Integer, Integer>();
   static HashSet<Integer> myUnits;
   static HashSet<Integer> healers;
   static HashSet<Integer> factories;
   static HashSet<Integer> knights;
   static HashSet<Integer> mages;
   static HashSet<Integer> rangers;
   static HashSet<Integer> rockets;
   static HashSet<Integer> workers;
   static HashMap<Integer,Integer> typePerTask = new HashMap<Integer,Integer>();
   static HashMap<Integer, Task> tasks = new HashMap<Integer, Task>();
   static boolean[][] beingMined = new boolean[eWidth][eHeight];
   static HashMap<Integer,Integer> rocketLoad = new HashMap<Integer,Integer>();
   
   // Map Info
   static MapLocation[][] eMapLoc = new MapLocation[eWidth][eHeight];
   static long[][] karbDep = new long[eWidth][eHeight];//amount of karbonite in the square
   static long[][] karbAdj = new long[eWidth][eHeight];//sum of karbonite on and adjacent to the square
   static boolean[][] passable = new boolean[eWidth][eHeight];
   static Path[][][][] paths = new Path[eWidth][eHeight][eWidth][eHeight];
   static HashMap<Integer, PriorityQueue<KarbAdjacent>> bestKarbAdj = new HashMap<Integer, PriorityQueue<KarbAdjacent>>();

   // temp stuff
   static int primary = -1;
   static int primaryTarget = -1;
   static boolean enemyConnected = false;
   //
      
      
   static {
      try {
      //update units
         updateUnits();
      
      //initializes teams
         if(myTeam==Team.Red)
            enemyTeam = Team.Blue;
         else
            enemyTeam = Team.Red;
      
      //initializes eMapLoc, karbDep, and passable
         System.out.println("Initializing basic map info");
         for(int x = 0; x < eWidth; x++)
            for(int y = 0; y < eHeight; y++) 
               try {
                  eMapLoc[x][y] = new MapLocation(earth,x,y);
                  karbDep[x][y] = eMap.initialKarboniteAt(eMapLoc[x][y]);
                  passable[x][y] = eMap.isPassableTerrainAt(eMapLoc[x][y])!=0;
               } catch(Exception e) {e.printStackTrace();}
         System.out.println("Finished initializing basic map info");
               
      //initializes myInit and enemyInit
         System.out.println("Initializing all initial units");
         VecUnit allInit = eMap.getInitial_units();
         for(int i = 0; i < allInit.size(); i++)
            try {
               if(myUnits.contains(allInit.get(i).id()))
                  myInit.add(allInit.get(i).id());
               else
                  enemyInit.put(allInit.get(i).id(), mapPair(allInit.get(i).location().mapLocation()));
            } catch(Exception e) {e.printStackTrace();}
         System.out.println("Finished initializing all initial units");
            
         for(int w: myInit)
            System.out.println("My initial workers are: "+w); 
            
      //initializes karbAdj
         System.out.println("Finding karbonite sums");
         for(int x = 0; x < eWidth; x++)
            for(int y = 0; y < eHeight; y++)
               try {
                  karbAdj[x][y] = countKarbAdj(x,y);
               } catch(Exception e) {e.printStackTrace();}
         System.out.println("Finished finding karbonite sums");
            
      //initializes tasks
         System.out.println("Initializing tasks");
         for(int x: myInit)
            try {
               tasks.put(x, new Task(x, 0));
            } catch(Exception e) {e.printStackTrace();}
         System.out.println("Finished initializing tasks");     
         
      //initializes primary
         System.out.println("Finding primary worker");
         init: for(int start: myUnits)
            try {
               primary = start;
               for(int target: enemyInit.keySet())
               {
                  Pair p1 = unitPair(start);
                  Pair p2 = enemyInit.get(target);
                  System.out.println("Going from "+p1+" to "+p2);
                  paths[p1.x][p1.y][p2.x][p2.y] = findPath(p1, p2, null);
                  if(paths[p1.x][p1.y][p2.x][p2.y]!=null)
                  {   
                     System.out.println("Found path for primary worker");
                     tasks.get(primary).taskType = 1;
                     enemyConnected = true;
                     primaryTarget = target;
                     break init;
                  }
               }
            } catch(Exception e) {e.printStackTrace();}
         System.out.println("Primary worker found: "+primary);
      
         
      //initializes bestKarbAdj
         System.out.println("Finding best mining location");
         for(int id: myUnits)
            try {
               bestKarbAdj.put(id, countMaxKarbAdj(unitPair(id)));
            } catch(Exception e) {e.printStackTrace();}
         System.out.println("Finished finding best mining location");
            
      //Research
         System.out.println("Queueing research");
         /* Start Strategy 1 - Grind & Defend
          * Generate as much Karbonite as Possible
          * Have Healers, Rangers, and Knights as defense for Rangers
          * Late-Game: No need to Dominate Earth - Attempt to Dominate Mars Instead. Bring Mages, Healers, and Knights to dominate Mars. <----- WIN CONDITION
          * */
         gc.queueResearch(UnitType.Worker);  // 25 Rounds - "Gimme some of that Black Stuff"
         gc.queueResearch(UnitType.Worker);  // 75 Rounds - "Time is of the Essence"
         gc.queueResearch(UnitType.Ranger);  // 25 Rounds - "Get in Fast"
         gc.queueResearch(UnitType.Worker);  // 75 Rounds - "Time is of the Essence II"
         gc.queueResearch(UnitType.Healer);  // 25 Rounds - "Spirit Water"
         gc.queueResearch(UnitType.Mage);    // 25 Rounds - "Glass Cannon"
         gc.queueResearch(UnitType.Rocket);  // 100 Rounds - "Rocketry"
         gc.queueResearch(UnitType.Knight);  // 25 Rounds - "Armor"
         gc.queueResearch(UnitType.Knight);  // 75 Rounds - "Even More Armor"
         gc.queueResearch(UnitType.Mage);    // 75 Rounds - "Glass Cannon II"
         gc.queueResearch(UnitType.Rocket);  // 100 Rounds - "Rocket Boosters" (625 Rounds)
         gc.queueResearch(UnitType.Mage);    // 100 Rounds - "Glass Cannon III" (725 Rounds)
         //////////////////////////////////////
         //         EARTH IS GONE            //
         //////////////////////////////////////
         gc.queueResearch(UnitType.Healer);
         gc.queueResearch(UnitType.Ranger);
         /*End Strategy 1*/
         System.out.println("Finished queueing research");
         
      } catch(Exception e) {e.printStackTrace();}
   }
   public static void main(String[] args) {
      if(gc.planet().equals(Planet.Earth))
         earth();
      else
         mars();
   }
   public static void earth() {
      int curRound = 1;
      //1st round
      try {
         System.out.println("Earth Round "+gc.round()+": ");
         System.out.println("Time used: "+(System.currentTimeMillis()-startTime));
         for(int id: myUnits)
            if(id==primary)
            {
               Pair p1 = unitPair(primary);
               Pair p2 = enemyInit.get(primaryTarget);
               tasks.get(primary).startMoving(p2);
               move(id, false);
               tasks.get(id).stopMoving();
               int buildID = blueprint(primary, UnitType.Factory, oppositeDirection(paths[p1.x][p1.y][p2.x][p2.y].seq.get(0)));
               tasks.get(id).startBuilding(buildID);
               tasks.get(id).doTask();
            }
            else
            {
               tasks.get(id).startMoving(bestKarbAdj.get(id).remove().loc);
               tasks.get(id).startMining();
               tasks.get(id).doTask();
            }
         curRound++;
      } catch(Exception e) {e.printStackTrace();}
      gc.nextTurn();
      //rounds 2-1000
      while(gc.round()<=maxRound)
      {
         try
         {
            System.out.println("Earth Round "+gc.round()+": ");
            System.out.println("Time used: "+(System.currentTimeMillis()-startTime));
            updateUnits();
           
            for(int id: workers)
            {
               if(tasks.get(id).getTask()==-1)
               {
                  int type = tasks.get(id).taskType;
                  if(type==0)
                  {
                  }
                  throw new Exception("wait what? "+id);
               }
               if(tasks.get(id).moveTarget!=null&&move(id, false)==1)
               {
                  tasks.get(id).stopMoving();
               }
               int task = tasks.get(id).getTask();
               int val = tasks.get(id).doTask();
               if(task==0&&val==-1)
               {
                  System.out.println("finished mining");
                  tasks.get(id).startMoving(bestKarbAdj.get(id).remove().loc);
               }
               if(task==1&&val==-2)
               {
                  Direction d = null;
                  int buildID = blueprint(id, UnitType.Factory, d);
                  tasks.get(id).startBuilding(buildID);
               }
               if(task==2&&val==-3) {
            	   Direction d = null;
            	   int buildID = blueprint(id,UnitType.Rocket,d);
            	   tasks.get(id).startBuilding(buildID);
               }
            }
            for(int id: factories)
            {
               if(tasks.get(id).getTask()==-1) {
            	  int i = rand.nextInt() % 100;
            	  if(i < 20) {
            		  tasks.get(id).startProducing(UnitType.Ranger);
            	  }
            	  else if(i < 60) {
            		  tasks.get(id).startProducing(UnitType.Mage);
            	  }
            	  else if(i < 75) {
            		  tasks.get(id).startProducing(UnitType.Healer);
            	  }
            	  else{
            		  tasks.get(id).startProducing(UnitType.Worker);
            	  }
               }
               tasks.get(id).doTask();
            }
            for(int id: rangers)
            {
               if(tasks.get(id).getTask()==-1)
               {
                  int n = (int)(Math.random()*enemyInit.size());
                  VecMapLocation v = gc.allLocationsWithin(gc.unit(id).location().mapLocation(), 50);
                  for(int enemyID: enemyInit.keySet())
                  {
                     if(n--==0)
                        tasks.get(id).startMoving(enemyInit.get(enemyID));
                  }
                  tasks.get(id).startAttacking();
               }
               tasks.get(id).doTask();
            }
         } catch(Exception e) {e.printStackTrace();} 
         gc.nextTurn();
      }
         //End of match
   }
   public static void mars() {
      // Do Something
      while(gc.round()<=maxRound)
      {
         System.out.println("Mars Round: "+gc.round());
         gc.nextTurn();
      }
   }
   //-2 means enemy spotted, but attack isn't ready, -1 means no enemy in sight, 0 means attacked enemy
   public static int attack(int id)
   {
      MapLocation curLoc = gc.unit(id).location().mapLocation();
      int range = (int) gc.unit(id).attackRange();
      for(int radius = 1; radius <= range; radius++)
      {
         HashSet<Integer> enemy = toIDList(gc.senseNearbyUnitsByTeam(curLoc, radius, enemyTeam));
         if(enemy.size()==0)
            continue;
         if(!gc.isAttackReady(id))
            return -2;
         for(int targetEnemy: enemy)
         {
            if(!gc.canAttack(id, targetEnemy))
               continue;
            gc.attack(id, targetEnemy);
            return 0;
         }
      }
      return -1;
   }
   //-2 means not enough resources, -1 means factory is busy, 0 means success
   public static int produce(int id, UnitType ut) //factory ID, type of unit to be made
   {
      if(gc.canProduceRobot(id, ut))
      {
         gc.produceRobot(id, ut);
         return 0;
      }
      if(gc.unit(id).isFactoryProducing()!=0)
         return -1;
      else
         return -2;
   }
   //worker id, structure id, -1 means cannot build, 0 means build success, 1 means structure is finished
   public static int build(int wID, int sID) 
   {
      if(!gc.canBuild(wID,sID))
         return -1;
      gc.build(wID, sID);
      if(gc.unit(sID).structureIsBuilt()!=0)
         return 1;
      else 
         return 0;
   }
   //returns the id of the blueprinted structure, -1 if no structure was created
   public static int blueprint(int id, UnitType ut, Direction d) throws Exception
   {
      if(gc.canBlueprint(id, ut, d))
      {
         gc.blueprint(id, ut, d);
         int blueID = gc.senseUnitAtLocation(gc.unit(id).location().mapLocation().add(d)).id();
         tasks.put(blueID, new Task(blueID, 2));
         return blueID;
      }
      return -1;
   }
   //-1 means no karbonite in adjacent squares, 0 means successfully mined
   public static int mine(int id)
   {
      Unit u = gc.unit(id);
      Pair p = mapPair(u.location().mapLocation());
      int xPos = p.x;
      int yPos = p.y;
      Direction best = null;
      int max = 0;
      for(Direction d: directions)
         try
         {
            MapLocation loc = eMapLoc[p.x][p.y].add(d);
            if(inBounds(mapPair(loc)))
               if(gc.karboniteAt(loc)>max)
               {
                  best = d;
                  max = (int)gc.karboniteAt(eMapLoc[p.x][p.y].add(d));
               }
         }catch(Exception e) {e.printStackTrace();}
      if(max==0)
         return -1;
      gc.harvest(u.id(), best);
      return 0;
   }
   //return -2 means path blocked, -1 means too much heat, 0 means move success, 1 means reached target
   public static int move(int id, boolean engage) 
   {
      Unit u = gc.unit(id);
      Pair prev = unitPair(id);
      Pair target = tasks.get(id).moveTarget;
      if(prev.equals(target))
      {
         tasks.get(id).moveTarget = null;
         return 1;
      }
      if(gc.canMove(id, paths[prev.x][prev.y][target.x][target.y].seq.getFirst()))
         if(gc.isMoveReady(id))
            if(engage)
            {
               gc.moveRobot(id, paths[prev.x][prev.y][target.x][target.y].seq.get(0));
               Pair p = unitPair(id);
               paths[p.x][p.y][target.x][target.y] = paths[prev.x][prev.y][target.x][target.y].shorten();
               return 0;
            }
            else
            {
               gc.moveRobot(id, paths[prev.x][prev.y][target.x][target.y].seq.get(0));
               Pair p = unitPair(id);
               paths[p.x][p.y][target.x][target.y] = paths[prev.x][prev.y][target.x][target.y].copy();
               paths[p.x][p.y][target.x][target.y].start = mapPair(u.location().mapLocation());
               paths[p.x][p.y][target.x][target.y].seq.remove();
               return 0;
            }
         else
            return -1;
      else
         return -2;
   }
   public static void updateUnits() throws Exception
   {
      myUnits = toIDList(gc.myUnits());
      healers = new HashSet<Integer>();
      factories = new HashSet<Integer>();
      knights = new HashSet<Integer>();
      mages = new HashSet<Integer>();
      rangers = new HashSet<Integer>();
      rockets = new HashSet<Integer>();
      workers = new HashSet<Integer>();
      for(int id: myUnits)
      {
         UnitType ut = gc.unit(id).unitType();
         if(ut.equals(UnitType.Factory))
            factories.add(id);
         else if(ut.equals(UnitType.Healer))
            healers.add(id);
         else if(ut.equals(UnitType.Knight))
            knights.add(id);
         else if(ut.equals(UnitType.Mage))
            mages.add(id);
         else if(ut.equals(UnitType.Ranger))
            rangers.add(id);
         else if(ut.equals(UnitType.Rocket))
            rockets.add(id);
         else if(ut.equals(UnitType.Worker))
            workers.add(id);
         else
            throw new Exception("Error, Unknown unit type: "+ut);
      }
   }
   public static int spaces(MapLocation m)
   {
      Pair id = mapPair(m);
      int count = 0;
      for(int x = Math.max(0, id.x-1); x <= Math.min(eWidth,id.x+1); x++)
         for(int y = Math.max(0, id.y-1); y <= Math.min(eHeight,id.y+1); y++)
            if(id.x!=x||id.y!=y)
               if(passable[x][y])
                  count++;
      return count;
   }
   public static Path findPath(Pair start, Pair end, ArrayList<Pair> avoid)
   {
      if(paths[start.x][start.y][end.x][end.y]!=null)
         return paths[start.x][start.y][end.x][end.y];
      int[][] dist = new int[eWidth][eHeight];
      Pair[][] prev = new Pair[eWidth][eHeight];
      for(int x = 0; x < dist.length; x++)
         Arrays.fill(dist[x], -1);
      dist[start.x][start.y] = 0;
      LinkedList<Pair> q = new LinkedList<Pair>();
      q.add(start);
      Pair id = null;
      boolean pathFound = false;
      while(!q.isEmpty())
      {
         id = q.remove();
         //System.out.println(id);
         if(id.equals(end))
         {
            pathFound = true;
            break;
         }
         for(Direction d: directions)
         {
            try
            {
               Pair next = mapPair(eMapLoc[id.x][id.y].add(d));
               //System.out.println(d+" "+next+" : "+inBounds(next)+" "+avoid(next, avoid)+" ("+next.x+", "+next.y+")");
               if(inBounds(next)&&avoid(next, avoid)&&dist[next.x][next.y]==-1)
               {
                  dist[next.x][next.y] = dist[id.x][id.y]+1;
                  prev[next.x][next.y] = id;
                  q.add(next);
               }
            }catch(Exception e) {e.printStackTrace();}
         }
      }   
      if(!pathFound)
         return null;
      Path p = new Path(start, end);
      while(prev[id.x][id.y]!=null)
      {
         p.seq.add(eMapLoc[prev[id.x][id.y].x][prev[id.x][id.y].y].directionTo(eMapLoc[id.x][id.y]));
         id = prev[id.x][id.y];
      }
      return p;
   }
   public static HashSet<Integer> toIDList(VecUnit vu)
   {
      HashSet<Integer> ret = new HashSet<Integer>();
      for(int x = 0; x < vu.size(); x++)
         ret.add(vu.get(x).id());
      return ret;
   }
   public static boolean isEmpty(Pair id)
   {
      return gc.hasUnitAtLocation(eMapLoc[id.x][id.y])&&passable[id.x][id.y];
   }
   public static boolean inBounds(Pair id)
   {
      return id.x>=0&&id.x<eWidth&&id.y>=0&&id.y<eHeight&&passable[id.x][id.y];
   }
   public static boolean avoid(Pair id, ArrayList<Pair> avoid)
   {
      if(avoid!=null)
         for(Pair a: avoid)
            if(a.equals(id))
               return false;
      return true;
   }
   public static Direction oppositeDirection(Direction d)
   {
      if(d.equals(Direction.North))
         return Direction.South;
      if(d.equals(Direction.Northeast))
         return Direction.Southwest;
      if(d.equals(Direction.East))
         return Direction.West;
      if(d.equals(Direction.Southeast))
         return Direction.Northwest;
      if(d.equals(Direction.South))
         return Direction.North;
      if(d.equals(Direction.Southwest))
         return Direction.Northeast;
      if(d.equals(Direction.West))
         return Direction.East;
      if(d.equals(Direction.Northwest))
         return Direction.Southeast;
      return null;
   }
   public static int countKarbAdj(int xPos, int yPos)
   {
      int count = 0;
      for(int x = Math.max(0, xPos-1); x < Math.min(eWidth-1, xPos+1); x++)
         for(int y = Math.max(0, yPos-1); y < Math.min(eHeight-1, yPos+1); y++)
            count+=karbDep[x][y];
      return count;
   }
   //floodfills region to count karbonite total
   public static int countKarbRegion(MapLocation m)
   {
      int karbCount = 0;
   
      return karbCount;
   }
   //floodfills region and returns a ArrayList containing locations with highest karbAdj value
   public static PriorityQueue<KarbAdjacent> countMaxKarbAdj(Pair p)
   {
      boolean[][] used = new boolean[eWidth][eHeight];
      PriorityQueue<KarbAdjacent> karbAdjOrder = new PriorityQueue<KarbAdjacent>();
      LinkedList<Pair> q = new LinkedList<Pair>();
      used[p.x][p.y] = true;
      q.add(p);
      while(!q.isEmpty())
      {
         Pair id = q.remove();
         karbAdjOrder.add(new KarbAdjacent(id, countKarbAdj(id.x, id.y)));
         for(Direction d: directions)
            try{
               Pair next = mapPair(eMapLoc[id.x][id.y].add(d));   
               if(inBounds(next)&&!used[next.x][next.y])
               {
                  used[next.x][next.y] = true;
                  q.add(next);
               }
            } catch(Exception e) {e.printStackTrace();}
      }
      return karbAdjOrder;
   }
   public static Pair mapPair(MapLocation m)
   {
      return new Pair(m.getX(),m.getY());
   }
   public static Pair unitPair(int id)
   {
      return mapPair(gc.unit(id).location().mapLocation());
   }
   public static UnitType unitType(int id)
   {
      return gc.unit(id).unitType();
   }
   public static void updateOcc() {
	   HashSet<Integer> oc = toIDList(gc.myUnits());
	   HashMap<Integer,Integer> temp = new HashMap<Integer,Integer>();
	   for(Integer x:oc) {
		   int taskType = tasks.get(x).taskType;
		   if(temp.containsKey(taskType)) 
			   temp.put(taskType, temp.get(taskType) + 1);
		   else
			   temp.put(taskType, 1);
	   }
   }
   static class Path
   {
      Pair start;
      Pair end;
      LinkedList<Direction> seq = new LinkedList<Direction>();
      public Path(Pair s, Pair e)
      {
         start = s;
         end = e;
      }
      public Path copy()
      {
         Path p = new Path(start, end);
         for(Direction d: seq)
            p.seq.add(d);
         return p;
      }
      public Path shorten()
      {
         if(seq.size()<=1)
            return null;
         Path p = new Path(mapPair(eMapLoc[start.x][start.y].add(seq.getFirst())), end);
         boolean first = false;
         for(Direction d: seq)
            p.seq.add(d);
         return p;
      }
      public Path reverse()
      {
         Path p = new Path(end, start);
         for(Direction d: seq)
            p.seq.addLast(oppositeDirection(d));
         return p;
      }
      public String toString()
      {
         String s = "[";
         for(Direction d: seq)
            s+=d+", ";
         s = s.substring(0, s.length()-2)+"]";
         return s;
      }
   }
   static class KarbAdjacent implements Comparable<KarbAdjacent> {
      Pair loc;
      int dep;
   
      public KarbAdjacent(Pair p, int d) {
         loc = p;
         dep = d;
      }
   // greatest goes first
      public int compareTo(KarbAdjacent x) {
         return x.dep-dep;
      }
   }
   static class Task
   {
   /* 
      0: mining
      1: building
      2: producing
      3: attack
      4: heal
   */
      int taskType;
      int[] typeOn = new int[10]; //lower value is higher priority, 0 means no priority
      int unitID;
      int maxStep = 1;
   //task-specific data (if taks is off, then value is null, false, -1, etc)
      int buildID; //for building: target blueprint
      Pair moveTarget; //for moving: final destination
      Pair attack; //for moving: final destination
      UnitType produceType; //for producing: type of 
      public Task(int id, int tType)
      {
         unitID = id;
         taskType = tType;
      }
      public boolean startMoving(Pair target)
      {
         Pair curLoc = unitPair(unitID);
         paths[curLoc.x][curLoc.y][target.x][target.y] = findPath(curLoc, target, null);
         if(paths[curLoc.x][curLoc.y][target.x][target.y]==null)
            return false;
         moveTarget = target;
         return true;
      }
      public boolean stopMoving()
      {
         moveTarget = null;
         return true;
      }
      public boolean startMining()
      {
         typeOn[0] = maxStep++;
         return true;
      }
      public boolean stopMining()
      {
         typeOn[0] = 0;
         return true;
      }
      public boolean startBuilding(int targetID)
      {
         typeOn[1] = maxStep++;
         buildID = targetID;
         return true;
      }
      public boolean stopBuilding()
      {
         typeOn[1] = 0;
         buildID = 0;
         return true;
      }
      public boolean startProducing(UnitType type)
      {
         typeOn[2] = maxStep++;
         produceType = type;
         return true;
      }
      public boolean stopProducing()
      {
         typeOn[2] = maxStep++;
         produceType = null;
         return true;
      }
      public boolean startAttacking()
      {
         typeOn[3] = maxStep++;
         return true;
      }
      public boolean stopAttacking()
      {
         typeOn[3] = 0;
         return true;
      }
      //return task number
      public int getTask()
      {
         int index = -1;
         for(int x = 0; x < typeOn.length; x++)
            if(typeOn[x]>0)
               if(index==-1||typeOn[x]<typeOn[index])
                  index = x;
         return index;
      }
      // if unit is a factory, return 0 if unloaded a unit, otherwise return -1
      // return -1*(tasktype+1) if finished with the task
      public int doTask()
      {
    	 updateOcc();
         int ret = 0;
         switch(getTask()) {
            case 0: // Worker Mining
               if(mine(unitID)==-1)
                  ret = -1;
               break;
            case 1: // Factory Building
               if(build(unitID, buildID)==1) {
                  ret = -2;
                  this.stopBuilding();
               }
               break;
            case 2: // Factory Shit
               if(produce(unitID, produceType)==1)
               {
                  //System.out.println("Successful production");
               }
               else
               {
                  //System.out.println("Failed production");
               }
               boolean working = true;
               int unloadCount = 0;
               cycle: while(working)
               {
                  if(gc.unit(unitID).structureGarrison().size()>0)
                  {
                     //System.out.println("Garrison is empty");
                  }
                  else
                  {
                     //System.out.println("Garrison is not empty");
                  }
                  working = false;
                  for(Direction d: directions)
                     if(gc.canUnload(unitID, d))
                     {
                        gc.unload(unitID, d);
                        int produceID = gc.senseUnitAtLocation(gc.unit(unitID).location().mapLocation().add(d)).id();
                        if(gc.unit(produceID).unitType().equals(UnitType.Worker)) {
                        	// Workers - Mine (0) Make Factories (1) Make Rockets (5)
                        	if(factories.size() + typePerTask.get(1) <= 5) {
                        		tasks.put(produceID, new Task(produceID,1));
                        	}
                        	else if(gc.round() < 150 || typePerTask.get(0) <= 4) {
                        		tasks.put(produceID, new Task(produceID, 0));
                        	}
                        	else {
                        		tasks.put(produceID, new Task(produceID,5));
                        	}
                        }
                        else if(gc.unit(produceID).unitType().equals(UnitType.Healer)) {
                        	// Healers - Only Task is to Heal EarthBound (4) MarsBound (6)
                        	if(typePerTask.get(4) < 5)
                        		tasks.put(produceID, new Task(produceID,4));   
                        	else
                        		tasks.put(produceID, new Task(produceID,6));
                        }
                        else if(gc.unit(produceID).unitType().equals(UnitType.Mage)) {
                        	if(typePerTask.get(7) < 3)
                        		tasks.put(produceID, new Task(produceID,7));
                        	else
                        		tasks.put(produceID, new Task(produceID,8));
                        	// Mages - EarthBound (7) MarsBound (8)
                        }
                        else { // Rangers - Only Task is to Defend EarthBound (3) MarsBound(9)
                        	if(typePerTask.get(3) < 15)
                        		tasks.put(produceID, new Task(produceID,3));
                        	else
                        		tasks.put(produceID, new Task(produceID,9));
                           tasks.put(produceID, new Task(produceID, 3));
                        }
                        working = true;
                        unloadCount++;
                        System.out.println("Unloading unit: "+produceID);
                        continue cycle;
                     }
                  //System.out.println("Finished unload attempt");
               }
               if(unloadCount>0)
                  ret = 0;
               else
                  ret = -1;
               break;
            case 3: // Ranger Attack
               attack(unitID);
               break;
            case 4: // Healer EarthBound
            	doHeal();
            case 5: // Build Rockets
            	if(build(unitID, buildID)==1) {
                    ret = -6;
                    this.stopBuilding();
                 }
                 break;
            case 6: // Healer MarsBound
            	for(int rocketunit:rocketLoad) {
            		
            	}
            
            case 7: // Mages EarthBound
            	
            case 8: // Mages MarsBound
            	
            case 9: // Rangers MarsBound
            	
            default:
               if(unitType(unitID).equals(UnitType.Worker))
                  mine(unitID);
         }
         if(moveTarget!=null)
         {
            int i = move(unitID, false);
            if(i==1)
               moveTarget=null;
         }
         return 0;
      }
      
      void doHeal() {
      	if(gc.senseNearbyUnits(gc.unit(unitID).location().mapLocation(),4).size() > 3) {
    		int minUn = 1000;
    		Direction dmin = null;
    		for(Direction d: directions) {
    			int l = (int) gc.senseNearbyUnits(gc.unit(unitID).location().mapLocation().add(d),4).size();
    			if(gc.canMove(unitID, d) && l < minUn) {
    				minUn = l;
    				dmin = d;
    			}
    		}
    		if(dmin == null);
    		else gc.moveRobot(unitID, dmin);
    	}            	
    	VecUnit healUnits = gc.senseNearbyUnits(gc.unit(unitID).location().mapLocation(), 30);  
    	for(int i = 0; i < (int) healUnits.size(); i++) {
    		Unit tempUn = healUnits.get(i);
    		if(tempUn.team().equals(gc.team())) {
        		UnitType type = tempUn.unitType();
        		if(type != UnitType.Rocket && type != UnitType.Factory) {
        			if(tempUn.health() != tempUn.maxHealth() && gc.canHeal(unitID, tempUn.id())) {
        				gc.heal(unitID, tempUn.id());
        				break;
        			}
        		}
    		}
    	}
      }
   }
   static class Connection
   {
      HashSet<Integer> myInit = new HashSet<Integer>();
      HashSet<Integer> enemyInit = new HashSet<Integer>();
      boolean[][] contains = new boolean[eWidth][eHeight];
      public Connection()
      {
      }
   }
   static class Pair
   {
	   int x;
	   int y;
	   public Pair(int a, int b)
	   {
	      x = a;
	      y = b;
	   }
	   public boolean equals(Pair p)
	   {
	      if(p==null)
	         return false;
	      return x==p.x&&y==p.y;
	   }
	   public String toString()
	   {
	      return x+" "+y;
	   }
	   static class CompareX implements Comparator<Pair>
	   {
	      public int compare(Pair p1, Pair p2)
	      {
	         if(p1.x!=p2.x)
	            return p1.x-p2.x;
	         return p1.y-p2.y;
	      }
	   }
	   static class CompareY implements Comparator<Pair>
	   {
	      public int compare(Pair p1, Pair p2)
	      {
	         if(p1.y!=p2.y)
	            return p1.y-p2.y;
	         return p1.x-p2.x;
	      }
	   }
	}
}