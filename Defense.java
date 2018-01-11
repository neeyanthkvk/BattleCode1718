import bc.*;
import java.util.*;
@SuppressWarnings("unchecked")
public class Defense {
   //CONSTANTS
   static int maxRound = 1000;   // the number of rounds
   static Planet earth = Planet.Earth;
   static Planet mars = Planet.Mars;
   static Direction[] directions = Direction.values();
   
   //Other Stuff
   static GameController gc = new GameController();;
   static PlanetMap eMap = gc.startingMap(earth);
   static PlanetMap mMap = gc.startingMap(mars);

   // Initial Earth Stuff
   static boolean[][] passable;
   static PriorityQueue<KarbDeposit> earthKarbs;
   static KarbDeposit[][] karbDep;
   static MapLocation[][] eMapLoc;
   
   static {
        for(int i = 0; i < (int) eMap.getWidth(); i++) {
            for(int j = 0; j < (int) eMap.getHeight(); j++) {
                eMapLoc[i][j] = new MapLocation(earth,i,j);
                karbDep[i][j] = new KarbDeposit(eMapLoc[i][j],eMap.initialKarboniteAt(eMapLoc[i][j]));

                if(karbDep[i][j].dep > 0) {
                    earthKarbs.add(karbDep[i][j]);
                }
            }
        }

        /* Start Strategy 1 - Grind & Defend
         * Generate as much Karbonite as Possible
         * Have Healers, Rangers, and Knights as defense for Rangers
         * Late-Game: No need to Dominate Earth - Attempt to Dominate Mars Instead. Bring Mages, Healers, and Knights to dominate Mars. <----- WIN CONDITION
         * */
        gc.queueResearch(UnitType.Worker);  // 25 Rounds - "Gimme some of that Black Stuff"
        gc.queueResearch(UnitType.Worker);  // 75 Rounds - "Time is of the Essence"
        gc.queueResearch(UnitType.Ranger);  // 25 Rounds - "Get in Fast"
        gc.queueResearch(UnitType.Healer);  // 25 Rounds - "Spirit Water"
        gc.queueResearch(UnitType.Mage);    // 25 Rounds - "Glass Cannon"
        gc.queueResearch(UnitType.Rocket);  // 100 Rounds - "Rocketry"
        gc.queueResearch(UnitType.Knight);  // 25 Rounds - "Armor"
        gc.queueResearch(UnitType.Knight);  // 75 Rounds - "Even More Armor"
        gc.queueResearch(UnitType.Mage);    // 75 Rounds - "Glass Cannon II"
        gc.queueResearch(UnitType.Rocket);  // 100 Rounds - "Rocket Boosters"
        
        /*End Strategy 1*/
   }



   public static void main(String[] args) {
      if(gc.planet().equals(Planet.Earth))
         earth();
      else
         mars();
   }
   
   
   public static void earth() {
      int numDep = earthKarbs.size();
      for(int curRound = 0; curRound < maxRound; curRound++)
      {
         //try-catch is used for everything since if we have an uncaught exception, we lose
         try
         {
            System.out.println("Earth Round "+curRound+": ");
            VecUnit units = gc.myUnits();
         
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
          
            for(int x = 0; x < workers.size(); x++)
            {
               Unit w = workers.get(x);
               cycle: for(int y = 0; y < factories.size(); y++)
                  try {//not sure if structureIsBuilt() = 0 is when the structure isn't built
                     if(factories.get(y).structureIsBuilt()==0&&w.location().mapLocation().isAdjacentTo(factories.get(y).location().mapLocation()))
                     {
                        gc.build(w.id(),factories.get(y).id());
                        System.out.println("Worker "+w.id()+" built factory "+factories.get(y).id());
                        break cycle;
                     }
                  } catch(Exception e) {e.printStackTrace();}
               if(factories.size()==0)
                  cycle: for(int y = 0; y < 8; y++)
                     try {
                        gc.blueprint(w.id(), UnitType.Factory, directions[y]);
                        System.out.println("Worker "+w.id()+" blueprinted a factory");
                        break cycle;
                     } catch(Exception e) {e.printStackTrace();}
               else
                  cycle: for(int y = 0; y < 8; y++)
                     try {
                        gc.replicate(w.id(), directions[y]);
                        System.out.println("Worker "+w.id()+" replicated");
                        break cycle;
                     } catch(Exception e) {e.printStackTrace();}
            }
            for(int x = 0; x < factories.size(); x++)
            {
            
            
            }
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
}
class KarbDeposit implements Comparable<KarbDeposit> {
   MapLocation ml;
   long dep;
	
   public KarbDeposit(MapLocation m, long d) {
      this.ml = m;
      this.dep = d;
   }
	// LEAST GOES FIRST
   public int compareTo(KarbDeposit x) {
      return (int) (0-1) * (int) (this.dep-x.dep);
   }
	
}
