import bc.*;
import java.util.*;
@SuppressWarnings("unchecked")
public class Rusher {
   //Basic Info 
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
   
   //Unit Info
   static HashSet<Integer> myUnits = toIDList(gc.myUnits());
   static HashSet<Integer> enemyInit = toIDList(gc.units());
   static HashSet<Integer> myInit = new HashSet<Integer>();
   static HashSet<Integer>[] ubt = sortUnitTypes(gc.myUnits());
   static HashSet<Integer> healers = ubt[0];
   static HashSet<Integer> factories = ubt[1];
   static HashSet<Integer> knights = ubt[2];
   static HashSet<Integer> mages = ubt[3];
   static HashSet<Integer> rangers = ubt[4];
   static HashSet<Integer> rockets = ubt[5];
   static HashSet<Integer> workers = ubt[6];
   static HashMap<Integer, Task> tasks = new HashMap<Integer, Task>();
   static boolean[][] beingMined = new boolean[eWidth][eHeight];

   static {
      ArrayList<Integer> toRemove = new ArrayList<Integer>();
      for(int x: enemyInit)
         if(myUnits.contains(x))
         {
            toRemove.add(x);
            myInit.add(x);
         }
      for(int x: toRemove)
         enemyInit.remove(x);
   }
   // Map Info
   static MapLocation[][] eMapLoc = new MapLocation[eWidth][eHeight];
   static long[][] karbDep = new long[eWidth][eHeight];//amount of karbonite in the square
   static long[][] karbAdj = new long[eWidth][eHeight];//sum of karbonite on and adjacent to the square
   static boolean[][] passable = new boolean[eWidth][eHeight];
   static Path[][][][] paths = new Path[eWidth][eHeight][eWidth][eHeight];
   
   static {
      for(int i = 0; i < eWidth; i++) {
         for(int j = 0; j < eHeight; j++) {
            eMapLoc[i][j] = new MapLocation(earth,i,j);
            karbDep[i][j] = eMap.initialKarboniteAt(eMapLoc[i][j]);
            passable[i][j] = eMap.isPassableTerrainAt(eMapLoc[i][j])==0;
         }
      }
      for(int x = 0; x < eWidth; x++)
         for(int y = 0; y < eHeight; y++)
            karbAdj[x][y] = countKarbAdj(x,y);
     
        /* Start Strategy 2 - Rush
         * Harass enemy early on by targetting workers to give them a slow start
         * Snipe == Win Condition, make as many Rangers as possible, use knights as meat shields with healers supporting them
         * Dominate on Earth to weaken enemy's late game in Mars
                     * */
      gc.queueResearch(UnitType.Ranger);  // 25 Rounds - "Get in Fast"
      gc.queueResearch(UnitType.Worker);  // 25 Rounds - "Gimme some of that Black Stuff"
      gc.queueResearch(UnitType.Ranger);  // 100 Rounds - "Scopes"
      gc.queueResearch(UnitType.Ranger);  // 200 Rounds - "Snipe"
      gc.queueResearch(UnitType.Rocket);  // 100 Rounds - "Rocketry"
      gc.queueResearch(UnitType.Knight);  // 25 Rounds - "Armor"
      gc.queueResearch(UnitType.Knight);  // 75 Rounds - "Even More Armor"
      gc.queueResearch(UnitType.Healer);  // 25 Rounds - "Spirit Water"
      gc.queueResearch(UnitType.Healer);  // 100 Rounds - "Spirit Water II"
        
        /*End Strategy 2*/
   }
   public static void main(String[] args) {
      if(gc.planet().equals(Planet.Earth))
         earth();
      else
         mars();
   }
   public static void earth() {
      try {
         long startTime = System.currentTimeMillis();
         System.out.println("start time = "+startTime);
         System.out.println("Running Earth Player: ");
         int curRound = 1;
         int primary = -1;
         boolean enemyConnected = false;
      //first round
         try {
            System.out.println("Earth Round "+curRound+": ");
            System.out.println("Time used: "+(System.currentTimeMillis()-startTime));
            for(int x: myInit)
               tasks.put(x, new Task(x));
            init: for(int start: myUnits)
            {
               primary = start;
               for(int target: enemyInit)
               {
                  Pair p1 = mapPair(gc.unit(start).location().mapLocation());
                  Pair p2 = mapPair(gc.unit(target).location().mapLocation());
                  paths[p1.x][p1.y][p2.x][p2.y] = findPath(p1, p2, new ArrayList<MapLocation>());
                  if(paths[p1.x][p1.y][p2.x][p2.y]!=null)
                  {   
                     Task t = new Task(primary);
                     t.moveTarget = p2;
                     tasks.get(primary).typeOn[0] = true;
                     tasks.get(primary).moveTarget = p2;
                     move(primary, true);
                     tasks.get(primary).typeOn[0] = false;
                     tasks.get(primary).moveTarget = null;
                     t.buildID = blueprint(primary, UnitType.Factory, oppositeDirection(paths[p1.x][p1.y][p2.x][p2.y].seq.get(0)));
                     enemyConnected = true;
                     break init;
                  }
               }
            }
            HashMap<Integer, PriorityQueue<KarbAdjacent>> bestKarbAdj = new HashMap<Integer, PriorityQueue<KarbAdjacent>>();;
         //finds the highest karbAdj locations for each starting unit
            for(int id: myUnits)
            {
               bestKarbAdj.put(id, countMaxKarbAdj(gc.unit(id).location().mapLocation()));
               if(id!=primary)
               {
                  tasks.get(id).typeOn[0] = true;
                  tasks.get(id).moveTarget = bestKarbAdj.get(id).remove().loc;
                  move(id, false);
               }
            }
            curRound++;
            gc.nextTurn();
         } catch(Exception e) {e.printStackTrace();}
      //end of first round
         for(; curRound <= 10; curRound++)
         {
            System.out.println("Earth Round "+curRound+": ");
            System.out.println("Time used: "+(System.currentTimeMillis()-startTime));
            updateUnits();
            for(int id: myUnits)
               if(id==primary)
               {
                  build(primary, tasks.get(primary).buildID);
               }
               else
               {
                  int arrived = move(id, false);
                  if(arrived==1)
                  {
                     mine(id);
                  }
               }
            gc.nextTurn();
         }
         for(; curRound <= maxRound; curRound++)
         {
            //try-catch is used for everything since if we have an uncaught exception, we lose
            try
            {
               System.out.println("Earth Round "+curRound+": ");
               System.out.println("Time used: "+(System.currentTimeMillis()-startTime));
               updateUnits();
               gc.nextTurn();
            }
            catch(Exception e) {
               e.printStackTrace();
            }
            //End turn
            gc.nextTurn(); 
         }
      }
      catch(Exception e) {
         e.printStackTrace();
      }
   }
   public static void mars() {
      // Do Something
      System.out.println("Mars Stuffs");
      
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
   public static int blueprint(int id, UnitType ut, Direction d)
   {
      Unit u = gc.unit(id);
      HashSet<Integer> prevFact = factories;
      if(gc.canBlueprint(id, ut, d))
         gc.blueprint(id, ut, d);
      else
         return -1;
      int blueID = -2;
      for(int x: factories)
         if(!prevFact.contains(x))
            blueID = x;
      if(blueID==-2)
         System.out.println("Blueprint method error");
      return blueID;
   }
   public static boolean mine(int id)
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
            if(gc.karboniteAt(eMapLoc[p.x][p.y].add(d))>max)
            {
               best = d;
               max = (int)gc.karboniteAt(eMapLoc[p.x][p.y].add(d));
            }
         }catch(Exception e) {e.printStackTrace();}
      if(max>0)
      {
         gc.harvest(u.id(), best);
         return true;
      }
      return false;
   }
   //return -1 means path blocked, 0 means move success, 1 means reached target
   public static int move(int id, boolean engage) 
   {
      Unit u = gc.unit(id);
      Pair prev = mapPair(u.location().mapLocation());
      Pair target = tasks.get(id).moveTarget;
      if(prev.equals(target))
      {
         tasks.get(id).moveTarget = null;
         return 1;
      }
      if(gc.canMove(id, paths[prev.x][prev.y][target.x][target.y].seq.get(0)))
         if(engage)
         {
         }
         else
         {
            gc.moveRobot(id, paths[prev.x][prev.y][target.x][target.y].seq.get(0));
            Pair p = mapPair(u.location().mapLocation());
            paths[p.x][p.y][target.x][target.y] = paths[prev.x][prev.y][target.x][target.y].copy();
            paths[p.x][p.y][target.x][target.y].start = mapPair(u.location().mapLocation());
            paths[p.x][p.y][target.x][target.y].seq.remove(0);
            return 0;
         }
      return 0;
   }
   public static void updateUnits()
   {
      myUnits = toIDList(gc.myUnits());
      ubt = sortUnitTypes(gc.myUnits());
      healers = ubt[0];
      factories = ubt[1];
      knights = ubt[2];
      mages = ubt[3];
      rangers = ubt[4];
      rockets = ubt[5];
      workers = ubt[6];
   }
   public static HashSet<Integer>[] sortUnitTypes(VecUnit units) {
      HashSet<Integer>[] unitsByType = new HashSet[7];
      for(int x = 0; x < 7; x++)
         unitsByType[x] = new HashSet<Integer>();
      for(int x = 0; x < units.size(); x++)
      {
         UnitType ut = units.get(x).unitType();
         if(ut.equals(UnitType.Factory))
            unitsByType[0].add(units.get(x).id());
         else if(ut.equals(UnitType.Healer))
            unitsByType[1].add(units.get(x).id());
         else if(ut.equals(UnitType.Knight))
            unitsByType[2].add(units.get(x).id());
         else if(ut.equals(UnitType.Mage))
            unitsByType[3].add(units.get(x).id());
         else if(ut.equals(UnitType.Ranger))
            unitsByType[4].add(units.get(x).id());
         else if(ut.equals(UnitType.Rocket))
            unitsByType[5].add(units.get(x).id());
         else if(ut.equals(UnitType.Worker))
            unitsByType[6].add(units.get(x).id());
         else
            System.out.println("Error, Unknown unit type: "+ut);
      }
      return unitsByType;
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
   public static Path findPath(Pair start, Pair end, ArrayList<MapLocation> avoid)
   {
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
         if(id.equals(end))
         {
            pathFound = true;
            break;
         }
         for(Direction d: directions)
            if(!d.equals(Direction.Center))
               try
               {
                  Pair next = mapPair(eMapLoc[id.x][id.y].add(d));
                  if(inBounds(next)&&avoid(next, avoid)&&dist[next.x][next.y]==-1)
                  {
                     dist[next.x][next.y] = dist[id.x][id.y]+1;
                     prev[next.x][next.y] = id;
                     q.add(next);
                  }
               }catch(Exception e) {e.printStackTrace();}
      }   
      if(!pathFound)
         return null;
      Path p = new Path(start, end);
      while(prev[id.x][id.y]!=null)
      {
         p.seq.add(eMapLoc[id.x][id.y].directionTo(eMapLoc[prev[id.x][id.y].x][prev[id.x][id.y].y]));
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
      return id.x<0||id.x>=eWidth||id.y<0||id.y>=eHeight;
   }
   public static boolean avoid(Pair id, ArrayList<MapLocation> avoid)
   {
      if(avoid!=null)
         for(int a = 0; a < avoid.size(); a++)
            if(mapPair(avoid.get(a)).equals(id))
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
   public static PriorityQueue<KarbAdjacent> countMaxKarbAdj(MapLocation m)
   {
      boolean[][] used = new boolean[eWidth][eHeight];
      PriorityQueue<KarbAdjacent> karbAdjOrder = new PriorityQueue<KarbAdjacent>();
      LinkedList<Pair> q = new LinkedList<Pair>();
      used[mapPair(m).x][mapPair(m).y] = true;
      q.add(mapPair(m));
      while(!q.isEmpty())
      {
         Pair id = q.remove();
         karbAdjOrder.add(new KarbAdjacent(id, countKarbAdj(id.x, id.y)));
         for(Direction d: directions)
            try{
               Pair next = mapPair(eMapLoc[id.x][id.y].add(d));   
               if(inBounds(next))
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
   static class Path
   {
      Pair start;
      Pair end;
      ArrayList<Direction> seq;
      public Path(Pair s, Pair e)
      {
         start = s;
         end = e;
         seq = new ArrayList<Direction>();
      }
      public Path copy()
      {
         Path p = new Path(start, end);
         for(int x = 0; x < seq.size(); x++)
            p.seq.add(seq.get(x));
         return p;
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
      int unitID;
   /* 
      0: moving
      1: mining
      2: building
   */
      boolean[] typeOn = new boolean[10]; //if the robot is doing task "x"
      int buildID; //for building: target blueprint
      Pair moveTarget; //for moving: final destination
      public Task(int id)
      {
         unitID = id;
      }
   }
}
class Pair
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
      return x==p.x&&y==p.y;
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