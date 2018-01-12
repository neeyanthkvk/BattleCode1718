import bc.*;
import java.util.*;
@SuppressWarnings("unchecked")
public class Rusher {
   //CONSTANTS
   static int maxRound = 1000;   // the number of rounds
   static Planet earth = Planet.Earth;
   static Planet mars = Planet.Mars;
   static Direction[] directions = Direction.values();
   
   //Other Stuff
   static GameController gc = new GameController();;
   static PlanetMap eMap = gc.startingMap(earth);
   static PlanetMap mMap = gc.startingMap(mars);
   static int eWidth = (int) eMap.getWidth();
   static int eHeight = (int) eMap.getHeight();
   static Team team;
   static HashMap<Integer, Unit> idUnits = new HashMap<Integer, Unit>();
   static HashMap<Integer, MapLocation> unitTarget = new HashMap<Integer, MapLocation>();
   static boolean[][] beingMined = new boolean[eWidth][eHeight];

   // Initial Earth Stuff
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
      //first round
         System.out.println("Earth Round "+curRound+": ");
         System.out.println("Time used: "+(System.currentTimeMillis()-startTime));
         VecUnit initUnits = eMap.getInitial_units();
         VecUnit myUnits = gc.myUnits();
         ArrayList<Integer> initID = new ArrayList<Integer>();
         for(int x = 0; x < idUnits.size(); x++)
            initID.add(idUnits.get(x).id());
         Unit primary = null;
         team = myUnits.get(0).team();
         boolean enemyConnected = false;
         init: for(int n = 0; n < myUnits.size(); n++)
         {
            primary = myUnits.get(n);
            for(int targetI = 0; targetI < initUnits.size(); targetI++)
               if(!initUnits.get(targetI).team().equals(team))
               {
                  MapLocation m1 = primary.location().mapLocation();
                  MapLocation m2 = initUnits.get(targetI).location().mapLocation();
                  Pair p1 = mapID(m1);
                  Pair p2 = mapID(m2);
                  paths[p1.x][p1.y][p2.x][p2.y] = findPath(m1, m2, new ArrayList<MapLocation>());
                  if(paths[p1.x][p1.y][p2.x][p2.y]!=null)
                  {   
                     unitTarget.put(primary.id(), m2);
                     move(primary, true);
                     unitTarget.remove(primary.id());
                     gc.blueprint(primary.id(), UnitType.Factory, oppositeDirection(paths[p1.x][p1.y][p2.x][p2.y].seq.get(0)));
                     enemyConnected = true;
                     break init;
                  }
               }
         }
         ArrayList<KarbAdjacent>[] bestKarbAdj = new ArrayList[(int)myUnits.size()];
         //finds the highest karbAdj locations for each starting unit
         for(int x = 0; x < myUnits.size(); x++)
         {
            bestKarbAdj[x] = countMaxKarbAdj(myUnits.get(x).location().mapLocation());
            if(!myUnits.get(x).equals(primary))
            {
               unitTarget.put(myUnits.get(x).id(), bestKarbAdj[x].get(0).ml);
               bestKarbAdj[x].remove(0);
               move(myUnits.get(x), true);
            }
         }
         curRound++;
         gc.nextTurn();
      //end of first round
         for(; curRound <= 10; curRound++)
         {
            System.out.println("Earth Round "+curRound+": ");
            System.out.println("Time used: "+(System.currentTimeMillis()-startTime));
            ArrayList<Unit>[] ubt = sortUnitTypes(myUnits);
            ArrayList<Unit> healers = ubt[0];
            ArrayList<Unit> factories = ubt[1];
            ArrayList<Unit> knights = ubt[2];
            ArrayList<Unit> mages = ubt[3];
            ArrayList<Unit> rangers = ubt[4];
            ArrayList<Unit> rockets = ubt[5];
            ArrayList<Unit> workers = ubt[6];
            for(int x = 0; x < myUnits.size(); x++)
               if(myUnits.get(x).equals(primary))
                  gc.build(primary.id(), factories.get(0).id());
               else
               {
                  boolean arrived = move(myUnits.get(x), true);
                  if(arrived)
                  {
                     mine(myUnits.get(x));
                  }
               }
            gc.nextTurn();
         }
         if(enemyConnected)
            for(; curRound <= maxRound; curRound++)
            {
            //try-catch is used for everything since if we have an uncaught exception, we lose
               try
               {
                  System.out.println("Earth Round "+curRound+": ");
                  System.out.println("Time used: "+(System.currentTimeMillis()-startTime));
                  myUnits = gc.myUnits();
                  ArrayList<Unit>[] ubt = sortUnitTypes(myUnits); //units by type
                  ArrayList<Unit> healers = ubt[0];
                  ArrayList<Unit> factories = ubt[1];
                  ArrayList<Unit> knights = ubt[2];
                  ArrayList<Unit> mages = ubt[3];
                  ArrayList<Unit> rangers = ubt[4];
                  ArrayList<Unit> rockets = ubt[5];
                  ArrayList<Unit> workers = ubt[6];
                  for(int x = 0; x < myUnits.size(); x++)
                     if(myUnits.get(x).equals(primary))
                        gc.build(primary.id(), factories.get(0).id());
                     else
                     {
                        boolean arrived = move(myUnits.get(x), true);
                        if(arrived)
                           mine(myUnits.get(x));
                     }
                  gc.nextTurn();
               }
               catch(Exception e) {
                  e.printStackTrace();
               }
            //End turn
               gc.nextTurn(); 
            }    
         else
         {
            for(int x = 0; x < initID.size(); x++)
            {
            
            }
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
   public static boolean mine(Unit u)
   {
      Pair p = mapID(u.location().mapLocation());
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
   public static boolean move(Unit u, boolean ignoreEnemy)
   {
      int id = u.id();
      if(unitTarget.get(id)==null)
         return false;
      Pair prev = mapID(u.location().mapLocation());
      Pair target = mapID(unitTarget.get(id));
      if(prev.equals(target))
      {
         unitTarget.remove(id);
         return true;
      }
      if(ignoreEnemy)
      {
         gc.moveRobot(id, paths[prev.x][prev.y][target.x][target.y].seq.get(0));
         Pair p = mapID(u.location().mapLocation());
         paths[p.x][p.y][target.x][target.y] = paths[prev.x][prev.y][target.x][target.y].copy();
         paths[p.x][p.y][target.x][target.y].start = u.location().mapLocation();
         paths[p.x][p.y][target.x][target.y].seq.remove(0);
      }
      else
      {
      
      }
      return false;
   }
   public static ArrayList<Unit>[] sortUnitTypes(VecUnit units) {
      ArrayList<Unit>[] unitsByType = new ArrayList[7];
      for(int x = 0; x < 7; x++)
         unitsByType[x] = new ArrayList<Unit>();
      for(int x = 0; x < units.size(); x++)
      {
         UnitType ut = units.get(x).unitType();
         if(ut.equals(UnitType.Factory))
            unitsByType[0].add(units.get(x));
         else if(ut.equals(UnitType.Healer))
            unitsByType[1].add(units.get(x));
         else if(ut.equals(UnitType.Knight))
            unitsByType[2].add(units.get(x));
         else if(ut.equals(UnitType.Mage))
            unitsByType[3].add(units.get(x));
         else if(ut.equals(UnitType.Ranger))
            unitsByType[4].add(units.get(x));
         else if(ut.equals(UnitType.Rocket))
            unitsByType[5].add(units.get(x));
         else if(ut.equals(UnitType.Worker))
            unitsByType[6].add(units.get(x));
         else
            System.out.println("Error, Unknown unit type: "+ut);
      }
      return unitsByType;
   }
   public static int spaces(MapLocation m)
   {
      Pair id = mapID(m);
      int count = 0;
      for(int x = Math.max(0, id.x-1); x <= Math.min(eWidth,id.x+1); x++)
         for(int y = Math.max(0, id.y-1); y <= Math.min(eHeight,id.y+1); y++)
            if(id.x!=x||id.y!=y)
               if(passable[x][y])
                  count++;
      return count;
   }
   public static Path findPath(MapLocation start, MapLocation end, ArrayList<MapLocation> avoid)
   {
      Pair startID = mapID(start);
      Pair endID = mapID(end);
      int[][] dist = new int[eWidth][eHeight];
      Pair[][] prev = new Pair[eWidth][eHeight];
      for(int x = 0; x < dist.length; x++)
         Arrays.fill(dist[x], -1);
      dist[startID.x][startID.y] = 0;
      LinkedList<Pair> q = new LinkedList<Pair>();
      q.add(startID);
      Pair id = null;
      boolean pathFound = false;
      while(!q.isEmpty())
      {
         id = q.remove();
         if(id.equals(endID))
         {
            pathFound = true;
            break;
         }
         for(Direction d: directions)
            if(!d.equals(Direction.Center))
               try
               {
                  Pair next = mapID(eMapLoc[id.x][id.y].add(d));
                  if(checkAvoid(next, avoid)&&dist[next.x][next.y]==-1)
                  {
                     dist[next.x][next.y] = dist[id.x][id.y]+1;
                     prev[next.x][next.y] = id;
                     q.add(next);
                  }
               }catch(Exception e) {e.printStackTrace();}
      }   
      if(!pathFound)
         return null;
      Path p = new Path(eMapLoc[startID.x][startID.y], eMapLoc[endID.x][endID.y]);
      while(prev[id.x][id.y]!=null)
      {
         p.seq.add(eMapLoc[id.x][id.y].directionTo(eMapLoc[prev[id.x][id.y].x][prev[id.x][id.y].y]));
         id = prev[id.x][id.y];
      }
      return p;
   }
   public static boolean checkAvoid(Pair id, ArrayList<MapLocation> avoid)
   {
      if(id.x<0||id.x>=eWidth||id.y<0||id.y>=eHeight)
         return false;
      if(avoid!=null)
         for(int a = 0; a < avoid.size(); a++)
            if(mapID(avoid.get(a)).equals(id))
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
   public static ArrayList<KarbAdjacent> countMaxKarbAdj(MapLocation m)
   {
      boolean[][] used = new boolean[eWidth][eHeight];
      ArrayList<KarbAdjacent> karbAdjOrder = new ArrayList<KarbAdjacent>();
      LinkedList<Pair> q = new LinkedList<Pair>();
      used[mapID(m).x][mapID(m).y] = true;
      q.add(mapID(m));
      while(!q.isEmpty())
      {
         Pair id = q.remove();
         karbAdjOrder.add(new KarbAdjacent(id, countKarbAdj(id.x, id.y)));
         for(Direction d: directions)
            try{
               Pair next = mapID(eMapLoc[id.x][id.y].add(d));   
               if(checkAvoid(next, null))
               {
                  used[next.x][next.y] = true;
                  q.add(next);
               }
            } catch(Exception e) {e.printStackTrace();}
      }
      Collections.sort(karbAdjOrder);
      return karbAdjOrder;
   }
   public static Pair mapID(MapLocation m)
   {
      return new Pair(m.getX(),m.getY());
   }
   static class Path
   {
      MapLocation start;
      MapLocation end;
      ArrayList<Direction> seq;
      public Path(MapLocation s, MapLocation e)
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
      Pair id;
      MapLocation ml;
      int dep;
   
      public KarbAdjacent(Pair p, int d) {
         id = p;
         ml = eMapLoc[id.x][id.y];
         dep = d;
      }
   // greatest goes first
      public int compareTo(KarbAdjacent x) {
         return x.dep-dep;
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