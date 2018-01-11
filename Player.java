import bc.*;
import java.util.*;
@SuppressWarnings("unchecked")
public class Player {
   //CONSTANTS
   static int maxRound = 1000;   // the number of rounds
   static Planet earth = Planet.Earth;
   static Planet mars = Planet.Mars;
   static Direction[] directions = Direction.values();
   //Other Stuff
   static GameController gc = new GameController();;
   static PlanetMap eMap = gc.startingMap(earth);
   static PlanetMap mMap = gc.startingMap(mars);
   public static void main(String[] args) {
      if(gc.planet().equals(Planet.Earth))
         earth();
      else
         mars();
   }
   public static void earth() {
      PriorityQueue<KarbDeposit> earthKarbs = new PriorityQueue<KarbDeposit>();
      boolean[][] passable = new boolean[(int)eMap.getWidth()][(int)eMap.getHeight()];
      KarbDeposit[][] karbDep = new KarbDeposit[(int)eMap.getWidth()][(int)eMap.getHeight()];
      MapLocation[][] eMapLoc = new MapLocation[(int)eMap.getWidth()][(int)eMap.getHeight()];
      for(int i = 0; i < (int) eMap.getWidth(); i++) {
         for(int j = 0; j < (int) eMap.getHeight(); j++) {
         
            eMapLoc[i][j] = new MapLocation(earth,i,j);
            karbDep[i][j] = new KarbDeposit(eMapLoc[i][j], eMap.initialKarboniteAt(eMapLoc[i][j]));
            if(karbDep[i][j].dep>0)
               earthKarbs.add(karbDep[i][j]);
         }
      }
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
            int factoriesBuilt = 0; //number of factories that were built this round
            long karbs = gc.karbonite();
          
            for(int x = 0; x < workers.size(); x++)
            {
               Unit w = workers.get(x);
               if(factories.size()==0)
                  for(int y = 0; y < 8; y++)
                     try{
                        gc.blueprint(w.id(), UnitType.Factory, directions[y]);
                        System.out.println("Worker "+w.id()+" blueprinted a factory");
                        break;
                     } catch(Exception e) {e.printStackTrace();}
               else
                  for(int y = 0; y < 8; y++)
                     try{
                        gc.replicate(w.id(), directions[y]);
                        System.out.println("Worker "+w.id()+" replicated");
                        break;
                     } catch(Exception e) {e.printStackTrace();}
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
