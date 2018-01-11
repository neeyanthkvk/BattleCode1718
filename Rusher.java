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

   // Initial Earth Stuff
   static HashMap<MapLocation, Integer> mapToNum = new HashMap<MapLocation, Integer>();
   static MapLocation[][] eMapLoc = new MapLocation[eWidth][eHeight];
   static long[][] karbDep = new long[eWidth][eHeight];
   static boolean[][] passable = new boolean[eWidth][eHeight];
   static Path[][] paths = new Path[eWidth][eHeight];
   static {
      for(int i = 0; i < eWidth; i++) {
         for(int j = 0; j < eHeight; j++) {
            eMapLoc[i][j] = new MapLocation(earth,i,j);
            mapToNum.put(eMapLoc[i][j], i+j*eWidth);
            karbDep[i][j] = eMap.initialKarboniteAt(eMapLoc[i][j]);
            passable[i][j] = eMap.isPassableTerrainAt(eMapLoc[i][j])==0;
         }
      }
   
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
      System.out.println("Running Earth Player: ");
      int curRound = 1;
      System.out.println("Earth Round "+curRound+": ");
      VecUnit initUnits = eMap.getInitial_units();
      VecUnit units = gc.myUnits();
      Unit primary = null;
      team = primary.team();
      boolean enemyConnected = false;
      init: for(int primaryI = 0; primaryI < units.size(); primaryI++)
      {
         primary = units.get(primaryI);
         for(int targetI = 0; targetI < initUnits.size(); targetI++)
            if(!initUnits.get(targetI).team().equals(team))
            {
               MapLocation m1 = primary.location().mapLocation();
               MapLocation m2 = initUnits.get(targetI).location().mapLocation();
               int i1 = mapToNum.get(m1);
               int i2 = mapToNum.get(m2);
               paths[i1][i2] = findPath(m1, m2, new ArrayList<MapLocation>());
               if(paths[i1][i2]!=null)
               {   
                  gc.moveRobot(primary.id(), paths[i1][i2].seq.get(0));
                  gc.blueprint(primary.id(), UnitType.Factory, paths[i1][i2].seq.get(0));
                  enemyConnected = true;
                  break init;
               }
            }
      }
      curRound++;
      gc.nextTurn();
      if(enemyConnected)
         for(; curRound <= maxRound; curRound++)
         {
         //try-catch is used for everything since if we have an uncaught exception, we lose
            try
            {
               System.out.println("Earth Round "+curRound+": ");
               units = gc.myUnits();
            
             //Each arraylist contains all of the units of that type
               ArrayList<Unit>[] ubt = sortUnitTypes(units);
               ArrayList<Unit> healers = ubt[0];
               ArrayList<Unit> factories = ubt[1];
               ArrayList<Unit> knights = ubt[2];
               ArrayList<Unit> mages = ubt[3];
               ArrayList<Unit> rangers = ubt[4];
               ArrayList<Unit> rockets = ubt[5];
               ArrayList<Unit> workers = ubt[6];
               long karbs = gc.karbonite();
               
            }
            catch(Exception e) {
               e.printStackTrace();
            }
         //End turn
            gc.nextTurn(); 
         }    
   }
   public static void mars() {
      // Do Something
      System.out.println("Mars Stuffs");
      
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
      int i = mapToNum.get(m);
      int xPos = i%eMapLoc.length;
      int yPos = i/eMapLoc.length;
      int count = 0;
      for(int x = Math.max(0, xPos-1); x <= Math.min(eWidth,xPos+1); x++)
         for(int y = Math.max(0, yPos-1); y <= Math.min(eHeight,yPos+1); y++)
            if(xPos!=x||yPos!=y)
               if(passable[x][y])
                  count++;
      return count;
   }
   public static Path findPath(MapLocation start, MapLocation end, ArrayList<MapLocation> avoid)
   {
      int startI = mapToNum.get(start);
      int endI = mapToNum.get(end);
      int[][] dist = new int[eWidth][eHeight];
      int[][] prev = new int[eWidth][eHeight];
      for(int x = 0; x < prev.length; x++)
         Arrays.fill(prev[x], -1);
      LinkedList<Integer> q = new LinkedList<Integer>();
      q.add(startI);
      int i = 0;
      boolean pathFound = false;
      while(!q.isEmpty())
      {
         i = q.remove();
         if(i==endI)
         {
            pathFound = true;
            break;
         }
         int x = i%(int)eWidth;
         int y = i/(int)eWidth;
      //North
         if(checkBounds(x,y+1,avoid)&&dist[x][y+1]==-1&&passable[x][y+1])
         {
            dist[x][y+1] = dist[x][y]+1;
            prev[x][y+1] = (int)(x+y*eWidth);
            q.add((int)((x)+(y+1)*eWidth));
         }
      //Northeast
         if(checkBounds(x+1,y+1,avoid)&&dist[x+1][y+1]==-1&&passable[x+1][y+1])
         {
            dist[x+1][y+1] = dist[x][y]+1;
            prev[x+1][y+1] = (int)(x+y*eWidth);
            q.add((int)((x+1)+(y+1)*eWidth));
         }
      //East
         if(checkBounds(x+1,y,avoid)&&dist[x+1][y]==-1&&passable[x+1][y])
         {
            dist[x+1][y] = dist[x][y]+1;
            prev[x+1][y] = (int)(x+y*eWidth);
            q.add((int)((x+1)+(y)*eWidth));
         }
      //Southeast
         if(checkBounds(x+1,y-1,avoid)&&dist[x+1][y-1]==-1&&passable[x+1][y-1])
         {
            dist[x+1][y-1] = dist[x][y]+1;
            prev[x+1][y-1] = (int)(x+y*eWidth);
            q.add((int)((x+1)+(y-1)*eWidth));
         }
      //South
         if(checkBounds(x,y-1,avoid)&&dist[x][y-1]==-1&&passable[x][y-1])
         {
            dist[x][y-1] = dist[x][y]+1;
            prev[x][y-1] = (int)(x+y*eWidth);
            q.add((int)((x)+(y-1)*eWidth));
         }
      //Southwest
         if(checkBounds(x-1,y-1,avoid)&&dist[x-1][y-1]==-1&&passable[x-1][y-1])
         {
            dist[x-1][y-1] = dist[x][y]+1;
            prev[x-1][y-1] = (int)(x+y*eWidth);
            q.add((int)((x-1)+(y-1)*eWidth));
         }
      //West
         if(checkBounds(x-1,y,avoid)&&dist[x+1][y]==-1&&passable[x-1][y])
         {
            dist[x-1][y] = dist[x][y]+1;
            prev[x-1][y] = (int)(x+y*eWidth);
            q.add((int)((x-1)+(y)*eWidth));
         }
      //Northwest
         if(checkBounds(x-1,y+1,avoid)&&dist[x-1][y+1]==-1&&passable[x-1][y+1])
         {
            dist[x-1][y+1] = dist[x][y]+1;
            prev[x-1][y+1] = (int)(x+y*eWidth);
            q.add((int)((x-1)+(y+1)*eWidth));
         }
      }   
      if(!pathFound)
         return null;
      Path p = new Path(eMapLoc[startI%eWidth][startI/eWidth], eMapLoc[endI%eWidth][endI/eWidth]);
      while(prev[i%eWidth][i/eWidth]!=-1)
      {
         p.seq.add(eMapLoc[i%eWidth][i/eWidth].directionTo(eMapLoc[prev[i%eWidth][i/eWidth]%eWidth][prev[i%eWidth][i/eWidth]/eWidth]));
         i = prev[i%eWidth][i/eWidth];
      }
      return p;
   }
   public static boolean checkBounds(int x, int y, ArrayList<MapLocation> avoid)
   {
      if(x<0||x>=eWidth||y<0||y>=eHeight)
         return false;
      for(int a = 0; a < avoid.size(); a++)
         if(mapToNum.get(avoid.get(a))==x+y*eWidth)
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
   public static void floodfill()
   {
   
   
   
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
}