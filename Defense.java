import bc.*;
import java.util.*;

@SuppressWarnings("unchecked")

public class Defense {
   // CONSTANTS
   static int maxRound = 1000;   // the number of rounds
   static Planet earth = Planet.Earth;
   static Planet mars = Planet.Mars;
   static Direction[] directions = Direction.values();
      
   // Other Stuff
   static GameController gc = new GameController();;
   static PlanetMap eMap = gc.startingMap(earth);
   static PlanetMap mMap = gc.startingMap(mars);
   static AsteroidPattern ap = gc.asteroidPattern();
   
   // Initial Earth Stuff
   static int eWidth = (int) eMap.getWidth();
   static int eHeight = (int) eMap.getHeight();
   static boolean[][] passable = new boolean[eWidth][eHeight];
   static int maxFactory = 8;
   static int maxRockets = 15;
   
   // Map Stuff
   static boolean[][] beingMined = new boolean[eWidth][eHeight];
   static MapLocation[][] eMapLoc = new MapLocation[eWidth][eHeight];
   static long[][] karbDep = new long[eWidth][eHeight];//amount of karbonite in the square
   static long[][] karbAdj = new long[eWidth][eHeight];//sum of karbonite on and adjacent to the square
   static Path[][][][] paths = new Path[eWidth][eHeight][eWidth][eHeight];
   static HashMap<Integer, PriorityQueue<KarbAdjacent>> bestKarbAdj = new HashMap<Integer, PriorityQueue<KarbAdjacent>>();


   // Worker Stuff
   static HashMap<Integer,WorkerTask> workerTask = new HashMap<Integer,WorkerTask>();
   
   // Factory Stuff
   static HashMap<Integer,Direction> deployDirection = new HashMap<Integer,Direction>();

   static {
        for(int i = 0; i < (int) eMap.getWidth(); i++) {
            for(int j = 0; j < (int) eMap.getHeight(); j++) {
                eMapLoc[i][j] = new MapLocation(earth,i,j);
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
        gc.queueResearch(UnitType.Worker);   // 75 Rounds - "Time is of the Essence II"
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
   }



   public static void main(String[] args) {
      while(true) { 
      if(gc.planet().equals(Planet.Earth))
         earth();
      else
         mars();
      }
   }
   
   
   public static void earth() {
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
            for(int index = 0; index < workers.size(); index++)
            {
                Unit w = workers.get(index);
                // Mining Karbonite?
                WorkerTask t = workerTask.get(w.id());
                System.out.println(t.type);
                if(t.type.equals("mine")) {
                    Direction dir = t.mine;
                    long karboniteAt = gc.karboniteAt(gc.unit(t.unitID).location().mapLocation().add(dir));
                    if(karboniteAt > 0) {
                        gc.harvest(w.id(),dir);     
                    }
                    else {
                        // Karbonite Deposit is Empty - What should it do? Just look for another Mine or Be Smarter?
                        t.type = "getMine";
                        t.target = null;
                        t.mine = null;
                    }
                }
                if(t.type.equals("getMine")) {
                    // GET TO CLOSEST MINE
                	System.out.println("GET TO THE MINE");
                	if(t.mine == null) {
                		// Find a Suitable Mine
                		PriorityQueue<KarbAdjacent> q = countMaxKarbAdj(w.location().mapLocation());
                		bestKarbAdj.put(w.id(), q);
                		KarbAdjacent ka = q.remove();
                	}
                	
                }
                
                if(t.type.equals("build")) {
                	gc.build(w.id(), t.blueprint_id);
                	
                	
                }
                
                // Blueprinting Stuffs
                if(t.type.equals("printRocket")) {
                    if(gc.karbonite() > 250) {
                        Direction d = null;
                        int idNew = -1;
                        for(Direction temp:directions) {
                            if(gc.canBlueprint(w.id(),UnitType.Rocket,temp)) {
                                d = temp;
                                gc.blueprint(w.id(),UnitType.Rocket,temp);
                            }
                        }
                        if(d != null) {
                            MapLocation ml = w.location().mapLocation().add(d);
                            VecUnit tempUnits = gc.myUnits();  
                            for(int i = 0; i < tempUnits.size(); i++) {
                                Unit tempUnit = tempUnits.get(i);
                                if(tempUnit.location().mapLocation().equals(ml)) {
                                    idNew = tempUnit.id();
                                    break;
                                }
                            }
                            t.blueprint_id = idNew;
                            t.type = "build";
                        }
                        else {
                           // No Where to GO??????
                           // This Robot Can't do anything... what do we do :( (I guess we can make the blueprinting method in general smarter)
                           System.out.println("Rip Robot " + w.id() + " he can't go anywhere.");
                        }
                        
                    }
                    else {
                        // Send him to go get Karbonite -- Emplace RocketMaker in the Queue
                        t.type = "getMine";
                    }
                }
                if(t.type.equals("printFactory")) {
                	int x = w.location().mapLocation().getX();
                	int y = w.location().mapLocation().getY();
                	if(t.build == null) {
                		// Needs to find a place to build
                		// First, find the edge closest to the robot
                		int down = y;
                		int left = x;
                		int up = eHeight - y;
                		int right = eWidth-x;
                		int min = Math.min(Math.min(down, left), Math.min(up, right));
                		Direction toGo = null;
                		if(down == min) {
                			toGo = Direction.South;
                		}
                		if(left == min) {
                			toGo = Direction.West;
                		}
                		if(right == min) {
                			toGo = Direction.East;
                		}
                		if(up == min) {
                			toGo = Direction.North;
                		}
                		
                	}
                    if(gc.karbonite() > 300) {
                        Direction d = null;
                        int idNew = -1;
                        
                        for(Direction temp:directions) {
                            if(gc.canBlueprint(w.id(),UnitType.Factory,temp)) {
                                d = temp;
                                gc.blueprint(w.id(),UnitType.Factory,temp);
                            }
                        }
                        if(d != null) {
                            MapLocation ml = w.location().mapLocation().add(d);
                            VecUnit tempUnits = gc.myUnits();  
                            for(int i = 0; i < tempUnits.size(); i++) {
                                Unit tempUnit = tempUnits.get(i);
                                if(tempUnit.location().mapLocation().equals(ml)) {
                                    idNew = tempUnit.id();
                                    break;
                                }
                            }
                            t.blueprint_id = idNew;
                            t.type = "build";
                        }
                        else {
                           // No Where to GO??????
                           // This Robot Can't do anything... what do we do :( (I guess we can make the blueprinting method in general smarter)
                           System.out.println("Rip Robot " + w.id() + " he can't go anywhere.");
                        }
                        
                    }
                    else {
                        // Send him to go get Karbonite -- Emplace RocketMaker in the Queue
                        t.type = "getMine";
                    }
                }
            }   
            for(int x = 0; x < factories.size(); x++) {
                Unit fact = factories.get(x);
                int id = fact.id();
                if(fact.isFactoryProducing() != 0) {
                	if(workers.size() < 10 && rangers.size() / workers.size() > 1) {
                		if(gc.canProduceRobot(id, UnitType.Worker)) {
                			gc.produceRobot(id, UnitType.Worker);
                		}
                	}
                	else {
                		if(gc.canProduceRobot(id, UnitType.Ranger)) {
                			gc.produceRobot(id, UnitType.Ranger);
                		}
                	}
                }
                VecUnitID garr = fact.structureGarrison();
                int index = 0;
                for(Direction d:directions) {
                	while(gc.canUnload(id, d)) {
                		int garrID = garr.get(index);
                		gc.unload(id, d);
                		index += 1;
                		Unit newUnit = gc.unit(garrID);
                		if(newUnit.unitType() == UnitType.Worker) {
                			VecUnit tempUnits = gc.myUnits();
                			int rocketCount = 0;
                			int factoryCount = 0;
                			for(int i = 0; i < tempUnits.size(); i++) {
                				if(tempUnits.get(i).unitType() == UnitType.Rocket) {
                					rocketCount += 1;
                				}
                				if(tempUnits.get(i).unitType() == UnitType.Factory) {
                					factoryCount += 1;
                				}
                			}
                			if(gc.round() < 350) {
                				if(gc.round()/factoryCount >= 50 && factoryCount < maxFactory) {
                					WorkerTask tempTask = new WorkerTask(garrID, "printFactory");
                					workerTask.put(garrID,tempTask);
                				}
                			}
                		}
                	}
                }
                
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
   
   public static int countKarbAdj(int xPos, int yPos)
   {
      int count = 0;
      for(int x = Math.max(0, xPos-1); x < Math.min(eWidth-1, xPos+1); x++)
         for(int y = Math.max(0, yPos-1); y < Math.min(eHeight-1, yPos+1); y++)
            count+=karbDep[x][y];
      return count;
   }
   
   public static PriorityQueue<KarbAdjacent> countMaxKarbAdj(MapLocation p)
   {
      boolean[][] used = new boolean[eWidth][eHeight];
      PriorityQueue<KarbAdjacent> karbAdjOrder = new PriorityQueue<KarbAdjacent>();
      LinkedList<MapLocation> q = new LinkedList<MapLocation>();
      used[p.getX()][p.getY()] = true;
      q.add(p);
      while(!q.isEmpty())
      {
         MapLocation id = q.remove();
         karbAdjOrder.add(new KarbAdjacent(id, countKarbAdj(id.getX(), id.getY())));
         for(Direction d: directions)
            try{
               MapLocation next = eMapLoc[id.getX()][id.getY()].add(d);   
               if(inBounds(next)&&!used[next.getX()][next.getY()])
               {
                  used[next.getX()][next.getY()] = true;
                  q.add(next);
               }
            } catch(Exception e) {e.printStackTrace();}
      }
      return karbAdjOrder;
   }
   
   public static Path findPath(MapLocation start, MapLocation end)
   {
      if(paths[start.getX()][start.getY()][end.getX()][end.getY()]!=null)
         return paths[start.getX()][start.getY()][end.getX()][end.getY()];
      int[][] dist = new int[eWidth][eHeight];
      MapLocation[][] prev = new MapLocation[eWidth][eHeight];
      for(int x = 0; x < dist.length; x++)
         Arrays.fill(dist[x], -1);
      dist[start.getX()][start.getY()] = 0;
      LinkedList<MapLocation> q = new LinkedList<MapLocation>();
      q.add(start);
      MapLocation id = null;
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
         {
            try
            {
               MapLocation next = eMapLoc[id.getX()][id.getY()].add(d);
               //System.out.println(d+" "+next+" : "+inBounds(next)+" "+avoid(next, avoid)+" ("+next.getX()+", "+next.getY()+")");
               if(inBounds(next) && dist[next.getX()][next.getY()]==-1)
               {
                  dist[next.getX()][next.getY()] = dist[id.getX()][id.getY()]+1;
                  prev[next.getX()][next.getY()] = id;
                  q.add(next);
               }
            }catch(Exception e) {e.printStackTrace();}
         }
      }   
      if(!pathFound)
         return null;
      Path p = new Path(start, end);
      while(prev[id.getX()][id.getY()]!=null)
      {
         p.seq.add(eMapLoc[prev[id.getX()][id.getY()].getX()][prev[id.getX()][id.getY()].getY()].directionTo(eMapLoc[id.getX()][id.getY()]));
         id = prev[id.getX()][id.getY()];
      }
      return p;
   }
   
   public static boolean inBounds(MapLocation id)
   {
      return id.getX()>=0&&id.getX()<eWidth&&id.getY()>=0&&id.getY()<eHeight&&passable[id.getX()][id.getY()];
   }
   
   static class Path
   {
      MapLocation start;
      MapLocation end;
      LinkedList<Direction> seq = new LinkedList<Direction>();
      public Path(MapLocation s, MapLocation e)
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
      MapLocation loc;
      int dep;
   
      public KarbAdjacent(MapLocation p, int d) {
         loc = p;
         dep = d;
      }
      // greatest goes first
      public int compareTo(KarbAdjacent x) {
         return x.dep-dep;
      }
   }
   
   static class WorkerTask {
	   // Moving Variables
	   MapLocation target;
	   Path dirs;
	   
	   // Building Stuff
	   MapLocation build;
	   int blueprint_id;
	   
	   // Mining Stuff
	   Direction mine;
	   
	   // Repair Stuff
	   MapLocation repair;
	   int repair_id;
	   
	   // General Stuff
	   int unitID;
	   String type;
	   
	   public WorkerTask(int id, String t) {
		   unitID = id;
		   type = t;
	   }
	   
	   public int hashCode() {
		   return unitID;
	   }
	   
	   
   }
}

