import bc.*;
import java.util.*;
import java.io.*;
@SuppressWarnings("unchecked")
public class Player {

   //Basic Info 
   static PrintWriter out;
   static int maxRound = 1000;   // the number of rounds
   static Planet earth = Planet.Earth;
   static Planet mars = Planet.Mars;
   static int rangerDamage = -1;
   static int knightDefense = -1;
   static int healerHeal = -1;
   static Direction[] directions = Direction.values();
   static Direction[] cardinals = {Direction.North, Direction.East, Direction.South, Direction.West};
   static Direction[] diagonals = {Direction.Northeast, Direction.Southeast, Direction.Northwest, Direction.Southwest};
   static Direction[] adjacent = {Direction.North, Direction.East, Direction.South, Direction.West, Direction.Northeast, Direction.Southeast, Direction.Northwest, Direction.Southwest};
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
   static boolean rocketResearched = false;
   static boolean[][] occupied = new boolean[eWidth][eHeight];
   static boolean[][] launchLoc = new boolean[mWidth][mHeight];
   static int[][][][] moveDist = new int[eWidth][eHeight][eWidth][eHeight];
   static int[][] adjCount = new int[eWidth][eHeight];
   
   //Unit Info
   static HashSet<Integer> myInit = new HashSet<Integer>();
   static HashSet<Integer> myUnits;
   static HashSet<Integer> healers;
   static HashSet<Integer> factories;
   static HashSet<Integer> knights;
   static HashSet<Integer> mages;
   static HashSet<Integer> rangers;
   static HashSet<Integer> rockets;
   static HashSet<Integer> workers;
   static int builderCount = 0;
   static int minerCount = 0;
   static HashMap<Integer, Task> tasks = new HashMap<Integer, Task>();
   static HashMap<Integer, Pair> enemyInit = new HashMap<Integer, Pair>();
   static TreeSet<Integer> enemyUnits = new TreeSet<Integer>();
   static HashSet<Pair> attackList = new HashSet<Pair>();
   
   // Map Info
   static MapLocation[][] eMapLoc = new MapLocation[eWidth][eHeight];
   static MapLocation[][] mMapLoc = new MapLocation[mWidth][mHeight];
   static int[][] karbDep = new int[eWidth][eHeight];//amount of karbonite in the square
   static int[][] karbAdj = new int[eWidth][eHeight];//sum of karbonite on and adjacent to the square
   static int[][] regions = new int[eWidth][eHeight];
   static int regionCount = 0;
   static boolean[][] usedMine = new boolean[eWidth][eHeight];
   static boolean[][] viableSite = new boolean[eWidth][eHeight];
   static boolean[] open = new boolean[512];
   static int[][] siteID = new int[eWidth][eHeight];
      
   static {
      try {
         System.out.println("first random is "+random.nextDouble());
      
      //log file
         out = new PrintWriter(new File(myTeam+"_"+gc.planet()+"log.txt"));
      
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
                  karbDep[x][y] = (int) eMap.initialKarboniteAt(eMapLoc[x][y]);
               } 
               catch(Exception e) {e.printStackTrace();}
         for(int x = 0; x < mWidth; x++)
            for(int y = 0; y < mHeight; y++) 
               mMapLoc[x][y] = new MapLocation(mars,x,y);
         System.out.println("Finished initializing basic map info");
         
      //initializes open
         cycle:for(int x = 0; x < 1<<9; x++)
         {
            boolean[] array = new boolean[9];
            int i = -1;
            for(int y = 0; y < 9; y++)
            {
               if((x%(1<<(y+1)))/(1<<y)==1)
               {
                  if(y!=4)
                  {
                     i = y;
                     array[y] = true;
                  }
               }
               else if(y==4)
                  continue cycle;
            }
            if(i==-1)
            {
               open[x] = true;
               continue cycle;
            }
            LinkedList<Pair> q = new LinkedList<Pair>();
            q.add(new Pair(i/3, i%3));
            array[i] = false;
            while(!q.isEmpty())
            {
               Pair p = q.remove();
               for(Direction d: directions)
               {
                  Pair next = mapPair(eMapLoc[p.x][p.y].add(d));
                  if(next.x>=0&&next.x<3&&next.y>=0&&next.y<3)
                     if(array[next.x*3+next.y])
                     {
                        array[next.x*3+next.y] = false;
                        q.add(next);
                     }
               }
            }
            for(int n = 0; n < 9; n++)
               if(array[n])
                  continue cycle;
            open[x] = true;
         }
         
      //initializes teams
         if(myTeam==Team.Red)
            enemyTeam = Team.Blue;
         else
            enemyTeam = Team.Red;
            
      //update units
         updateUnits();
               
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
            
      //initializes karbAdj
         System.out.println("Finding karbonite sums");
         for(int x = 0; x < eWidth; x++)
            for(int y = 0; y < eHeight; y++)
               try {
                  karbAdj[x][y] = countKarbAdj(new Pair(x, y), false);
               } 
               catch(Exception e) {e.printStackTrace();}
         System.out.println("Finished finding karbonite sums");
            
      //initializes tasks
         System.out.println("Initializing tasks");
         for(int x: myInit)
            try {
               tasks.put(x, new Task(x));
            } 
            catch(Exception e) {e.printStackTrace();}
         System.out.println("Finished initializing tasks");     
         
      //initializes regions
         System.out.println("Finding regions");
         HashSet<Integer> unreachable = new HashSet<Integer>();
         int count = 1;
         for(int x = 0; x < eWidth; x++)
            cycle: for(int y = 0; y < eHeight; y++)
               try {
                  if(regions[x][y]!=0||eMap.isPassableTerrainAt(eMapLoc[x][y])==0)
                     continue cycle;
                  regionCount++;
                  floodRegion(new Pair(x, y), regionCount);
                  for(int id: myUnits)
                     if(regions[unitPair(id).x][unitPair(id).y]==regions[x][y])
                        continue cycle;
                  unreachable.add(regions[x][y]);
               } 
               catch(Exception e) {e.printStackTrace();}
         for(int x = 0; x < eWidth; x++)
            for(int y = 0; y < eHeight; y++)
               if(unreachable.contains(regions[x][y]))
                  regions[x][y] = -1*regions[x][y];
         System.out.println("Finished finding regions");
         
      //initializes adjCount
         System.out.println("Finding adjacent counts");
         for(int x = 0; x < eWidth; x++)
            for(int y = 0; y < eHeight; y++)
               for(Direction d: adjacent)
               {
                  Pair p = mapPair(eMapLoc[x][y].add(d));
                  if(inBounds(p)&&regions[p.x][p.y]!=0)
                     adjCount[p.x][p.y]++;
               }
         
      //initializes moveDist
         System.out.println("Finding moveDist");
         for(int x = 0; x < eWidth; x++)
            for(int y = 0; y < eHeight; y++)
               moveDist[x][y] = initPath(new Pair(x, y));
               
      //finds viable sites
         System.out.println("Finding viable sites");
         for(int x = 0; x < eWidth; x++)
            for(int y = 0; y < eHeight; y++)
            {
               int index = 0;
               for(int a = -1; a <= 1; a++)
                  for(int b = -1; b <= 1; b++)
                     if(inBounds(x+a, y+b)&&regions[x+a][y+b]!=0)
                        index+=(1<<((a+1)*3+b+1));
               viableSite[x][y] = open[index];
            }
         
         int c = 0;
         for(int id: myUnits)
            if(c++%2==0)
               tasks.get(id).taskType = 1; 
            else
               tasks.get(id).taskType = 0;    
      //end of initialization
      } 
      catch(Exception e) {e.printStackTrace();}
   }
   public static void main(String[] args) {
      if(gc.planet().equals(Planet.Earth))
         earth();
      else
         mars();
      out.close();
   }
   public static void earth() {
      //rounds 1-1000
      while(gc.round()<=maxRound)
      {
         try
         {
            System.out.println("Earth Round "+gc.round()+": ");
            //System.out.println("Time left: "+gc.getTimeLeftMs());
            updateUnits();
                     
            for(int id: workers)
            {
               Pair start = unitPair(id);
               int task = tasks.get(id).getTask();
               if(task==-1)
               {
                  int type = tasks.get(id).taskType;
                  if(type==1)
                     tasks.get(id).startBuilding(bestSite(unitPair(id)), UnitType.Factory);
               }
               int val = tasks.get(id).doTask();
            }
            for(int id: factories)
            {
               if(tasks.get(id).getTask()==-1)
                  tasks.get(id).startProducing(UnitType.Ranger);
               tasks.get(id).doTask();
            }
            rangerTask();
            for(int id: rockets)
            {
               Unit u = gc.unit(id);
               if(u.health()<u.maxHealth())
                  if(u.structureGarrison().size()>0)
                     if(u.structureIsBuilt()!=0)
                        launch(id);
               if(u.structureGarrison().size()==8)
                  launch(id);
            }
         } 
         catch(Exception e) {e.printStackTrace();} 
         gc.nextTurn();
      }
         //End of match
   }
   public static void mars() {
      // Do Something
      while(gc.round()<=maxRound)
      {
         System.out.println("Mars Round: "+gc.round());
         //System.out.println("Time left: "+gc.getTimeLeftMs());
         gc.nextTurn();
      }
   }
   
   // UNIT METHODS
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
   //worker id, structure id, -2 means on top of site, -1 means too far away, 0 means build success, 1 means structure is finished
   public static int build(int id, Pair target, UnitType ut) 
   {
      Pair p = unitPair(id);
      //System.out.println("Unit at "+p+" is trying to build at "+target+" which has "+siteID[target.x][target.y]);
      if(p.equals(target))
         return -2;
      if(siteID[target.x][target.y]==-1)
      {
         System.out.println("build error");
         return -11;
      }
      if(siteID[target.x][target.y]==-2)
         return blueprint(id, target, ut);
      int sID = siteID[target.x][target.y];
      if(gc.unit(sID).structureIsBuilt()!=0)
         return 1;
      if(gc.canBuild(id, sID))
         gc.build(id, sID);
      else
         return -1;
      if(gc.unit(sID).structureIsBuilt()!=0)
         return 1;
      return 0;
   }
   //returns the id of the blueprinted structure, -5 if too far away, -4 if not enough resources, -3 if being blocked
   public static int blueprint(int id, Pair target, UnitType ut)
   {
      Pair p = unitPair(id);
      //System.out.println("Unit at "+p+" is trying to blueprint at "+target);
      if(!eMapLoc[p.x][p.y].isAdjacentTo(eMapLoc[target.x][target.y]))
         return -5;
      Direction d = findDirection(p, target);
      if(gc.canBlueprint(id, ut, d))
      {
         gc.blueprint(id, ut, d);
         int blueID = gc.senseUnitAtLocation(gc.unit(id).location().mapLocation().add(d)).id();
         tasks.put(blueID, new Task(blueID));
         tasks.get(blueID).taskType = 2;
         siteID[target.x][target.y] = blueID;
         return 0;
      }
      if(gc.karbonite()<100)
         return -4;
      else
         return -3;
   }
   //-2 means too much heat, -1 means no karbonite in adjacent squares, 0 means successfully mined
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
         }
         catch(Exception e) {e.printStackTrace();}
      if(max==0)
         return -1;
      if(!gc.canHarvest(id, best))
         return -2;
      gc.harvest(u.id(), best);
      return 0;
   }
   public static int move(int id)
   {
      Unit u = gc.unit(id);
      Pair start = unitPair(id);
      Pair target = tasks.get(id).moveTarget;
      if(target==null||!gc.isMoveReady(id))
         return -1;
      System.out.println("moving from "+start+" to "+target);
      HashSet<Direction> possible = nextMove(start, target, tasks.get(id).targetDist);
      if(possible==null)
         return -1;
      for(Direction d: possible)
         if(gc.canMove(id, d))
         {
            gc.moveRobot(id, d);
            break;
         }
      return 0;
   }
   public static void updateUnits() throws Exception
   {
      for(int x = 0; x < eWidth; x++)
         for(int y = 0; y < eHeight; y++)
            if(gc.canSenseLocation(eMapLoc[x][y]))
            {
               karbDep[x][y] = (int) gc.karboniteAt(eMapLoc[x][y]);
            }
            else
               karbDep[x][y] = Math.min(karbDep[x][y], (int) eMap.initialKarboniteAt(eMapLoc[x][y]));
   
      if(gc.researchInfo().getLevel(UnitType.Rocket)>0)
         rocketResearched = true;
      occupied = new boolean[eWidth][eHeight];
      myUnits = toIDList(gc.myUnits());
      healers = new HashSet<Integer>();
      factories = new HashSet<Integer>();
      knights = new HashSet<Integer>();
      mages = new HashSet<Integer>();
      rangers = new HashSet<Integer>();
      rockets = new HashSet<Integer>();
      workers = new HashSet<Integer>();
      for(int x = 0; x < eWidth; x++)
         for(int y = 0; y < eHeight; y++)
            siteID[x][y] = -1;
      for(int id: myUnits)
      {
         if(!inGarrison(id))
         {
            Pair p = unitPair(id);
            occupied[p.x][p.y] = true;
         }
         UnitType ut = gc.unit(id).unitType();
         if(ut.equals(UnitType.Factory))
         {
            factories.add(id);
            siteID[unitPair(id).x][unitPair(id).y] = id;
         }
         else if(ut.equals(UnitType.Healer))
         {
            healers.add(id);
         }
         else if(ut.equals(UnitType.Knight))
            knights.add(id);
         else if(ut.equals(UnitType.Mage))
            mages.add(id);
         else if(ut.equals(UnitType.Ranger))
         {
            rangers.add(id);
            rangerDamage = gc.unit(id).damage();
         }
         else if(ut.equals(UnitType.Rocket))
         {
            rockets.add(id);
            siteID[unitPair(id).x][unitPair(id).y] = id;
         }
         else if(ut.equals(UnitType.Worker))
         {
            workers.add(id);
            try{
               if(gc.round()>1)
               {
                  if(tasks.get(id).moveTarget!=null)
                     siteID[tasks.get(id).moveTarget.x][tasks.get(id).moveTarget.y]=-2;
                  if(tasks.get(id).taskType==0)
                     minerCount++;
                  else
                     builderCount++;
               }
            } catch(Exception e) {
               System.out.println("unit "+id+" broke");
               e.printStackTrace();
            }
         }
         else
            throw new Exception("Error, Unknown unit type: "+ut);
      }
      enemyUnits.clear();
      for(int id: myUnits)
         if(!gc.unit(id).location().isInGarrison())
         {
            VecUnit vu = gc.senseNearbyUnitsByTeam(gc.unit(id).location().mapLocation(), 5000, enemyTeam);
            for(int x = 0; x < vu.size(); x++)
               enemyUnits.add(vu.get(x).id());
            break;
         }
      attackList.clear();
      for(int id: enemyUnits)
      {
         attackList.add(unitPair(id));
         if(gc.unit(id).unitType().equals(UnitType.Knight))
            knightDefense = (int)gc.unit(id).knightDefense();
      }
      if(attackList.size()==0)
         for(int id: enemyInit.keySet())
            attackList.add(enemyInit.get(id));
      for(Pair p: attackList)
         System.out.println("enemy is at "+p);
   }
   //END OF UNIT METHODS
   
   //MAP METHODS
   public static void launch(int id)
   {
      for(int x = 0; x < mWidth; x++)
         for(int y = 0; y < mHeight; y++)
            if(!launchLoc[x][y])
               if(mMap.isPassableTerrainAt(mMapLoc[x][y])!=0)
               {
                  gc.launchRocket(id, mMapLoc[x][y]);
               }
   }
   public static HashSet<Direction> nextMove(Pair start, Pair end, int goal)
   {
      if(end==null||moveDist[start.x][start.y][end.x][end.y]==goal)
         return null;
      int curDist = moveDist[start.x][start.y][end.x][end.y];
      int direction = 1;
      if(goal<moveDist[start.x][start.y][end.x][end.y])
         direction = -1;
         
      if(moveDist[start.x][start.y][end.x][end.y]==-1)
         if(curDist>goal)
         {
            System.out.println("Cannot go from "+start+" to "+end);
            return null;
         }
         else
            direction = -1;
            
      HashSet<Direction> possible = new HashSet<Direction>();
      for(Direction d: adjacent)
      {
         Pair next = mapPair(eMapLoc[start.x][start.y].add(d));
         if(inBounds(next)&&moveDist[next.x][next.y][end.x][end.y]==curDist+direction)
            possible.add(d);
      }
      return possible;
   }
   public static int[][] initPath(Pair start)
   {
      HashSet<Direction>[][] moves = new HashSet[eWidth][eHeight];
      for(int x = 0; x < eWidth; x++)
         for(int y = 0; y < eHeight; y++)
         {
            moves[x][y] = new HashSet<Direction>();
            if(regions[x][y]!=0)
               for(Direction d: adjacent)
               {
                  Pair next = mapPair(eMapLoc[x][y].add(d));
                  if(inBounds(next)&&regions[next.x][next.y]!=0)
                     moves[x][y].add(d);
               }
         }
   
      int[][] dist = new int[eWidth][eHeight];
      boolean[][] used = new boolean[eWidth][eHeight];
      for(int x = 0; x < eWidth; x++)
         for(int y = 0; y < eHeight; y++)
            dist[x][y] = -1;
      if(regions[start.x][start.y]==0)
         return dist;
      dist[start.x][start.y] = 0;
      used[start.x][start.y] = true;
      LinkedList<Pair> q = new LinkedList<Pair>();
      q.add(start);
      cycle: while(!q.isEmpty())
      {
         Pair cur = q.remove();
         for(Direction d: moves[cur.x][cur.y])
         {
            Pair next = mapPair(eMapLoc[cur.x][cur.y].add(d));
            dist[next.x][next.y] = dist[cur.x][cur.y]+1;
            q.add(next);
            moves[next.x][next.y].remove(oppositeDirection(d));
         }
         moves[cur.x][cur.y].clear();
      }   
      return dist;
   }
         //floodfills map to assign region numbers
   public static void floodRegion(Pair p, int c)
   {
      int karbCount = 0;
      LinkedList<Pair> q = new LinkedList<Pair>();
      regions[p.x][p.y] = c;
      q.add(p);
      while(!q.isEmpty())
      {
         Pair id = q.remove();
         for(Direction d: directions)
            try{
               Pair next = mapPair(eMapLoc[id.x][id.y].add(d));  
               if(inBounds(next)&&regions[next.x][next.y]==0&&eMap.isPassableTerrainAt(eMapLoc[next.x][next.y])!=0)
               {
                  regions[next.x][next.y] = c; 
                  q.add(next);
               }
            } 
            catch(Exception e) {e.printStackTrace();}
      }
   }
   public static Pair bestSite(Pair p)
   {
      LinkedList<Pair> q = new LinkedList<Pair>();
      boolean[][] used = new boolean[eWidth][eHeight];
      q.add(p);
      while(!q.isEmpty())
      {
         Pair cur = q.remove();
         for(Direction d: directions)
         {
            Pair site = mapPair(eMapLoc[cur.x][cur.y].add(d));
            if(inBounds(site)&&!used[site.x][site.y]&&regions[site.x][site.y]!=0)
            {
               used[site.x][site.y] = true;
               if(siteID[site.x][site.y]==-1)
                  if(viableSite[site.x][site.y])
                     if(!occupied[site.x][site.y])
                        return site;
               q.add(site);
            }
         }
      }
      return null;
   }
   public static HashSet<Pair> locWithin(Pair target, int minRange, int maxRange)
   {
      VecMapLocation minVec = gc.allLocationsWithin(eMapLoc[target.x][target.y], minRange);
      VecMapLocation maxVec = gc.allLocationsWithin(eMapLoc[target.x][target.y], maxRange);
      HashSet<Pair> ret = new HashSet<Pair>();
      for(int x = 0; x < maxVec.size(); x++)
         ret.add(mapPair(maxVec.get(x)));
      for(int y = 0; y < minVec.size(); y++)
         ret.remove(mapPair(minVec.get(y)));
      return ret;
   }
   public static Pair closestEnemy(Pair start, int minRange, int maxRange)
   {
      Pair best = null;
      int bestScore = 100000;
      for(Pair en: attackList)
      {
         HashSet<Pair> possible = locWithin(en, minRange, maxRange);
         int min = Integer.MAX_VALUE;
         for(Pair end: possible)
            if(moveDist[start.x][start.y][end.x][end.y]!=-1)
            {
               min = Math.min(min, moveDist[start.x][start.y][end.x][end.y]);
            }
         if(min<bestScore)
         {
            best = en;
            bestScore = min;
         }
      }
      return best;
   }
   public static void rangerTask()
   {
      HashSet<Integer> killed = new HashSet<Integer>();
      if(enemyUnits!=null)
      {
         for(int en: enemyUnits)
         {
            HashSet<Integer> canAttack = new HashSet<Integer>();
            for(int id: rangers)
            {
               if(gc.unit(id).location().isInGarrison())
                  continue;
               if(gc.isAttackReady(id))
                  canAttack.add(id);
            }
            Pair enLoc = unitPair(en);
            int count = 0;
            int health = (int) gc.unit(en).health();
            int def = 0;
            if(gc.unit(en).unitType().equals(UnitType.Knight))
               def = knightDefense;
            HashSet<Integer> attackers = new HashSet<Integer>();
            HashMap<Integer, Direction> movers = new HashMap<Integer, Direction>();
            for(int id: canAttack)
            {
               Pair loc = unitPair(id);
               if(gc.canAttack(id, en))
                  attackers.add(id);
               else
               {
                  for(Direction d: adjacent)
                     if(gc.canMove(id, d)&&gc.isMoveReady(id))
                     {
                        int diff = (int)eMapLoc[loc.x][loc.y].add(d).distanceSquaredTo(eMapLoc[enLoc.x][enLoc.y]);
                        if(diff<=gc.unit(id).attackRange()&&diff>gc.unit(id).rangerCannotAttackRange())
                        {
                           movers.put(id, d);
                           break;
                        }
                     }
               }
            }
            if((attackers.size()+movers.size())*(rangerDamage-def)>=health)
            {
               boolean hasKilled = false;
               for(int at: attackers)
                  if(gc.canSenseUnit(en))
                     gc.attack(at, en);
                  else
                  {
                     System.out.println("Killed enemy unit at "+enLoc);
                     break;
                  }
               for(int mo: movers.keySet())
                  if(gc.canSenseUnit(en))
                     if(gc.canMove(mo, movers.get(mo)))
                     {
                        gc.moveRobot(mo, movers.get(mo));
                        gc.attack(mo, en);
                     }
                     else
                     {
                        System.out.println("Killed enemy unit at "+enLoc);
                        break;
                     }         
            }
         }
      }
      for(int id: rangers)
      {
         if(gc.unit(id).location().isInGarrison())
            continue;
         Pair cloEn = closestEnemy(unitPair(id), (int)gc.unit(id).rangerCannotAttackRange(), (int)gc.unit(id).attackRange());
         if(enemyUnits.size()==0)
            tasks.get(id).startMoving(cloEn, 0);
         else
            tasks.get(id).startMoving(cloEn, 4);
         System.out.println("ranger at "+unitPair(id)+" going to "+tasks.get(id).moveTarget);
         move(id);
      }
      for(int id: rangers)
         if(!gc.unit(id).location().isInGarrison()&&gc.isAttackReady(id))
         {
            VecUnit vu = gc.senseNearbyUnitsByTeam(eMapLoc[unitPair(id).x][unitPair(id).y], gc.unit(id).attackRange(), enemyTeam);
            TreeSet<Enemy> leftovers = new TreeSet<Enemy>();
            for(int x = 0; x < vu.size(); x++)
               leftovers.add(new Enemy(vu.get(x)));
            for(Enemy e: leftovers)
               if(gc.canAttack(id, e.unit.id()))
               {
                  gc.attack(id, e.unit.id());
                  break;
               }
         }
   }
   //END OF MAP METHODS
   
   //KARBONITE-RELATED METHODS
   //floodfills region and returns a PriorityQueue containing locations with highest karbAdj value
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
         if(karbAdj[id.x][id.y]==0)
            continue;
         karbAdjOrder.add(new KarbAdjacent(id, karbAdj[id.x][id.y]));
         for(Direction d: directions)
            try{
               Pair next = mapPair(eMapLoc[id.x][id.y].add(d));   
               if(inBounds(next)&&!used[next.x][next.y]&&regions[next.x][next.y]!=0)
               {
                  used[next.x][next.y] = true;
                  q.add(next);
               }
            } 
            catch(Exception e) {e.printStackTrace();}
      }
      return karbAdjOrder;
   }
   public static int countKarbAdj(Pair p, boolean useCur)
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
   //return best mine given known current conditions
   public static Pair bestMine(int id) throws Exception
   {
      Pair start = unitPair(id);
      LinkedList<Pair> q = new LinkedList<Pair>();
      boolean[][] used = new boolean[eWidth][eHeight];
      q.add(start);
      used[start.x][start.y] = true;
      while(!q.isEmpty())
      {
         Pair cur = q.remove();
         if(karbDep[cur.x][cur.y]>0&&!usedMine[cur.x][cur.y]&&siteID[cur.x][cur.y]==-1)
            return cur;
         for(Direction d: adjacent)
         {
            Pair next = mapPair(eMapLoc[start.x][start.y].add(d));
            if(inBounds(next)&&!used[next.x][next.y])
            {
               q.add(next);
               used[next.x][next.y] = true;
            }
         }
      }
      return null;
   }
   //END OF KARBONITE-RELATED METHODS
   
   //HELPER METHODS
   public static boolean safeFrom(int id, Pair start)
   {
      UnitType ut = gc.unit(id).unitType();
      if(ut.equals(UnitType.Healer))
         return false;
      if(ut.equals(UnitType.Rocket))
         return false;
      if(ut.equals(UnitType.Factory))
         return false;
      if(ut.equals(UnitType.Worker))
         return false;
      HashSet<Pair> set = new HashSet<Pair>();
      int range = (int) gc.unit(id).attackRange();
      int minRange = 0;
      double cooldown = (double)gc.unit(id).movementCooldown();
      if(ut.equals(UnitType.Ranger))
         minRange = (int) gc.unit(id).rangerCannotAttackRange();
      set = locWithin(unitPair(id), minRange, range);
      for(Pair p: set)
         if(moveDist[p.x][p.y][start.x][start.y]!=-1&&moveDist[p.x][p.y][start.x][start.y]*cooldown<25)
            return false;
      return true;
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
   public static int distance(Pair p1, Pair p2)
   {
      return (p1.x-p2.x)*(p1.x-p2.x)+(p1.y-p2.y)*(p1.y-p2.y);
   }
   public static boolean isStructure(int id)
   {
      return gc.unit(id).unitType().equals(UnitType.Factory)||gc.unit(id).unitType().equals(UnitType.Rocket);   
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
   public static boolean isEmpty(Pair id)
   {
      return gc.hasUnitAtLocation(eMapLoc[id.x][id.y])&&regions[id.x][id.y]!=0;
   }
   public static boolean inBounds(Pair id)
   {
      return id.x>=0&&id.x<eWidth&&id.y>=0&&id.y<eHeight;
   }
   public static boolean inBounds(int x, int y)
   {
      return x>=0&&x<eWidth&&y>=0&&y<eHeight;
   }
   public static boolean avoid(Pair id, ArrayList<Pair> avoid)
   {
      for(Pair a: avoid)
         if(a.equals(id))
            return false;
      return true;
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
   public static Pair closestRocket(int id)
   {
      Pair start = unitPair(id);
      int bestPathSize = 0;
      Pair bestPair = null;
      for(int ro: rockets)
      {
         Pair target = unitPair(ro);
         int pathSize = moveDist[start.x][start.y][target.x][target.y];
         if(pathSize==-1)
            continue;
         if(bestPair==null||pathSize<bestPathSize)
         {
            bestPathSize = pathSize;
            bestPair = target;
         }
      }
      return bestPair;
   }
   //END OF HELPER METHODS
   
   //ADDITIONAL CLASSES
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
      public Path reverse()
      {
         Path p = new Path(end, start);
         for(Direction d: seq)
            p.seq.addLast(oppositeDirection(d));
         return p;
      }
      public String toString()
      {
         if(seq.size()==0)
            return "[]";
         String s = "[";
         for(Direction d: seq)
            s+=d+", ";
         s = s.substring(0, s.length()-2)+"]";
         return s;
      }
   }
   static class KarbAdjacent implements Comparable<KarbAdjacent> {
      Pair loc;
      int size;
   
      public KarbAdjacent(Pair p, int d) {
         loc = p;
         size = d;
      }
   //greatest goes first
      public int compareTo(KarbAdjacent x) {
         return x.size-size;
      }
      public String toString()
      {
         return "["+loc.toString()+" "+size+"]";
      }
   }
   static class Task
   {
      int taskType;
      int[] typeOn = new int[10]; //lower value is higher priority, 0 means no priority
      int unitID;
      int maxStep = 1;
   //task-specific data (if task is off, then value is null, false, -1, etc)
      int targetDist = 0;
      Pair moveTarget = null; //for moving: final destination
      UnitType produceType = null; //for producing: type of robot
      UnitType buildType = null;
      boolean halting = false;
      public Task(int id)
      {
         unitID = id;
      }
      public String toString()
      {
         String str = "Unit ID: ";
         str+=unitID;
         str+="\n Target: ";
         str+=moveTarget;
         return str;
      }
      public boolean startMoving(Pair target, int goal)
      {
         if(target==null)
            return false;
         moveTarget = target;
         targetDist = goal;
         return true;
      }
      public boolean startMining(Pair target) throws Exception 
      {
         if(target==null)
         {
            System.out.println(unitPair(unitID)+" to "+target+" is null");
         }
         typeOn[0] = maxStep++;
         startMoving(target, 0);
         usedMine[target.x][target.y] = true;
         return true;
      }
      public boolean startBuilding(Pair site, UnitType ut) throws Exception
      {
         if(gc.round()>325&&Math.random()<0.2)
            ut = UnitType.Rocket;
         Pair cur = unitPair(unitID);
         //System.out.println("The unit at "+unitPair(unitID)+" is going to build at "+site);
         typeOn[1] = maxStep++;
         buildType = ut;
         if(isCardinal(cur, site))
            return true;
         startMoving(site, 1);
         return true;
      }
      public boolean startProducing(UnitType type)
      {
         typeOn[2] = maxStep++;
         produceType = type;
         return true;
      }
      public boolean startLoading()
      {
         Pair closest = closestRocket(unitID);
         if(closest==null)
            return false;
         startMoving(closest, 1);
         typeOn[6] = maxStep++;
         return true;
      }
      //return task number
      public int getTask()
      {
         int index = -1;
         for(int x = 0; x < typeOn.length; x++)
            if(typeOn[x]>0)
               if(index==-1||typeOn[x]>typeOn[index])
                  index = x;
         return index;
      }
      // if unit is a factory, return 0 if unloaded a unit, otherwise return -1
      // return -1*(tasktype+1) if finished with the task
      /* 
      0: mining
      1: building
      2: producing
      3: blueprint
      4: attack
      5: heal
      6: load
      */
      public int doTask() throws Exception
      {
         Pair start = unitPair(unitID);
         if(moveTarget!=null)
            move(unitID);
         int ret = 0;
         int status;
         switch(getTask()) {
            case 0:
               moveTarget = bestMine(unitID);
               if(moveTarget==null)
               {}
               System.out.println("worker at "+start+" is going to mine at "+moveTarget);
               status = mine(unitID);
               break;
            case 1:
               if(moveTarget==null)
                  moveTarget = bestSite(start);
               if(siteID[moveTarget.x][moveTarget.y]==-1)
                  siteID[moveTarget.x][moveTarget.y] = -2;
               status = build(unitID, moveTarget, buildType);
               if(status==-2)
                  if(gc.isMoveReady(unitID))
                     for(Direction d: directions)
                        if(!d.equals(Direction.Center))
                           if(gc.canMove(unitID, d))
                           {
                              gc.moveRobot(unitID, d);
                              status = blueprint(unitID, moveTarget, buildType);
                              break;
                           }
               for(int factID: factories)
                  if(gc.canRepair(unitID, factID))
                     gc.repair(unitID, factID);
               break;
            case 2:
               produce(unitID, produceType);
               boolean working = true;
               int unloadCount = 0;
               if(gc.unit(unitID).structureGarrison().size()>0)
                  cycle: while(working)
                  {
                     working = false;
                     for(Direction d: cardinals)
                        if(gc.canUnload(unitID, d))
                        {
                           gc.unload(unitID, d);
                           int produceID = gc.senseUnitAtLocation(gc.unit(unitID).location().mapLocation().add(d)).id();
                           tasks.put(produceID, new Task(produceID));
                           if(gc.unit(produceID).unitType().equals(UnitType.Healer))
                              tasks.get(produceID).taskType = 5;
                           else
                              tasks.get(produceID).taskType = 4;
                           working = true;
                           unloadCount++;
                           System.out.println("Unloading unit: "+produceID);
                           continue cycle;
                        }
                  }
               if(unloadCount>0)
                  ret = 0;
               else
                  ret = -1;
               break;
            default:
               if(unitType(unitID).equals(UnitType.Worker))
                  mine(unitID);
         }
         return ret;
      }
   }
   static class Site implements Comparable<Site>
   {
      Pair loc;
      HashSet<Integer> builders;
      int dist;
      public Site(Pair p, HashSet<Integer> b, int d)
      {
         loc = p;
         builders = b;
         dist = d;
      }
      public int compareTo(Site s)
      {
         if(s==null)
            return -1;
         if(s.builders.size()!=builders.size())
            return s.builders.size()-builders.size();
         return dist-s.dist;
      }
   }
   static class Enemy implements Comparable<Enemy>
   {
      Unit unit;
      int attackers;
      int movers;
      int score;
      boolean kill;
      public Enemy(Unit u)
      {
         unit = u; 
         UnitType ut = unit.unitType();
         if(ut.equals(UnitType.Mage))
            score+=10000;
         if(ut.equals(UnitType.Factory))
         {
            score+=1000;
            if(unit.structureIsBuilt()!=0)
               score+=100000;
         }
         if(ut.equals(UnitType.Knight))
            score+=1000;
         if(ut.equals(UnitType.Ranger))
            score+=1000;
         if(ut.equals(UnitType.Healer))
            score+=1000;
         if(ut.equals(UnitType.Worker))
            score+=10;
         if(ut.equals(UnitType.Rocket))
         {
            score+=1;
            if(unit.structureIsBuilt()!=0)
               score+=10*gc.round();
         }  
         score-=u.health();
      }
      public Enemy(Unit u, int a, int m, boolean k)
      {
         unit = u;
         attackers = a;
         movers = m;
         kill = k;
         UnitType ut = unit.unitType();
         score = attackers*2+movers;
         if(ut.equals(UnitType.Mage))
         {
            score+=10000;
            if(kill)
               score = Integer.MAX_VALUE/5;
         }
         if(ut.equals(UnitType.Factory))
         {
            score+=1000;
            if(unit.structureIsBuilt()!=0)
               score+=100000;
            if(kill)
               score = Integer.MAX_VALUE/2;
         }
         if(ut.equals(UnitType.Knight))
            score+=1000;
         if(ut.equals(UnitType.Ranger))
            score+=1000;
         if(ut.equals(UnitType.Healer))
         {
            score+=100;
            if(kill)
               score = Integer.MAX_VALUE/4;
         }
         if(ut.equals(UnitType.Worker))
            score+=10;
         if(ut.equals(UnitType.Rocket))
         {
            score+=1;
            if(unit.structureIsBuilt()!=0)
               score+=10*gc.round();
         }    
         if(kill)
            score*=2;  
      }
      public int compareTo(Enemy e)
      {
         return e.score-score;
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
//END OF ADDITIONAL CLASSES
