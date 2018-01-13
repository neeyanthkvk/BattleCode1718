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
   static HashSet<Integer> enemyInit = toIDList(gc.units());
   static HashSet<Integer> myInit = new HashSet<Integer>();
   static HashMap<Integer, Integer> initRegion = new HashMap<Integer, Integer>();
   static HashSet<Integer> myUnits = toIDList(gc.myUnits());
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
   
   // Map Info
   static MapLocation[][] eMapLoc = new MapLocation[eWidth][eHeight];
   static long[][] karbDep = new long[eWidth][eHeight];//amount of karbonite in the square
   static long[][] karbAdj = new long[eWidth][eHeight];//sum of karbonite on and adjacent to the square
   static boolean[][] passable = new boolean[eWidth][eHeight];
   static Path[][][][] paths = new Path[eWidth][eHeight][eWidth][eHeight];
   static HashMap<Integer, PriorityQueue<KarbAdjacent>> bestKarbAdj = new HashMap<Integer, PriorityQueue<KarbAdjacent>>();



//
   static int primary = -1;
   static boolean enemyConnected = false;
 //
      
      
   static {
      try {
      //initializes enemyInit and myInit
         ArrayList<Integer> toRemove = new ArrayList<Integer>();
         for(int x: enemyInit)
            try {
               if(myUnits.contains(x))
               {
                  toRemove.add(x);
                  myInit.add(x);
               }
            } catch(Exception e) {e.printStackTrace();}
         for(int x: toRemove)
            try {
               enemyInit.remove(x);
            } catch(Exception e) {e.printStackTrace();}
      //initializes tasks
         System.out.println("Start tasks");
         for(int x: myInit)
            try {
               tasks.put(x, new Task(x));
            } catch(Exception e) {e.printStackTrace();}
         System.out.println("end tasks");
      //initializes eMapLoc, karbDep, and passable
         for(int x = 0; x < eWidth; x++)
            for(int y = 0; y < eHeight; y++) 
               try {
                  eMapLoc[x][y] = new MapLocation(earth,x,y);
                  karbDep[x][y] = eMap.initialKarboniteAt(eMapLoc[x][y]);
                  passable[x][y] = eMap.isPassableTerrainAt(eMapLoc[x][y])!=0;
               } catch(Exception e) {e.printStackTrace();}
      //initializes karbAdj
         for(int x = 0; x < eWidth; x++)
            for(int y = 0; y < eHeight; y++)
               try {
                  karbAdj[x][y] = countKarbAdj(x,y);
               } catch(Exception e) {e.printStackTrace();}
      //initializes primary
         init: for(int start: myUnits)
         {
            primary = start;
            for(int target: enemyInit)
            {
               Pair p1 = unitPair(start);
               Pair p2 = unitPair(target);
               paths[p1.x][p1.y][p2.x][p2.y] = findPath(p1, p2, null);
               if(paths[p1.x][p1.y][p2.x][p2.y]!=null)
               {   
                  enemyConnected = true;
                  break init;
               }
            }
         }
      //initializes bestKarbAdj and assigns first tasks
         for(int id: myUnits)
            bestKarbAdj.put(id, countMaxKarbAdj(unitPair(id)));
      //Research
      
         gc.queueResearch(UnitType.Ranger);  // 25 Rounds - "Get in Fast"
         gc.queueResearch(UnitType.Worker);  // 25 Rounds - "Gimme some of that Black Stuff"
         gc.queueResearch(UnitType.Ranger);  // 100 Rounds - "Scopes"
         gc.queueResearch(UnitType.Ranger);  // 200 Rounds - "Snipe"
         gc.queueResearch(UnitType.Rocket);  // 100 Rounds - "Rocketry"
         gc.queueResearch(UnitType.Knight);  // 25 Rounds - "Armor"
         gc.queueResearch(UnitType.Knight);  // 75 Rounds - "Even More Armor"
         gc.queueResearch(UnitType.Healer);  // 25 Rounds - "Spirit Water"
         gc.queueResearch(UnitType.Healer);  // 100 Rounds - "Spirit Water II"
      } catch(Exception e) {e.printStackTrace();}
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
         int curRound = 1;
      //1st round
         try {
            System.out.println("Earth Round "+gc.round()+": ");
            System.out.println("Time used: "+(System.currentTimeMillis()-startTime));
            for(int id: myUnits)
               if(id==primary)
               {
                  Pair p1 = unitPair(primary);
                  Pair p2 = tasks.get(id).moveTarget;
                  tasks.get(primary).startMoving(p2);
                  move(id, false);
                  tasks.get(id).stopMoving();
                  int buildID = blueprint(primary, UnitType.Factory, oppositeDirection(paths[p1.x][p1.y][p2.x][p2.y].seq.get(0)));
                  tasks.get(id).startBuilding(buildID);
               }
               else
               {
                  tasks.get(id).startMoving(bestKarbAdj.get(id).remove().loc);
                  move(id, false);
               }
            curRound++;
            gc.nextTurn();
         } catch(Exception e) {e.printStackTrace();}
      //rounds 2-10
         while(gc.round()<=10)
            try {
               System.out.println("Earth Round "+gc.round()+": ");
               System.out.println("Time used: "+(System.currentTimeMillis()-startTime));
               updateUnits();
               for(int id: myUnits)
                  if(id==primary)
                  {
                     build(primary, tasks.get(primary).buildID);
                  }
                  else
                  {
                     if(tasks.get(id).getTask()==0&&move(id, false)==1)
                     {
                        tasks.get(id).stopMoving();
                        tasks.get(id).startMining();
                     }
                     tasks.get(id).doTask();
                  }
               gc.nextTurn();
            } catch(Exception e) {e.printStackTrace();}
      //rounds 11-1000
         while(gc.round()<=maxRound)
         {
            //try-catch is used for everything since if we have an uncaught exception, we lose
            try
            {
               System.out.println("Earth Round "+gc.round()+": ");
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
      while(gc.round()<=maxRound)
      {
         System.out.println("Mars Round: "+gc.round());
         gc.nextTurn();
      }
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
      Pair prev = unitPair(id);
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
            Pair p = unitPair(id);
            paths[p.x][p.y][target.x][target.y] = paths[prev.x][prev.y][target.x][target.y].copy();
            paths[p.x][p.y][target.x][target.y].start = mapPair(u.location().mapLocation());
            paths[p.x][p.y][target.x][target.y].seq.remove();
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
   public static Pair unitPair(int id)
   {
      return mapPair(gc.unit(id).location().mapLocation());
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
      0: moving
      1: mining
      2: building
   */
      int[] typeOn = new int[10]; //lower value is higher priority, 0 means no priority
      int unitID;
      int maxStep = 1;
   //task-specific data (if taks is off, then value is null, false, -1, etc)
      int buildID; //for building: target blueprint
      Pair moveTarget; //for moving: final destination
      public Task(int id)
      {
         unitID = id;
      }
      public boolean startMoving(Pair target)
      {
         Pair curLoc = unitPair(unitID);
         paths[curLoc.x][curLoc.y][target.x][target.y] = findPath(curLoc, target, null);
         if(paths[curLoc.x][curLoc.y][target.x][target.y]==null)
            return false;
         typeOn[0] = maxStep++;
         moveTarget = target;
         return true;
      }
      public boolean stopMoving()
      {
         typeOn[0] = 0;
         moveTarget = null;
         return true;
      }
      public boolean startMining()
      {
         typeOn[1] = maxStep++;
         return true;
      }
      public boolean stopMining()
      {
         typeOn[1] = 0;
         return true;
      }
      public boolean startBuilding(int targetID)
      {
         typeOn[2] = maxStep++;
         buildID = targetID;
         return true;
      }
      public boolean stopBuilding(int targetID)
      {
         typeOn[2] = 0;
         buildID = 0;
         return true;
      }
      public int getTask()
      {
         int index = -1;
         for(int x = 1; x < typeOn.length; x++)
            if(typeOn[x]>0)
               if(index==-1||typeOn[x]<typeOn[index])
                  index = x;
         return index;
      }
      public int doTask()
      {
         switch(getTask()) {
            case 0:
               move(unitID, false);
               break;
            case 1:
               mine(unitID);
               break;
            case 2:
               build(unitID, buildID);
               break;
            default:
               break;
         }
         return 0;
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
      if(p==null)
         return false;
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