import bc.*;
import java.util.*;
import java.io.*;
@SuppressWarnings("unchecked")
public class Player {
   static long startTime = System.currentTimeMillis();
   static int maxRound = 1000;
   static Planet earth = Planet.Earth;
   static Planet mars = Planet.Mars;
   static Direction[] directions = Direction.values();
   static Direction[] cardinals = {Direction.North, Direction.East, Direction.South, Direction.West};
   static Direction[] diagonals = {Direction.Northeast, Direction.Southeast, Direction.Northwest, Direction.Southwest};
   static GameController gc = new GameController();;
   static PlanetMap eMap = gc.startingMap(earth);
   static PlanetMap mMap = gc.startingMap(mars);
   static int eWidth = (int) eMap.getWidth();
   static int eHeight = (int) eMap.getHeight();
   static int mWidth = (int) mMap.getWidth();
   static int mHeight = (int) mMap.getHeight();
   static Team myTeam = gc.team();
   static Team enemyTeam;
   static Random random = new Random(1);
   static MapLocation[][] eMapLoc = new MapLocation[eWidth][eHeight];
   static MapLocation[][] mMapLoc = new MapLocation[mWidth][mHeight];
   static int[][] eKarbDep = new int[eWidth][eHeight]; //amount of karbonite in the square
   static int[][] eKarbAdj = new int[eWidth][eHeight]; //sum of karbonite on and adjacent to the square

   public static void main(String[] args) {
      System.out.println("First random value is "+random.nextDouble());
         
      //Research
      System.out.println("Queueing research");
      gc.queueResearch(UnitType.Worker);//25 // 25 Rounds - "Gimme some of that Black Stuff"
      gc.queueResearch(UnitType.Ranger);//50 // 25 Rounds - "Get in Fast"
      gc.queueResearch(UnitType.Ranger);//150// 100 Rounds - "Scopes"
      gc.queueResearch(UnitType.Worker);//225// 75 Rounds - "Time is of the Essence"
      gc.queueResearch(UnitType.Rocket);//325// 100 Rounds - "Rocketry"
      gc.queueResearch(UnitType.Worker);//400// 75 Rounds - "Time is of the Essence II"
      gc.queueResearch(UnitType.Worker);//475// 75 Rounds - "Time is of the Essence III"
      gc.queueResearch(UnitType.Rocket);//525// 100 Rounds - "Rocket Boosters"
      gc.queueResearch(UnitType.Rocket);//625// 100 Rounds - "Increased Capacity"
      System.out.println("Finished queueing research");
   
         //initializes eMapLoc, karbDep
      System.out.println("Initializing basic map info");
      for(int x = 0; x < eWidth; x++)
         for(int y = 0; y < eHeight; y++) 
            try {
               eMapLoc[x][y] = new MapLocation(earth,x,y);
               eKarbDep[x][y] = (int) eMap.initialKarboniteAt(eMapLoc[x][y]);
            } 
            catch(Exception e) {e.printStackTrace();}
      for(int x = 0; x < mWidth; x++)
         for(int y = 0; y < mHeight; y++) 
            mMapLoc[x][y] = new MapLocation(mars,x,y);
      System.out.println("Finished initializing basic map info");
   
         //initializes karbAdj
      System.out.println("Finding karbonite sums");
      for(int x = 0; x < eWidth; x++)
         for(int y = 0; y < eHeight; y++)
            try {
               eKarbAdj[x][y] = countKarbAdj(new Pair(x, y), eKarbDep, false);
            } 
            catch(Exception e) {e.printStackTrace();}
      System.out.println("Finished finding karbonite sums");
   
   
   
      if(gc.planet().equals(Planet.Earth))
         Earth.run();
      else
         Mars.run();
   }
   public static int countKarbAdj(Pair p, int[][] karbDep, boolean useCur)
   {
      int xPos = p.x;
      int yPos = p.y;
      if(useCur)
      {
         int count = 0;
         for(int x = Math.max(0, xPos-1); x < Math.min(eWidth-1, xPos+1); x++)
            for(int y = Math.max(0, yPos-1); y < Math.min(eHeight-1, yPos+1); y++)
               count+=gc.karboniteAt(eMapLoc[x][y]);
         return count;
      }
      else
      {
         int count = 0;
         for(int x = Math.max(0, xPos-1); x < Math.min(eWidth-1, xPos+1); x++)
            for(int y = Math.max(0, yPos-1); y < Math.min(eHeight-1, yPos+1); y++)
               count+=karbDep[x][y];
         return count;
      }
   }
   public static Direction findDirection(Pair start, Pair end)
   {
      return eMapLoc[start.x][start.y].directionTo(eMapLoc[end.x][end.y]);
   }
   public static HashSet<Integer> toIDList(VecUnit vu)
   {
      HashSet<Integer> ret = new HashSet<Integer>();
      for(int x = 0; x < vu.size(); x++)
         ret.add(vu.get(x).id());
      return ret;
   }
   public static boolean isEmpty(Pair id, MapLocation[][] mapLoc)
   {
      return !gc.hasUnitAtLocation(mapLoc[id.x][id.y]);
   }
   public static Pair mapPair(MapLocation m)
   {
      return new Pair(m.getX(),m.getY());
   }
   public static Pair unitPair(int id)
   {
      return mapPair(gc.unit(id).location().mapLocation());
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
   public static HashSet<Pair> locWithin(Pair target, MapLocation[][] mapLoc, int minRange, int maxRange)
   {
      VecMapLocation minVec = gc.allLocationsWithin(mapLoc[target.x][target.y], minRange);
      VecMapLocation maxVec = gc.allLocationsWithin(mapLoc[target.x][target.y], maxRange);
      HashSet<Pair> ret = new HashSet<Pair>();
      for(int x = 0; x < maxVec.size(); x++)
         ret.add(mapPair(maxVec.get(x)));
      for(int y = 0; y < minVec.size(); y++)
         ret.remove(mapPair(minVec.get(y)));
      return ret;
   }
   public static boolean inGarrison(int id)
   {
      return gc.unit(id).location().isInGarrison();
   }
   public static boolean isCardinal(Pair p1, Pair p2)
   {
      MapLocation m1 = eMapLoc[p1.x][p1.y];
      MapLocation m2 = eMapLoc[p2.x][p2.y];
      for(Direction d: cardinals)
         if(m1.add(d).equals(m2))
            return true;
      return false;
   }
   public static boolean isDiagonal(Pair p1, Pair p2)
   {
      MapLocation m1 = eMapLoc[p1.x][p1.y];
      MapLocation m2 = eMapLoc[p2.x][p2.y];
      for(Direction d: diagonals)
         if(m1.add(d).equals(m2))
            return true;
      return false;
   }
   public static int bestUnitType(Unit ua, Unit ub)
   {
      UnitType u1 = ua.unitType();
      UnitType u2 = ub.unitType();
      if(u1.equals(UnitType.Mage))
         return -1;
      if(u2.equals(UnitType.Mage))
         return 1;
      if(u1.equals(UnitType.Factory)&&ua.structureIsBuilt()!=0)
         return -1;
      if(u2.equals(UnitType.Factory)&&ub.structureIsBuilt()!=0)
         return 1;
      if(u1.equals(UnitType.Knight))
         return -1;
      if(u2.equals(UnitType.Knight))
         return 1;
      if(u1.equals(UnitType.Ranger))
         return -1;
      if(u2.equals(UnitType.Ranger))
         return 1;
      if(u1.equals(UnitType.Factory))
         return -1;
      if(u2.equals(UnitType.Factory))
         return 1;
      if(u1.equals(UnitType.Healer))
         return -1;
      if(u2.equals(UnitType.Healer))
         return 1;
      if(u1.equals(UnitType.Worker))
         return -1;
      if(u2.equals(UnitType.Worker))
         return 1;
      if(u1.equals(UnitType.Rocket))
         return -1;
      if(u2.equals(UnitType.Rocket))
         return 1;
      return 0;
   }
   public static int squareDist(Pair p1, Pair p2)
   {
      return (p1.x-p2.x)*(p1.x-p2.x)+(p1.y-p2.y)*(p1.y-p2.y);
   }
   public static boolean isStructure(int id)
   {
      return gc.unit(id).unitType().equals(UnitType.Factory)||gc.unit(id).unitType().equals(UnitType.Rocket);   
   }
   //EARTH PLAYER
   static class Earth {
   //Basic Info 
      static boolean rocketResearched = false;
      static boolean[][] occupied = new boolean[eWidth][eHeight];
      static boolean[][] launchLoc = new boolean[mWidth][mHeight];
   
      static HashSet<Integer> myInit = new HashSet<Integer>();
      static HashMap<Integer, Pair> enemyInit = new HashMap<Integer, Pair>();
      static HashSet<Integer> myUnits;
      static HashSet<Integer> healers;
      static HashSet<Integer> factories;
      static HashSet<Integer> knights;
      static HashSet<Integer> mages;
      static HashSet<Integer> rangers;
      static HashSet<Integer> rockets;
      static HashSet<Integer> workers;
      static HashSet<Integer> enemyUnits;
      static HashSet<Pair> attackList = new HashSet<Pair>();
      
      static {
         try {
         //initializes teams
            if(myTeam==Team.Red)
               enemyTeam = Team.Blue;
            else
               enemyTeam = Team.Red;
            
         //update units
               
               
         //initializes myInit and enemyInit
            System.out.println("Initializing all initial units");
            VecUnit allInit = eMap.getInitial_units();
            for(int i = 0; i < allInit.size(); i++)
               try {
                  if(myUnits.contains(allInit.get(i).id()))
                     myInit.add(allInit.get(i).id());
                  else
                     enemyInit.put(allInit.get(i).id(), mapPair(allInit.get(i).location().mapLocation()));
               } 
               catch(Exception e) {e.printStackTrace();}
            System.out.println("Finished initializing all initial units");
            
            for(int w: myInit)
               System.out.println("My initial workers are: "+w); 
            
            
         //end of initialization
         } 
         catch(Exception e) {e.printStackTrace();}
      }
      public static void run() {
      //rounds 1-1000
         while(gc.round()<=maxRound)
         {
            try
            {
               System.out.println("Earth Round "+gc.round()+": ");
               System.out.println("Time used: "+(System.currentTimeMillis()-startTime));
                     
            
               for(int id: workers)
               {
                  Unit u = gc.unit(id);
                  if(u.location().isInGarrison()||u.location().isInSpace())
                  {
                     continue;
                  }
                  
               }
               for(int id: rangers)
               {
                  Unit u = gc.unit(id);
                  if(u.location().isInGarrison()||u.location().isInSpace())
                  {
                     continue;
                  }
                  
               }
               for(int id: factories)
               {
                  Unit u = gc.unit(id);
                  if(u.structureIsBuilt()!=0)
                     continue;
                  
               }
               for(int id: rockets)
               {
                  Unit u = gc.unit(id);
                  if(u.location().isInSpace()&&u.structureIsBuilt()!=0)
                     continue;
                  if(u.health()<u.maxHealth())
                     if(u.structureGarrison().size()>0)
                     {
                        
                     }
                  if(u.structureGarrison().size()==8||gc.round()==749)
                  {
                     
                  }
               }
            } 
            catch(Exception e) {e.printStackTrace();} 
            gc.nextTurn();
         }
         //End of match
      }
      
      public static boolean inBounds(Pair id)
      {
         return id.x>=0&&id.x<eWidth&&id.y>=0&&id.y<eHeight;
      }
      public static boolean inBounds(int x, int y)
      {
         return x>=0&&x<eWidth&&y>=0&&y<eHeight;
      }
   }
   //MARS PLAYER
   static class Mars {
      public static void run() {
         while(gc.round()<=maxRound)
         {
            System.out.println("Mars Round: "+gc.round());
            //System.out.println("Time left: "+gc.getTimeLeftMs());
            gc.nextTurn();
         }
      }
      public static boolean inBounds(Pair id)
      {
         return id.x>=0&&id.x<mWidth&&id.y>=0&&id.y<mHeight;
      }
      public static boolean inBounds(int x, int y)
      {
         return x>=0&&x<mWidth&&y>=0&&y<mHeight;
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
   public boolean equals(Object o)
   {
      if(o==null)
         return false;
      Pair p = (Pair) o;
      return x==p.x&&y==p.y;
   }
   public String toString()
   {
      return x+" "+y;
   }
   public int hashCode()
   {
      return x*100+y;
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