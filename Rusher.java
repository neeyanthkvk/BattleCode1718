import bc.*;
import java.util.*;
import java.io.*;
@SuppressWarnings("unchecked")
public class Rusher {

   //Basic Info 
   static long startTime = System.currentTimeMillis();
   static PrintWriter out;
   static int maxRound = 1000;   // the number of rounds
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
   static boolean rocketResearched = false;
   static boolean[][] occupied = new boolean[eWidth][eHeight];
   static boolean[][] launchLoc = new boolean[mWidth][mHeight];
   
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
   static HashSet<Integer> enemyUnits;
   static HashSet<Pair> attackList = new HashSet<Pair>();
   
   // Map Info
   static MapLocation[][] eMapLoc = new MapLocation[eWidth][eHeight];
   static MapLocation[][] mMapLoc = new MapLocation[mWidth][mHeight];
   static int[][] karbDep = new int[eWidth][eHeight];//amount of karbonite in the square
   static int[][] karbAdj = new int[eWidth][eHeight];//sum of karbonite on and adjacent to the square
   static Path[][][][] paths = new Path[eWidth][eHeight][eWidth][eHeight];
   static HashMap<Integer, PriorityQueue<KarbAdjacent>> bestKarbAdj = new HashMap<Integer, PriorityQueue<KarbAdjacent>>();
   static int[][] regions = new int[eWidth][eHeight];
   static int regionCount = 0;
   static boolean[][] noKarbonite = new boolean[eWidth][eHeight];
   static boolean[][] usedMine = new boolean[eWidth][eHeight];
   static boolean[][] viableSite = new boolean[eWidth][eHeight];
   static boolean[] open = new boolean[512];
   static int[][] siteID = new int[eWidth][eHeight];
   static int siteParity = -1;
      
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
         
      //finds initial sites
         Site bestSite = null;
         for(int x = 0; x < eWidth; x++)
            for(int y = 0; y < eHeight; y++)
            {
               int index = 0;
               for(int a = -1; a <= 1; a++)
                  for(int b = -1; b <= 1; b++)
                     if(inBounds(x+a, y+b)&&regions[x+a][y+b]!=0)
                        index+=(1<<((a+1)*3+b+1));
               viableSite[x][y] = open[index];
               if(viableSite[x][y])
               {
                  Site s = initSite(new Pair(x, y));
                  if(bestSite==null||bestSite.compareTo(s)>0)
                     bestSite = s;
               }
            }
         siteParity = (bestSite.loc.x+bestSite.loc.y)%2;
         for(int x = 0; x < eWidth; x++)
            for(int y = 0; y < eHeight; y++)
               if((x+y)%2!=siteParity)
                  viableSite[x][y] = false;
         System.out.println("Best site is "+bestSite.loc);
         System.out.println("best site workers are ");
         
      //initializes bestKarbAdj
         System.out.println("Finding best mining location");
         for(int id: myUnits)
            try {
               Pair p = unitPair(id);
               if(bestKarbAdj.get(regions[p.x][p.y])==null)
                  bestKarbAdj.put(regions[p.x][p.y], countMaxKarbAdj(unitPair(id)));
            } 
            catch(Exception e) {e.printStackTrace();}
         System.out.println("Finished finding best mining location");
           
      //assigns tasks
         for(int id: bestSite.init)
         {
            tasks.get(id).taskType = 1;
            tasks.get(id).startBuilding(bestSite.loc, UnitType.Factory);
         }
         
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
            System.out.println("Time used: "+(System.currentTimeMillis()-startTime));
            updateUnits();
                     
            for(int id: myUnits)
            {
               if(gc.unit(id).location().isInGarrison())
                  continue;
               Pair p = unitPair(id);
               if(!noKarbonite[p.x][p.y]&&countKarbAdj(unitPair(id), true)==0)
                  noKarbonite[p.x][p.y] = true;
            }
         
            for(int id: workers)
            {
               int limit = (int)Math.sqrt(eWidth*eHeight)/2;
               if(gc.round()>1&&random.nextDouble()<0.1&&(minerCount<=limit||builderCount<=1.5*limit))
                  for(Direction d: diagonals)
                     if(gc.canReplicate(id, d))
                     {
                        gc.replicate(id, d);
                        int newID = gc.senseUnitAtLocation(gc.unit(id).location().mapLocation().add(d)).id();
                        tasks.put(newID, new Task(newID));
                        //tasks.get(newID).taskType = 1;
                        if(minerCount>2*builderCount)
                        {
                           tasks.get(newID).taskType = 1;
                           builderCount++;
                        }
                        else
                           minerCount++;
                     }
               int task = tasks.get(id).getTask();
               if(task==-1)
               {
                  int type = tasks.get(id).taskType;
                  if(type==0)
                     if(bestMine(id)!=null)
                        tasks.get(id).startMining(bestMine(id).loc);
                     else
                        tasks.get(id).taskType = 1;
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
            for(int id: rangers)
            {
               if(Math.random()<0.1)
                  tasks.get(id).startLoading();
               if(gc.unit(id).location().isInGarrison())
                  continue;
               //System.out.println("ranger "+id);
               if(tasks.get(id).getTask()==-1)
               {
                  tasks.get(id).startAttacking(bestAttack(id));
               }
               tasks.get(id).doTask();
            }
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
         System.out.println("Time used: "+(System.currentTimeMillis()-startTime));
         gc.nextTurn();
      }
   }
   
   // UNIT METHODS
   //-2 means enemy spotted, but attack isn't ready, -1 means no enemy in sight, 0 means attacked enemy
   public static int attack(int id)
   {
      MapLocation curLoc = gc.unit(id).location().mapLocation();
      int range = (int) gc.unit(id).attackRange();
      int minRadius = 0;
      if(gc.unit(id).unitType().equals(UnitType.Ranger))
         minRadius = (int)gc.unit(id).rangerCannotAttackRange();
      HashSet<Integer> enemy = toIDList(gc.senseNearbyUnitsByTeam(curLoc, range, enemyTeam));
      if(enemy.size()==0)
         tasks.get(id).halting = false;
      else
         tasks.get(id).halting = true;      
      if(!gc.isAttackReady(id))
         return -2;
      int bestEnemy = -1;
      int bestDist = 0;
      Unit bestU = null;
      for(int targetEnemy: enemy)
      {
         if(!gc.canAttack(id, targetEnemy))
            continue;
         int dist = distance(unitPair(id), unitPair(targetEnemy));
         Unit u = gc.unit(targetEnemy);
         if(bestEnemy==-1||(bestUnitType(u, bestU)*10000+dist-bestDist)<0)
         {
            bestEnemy = targetEnemy;
            bestDist = dist;
            bestU = u;
         }
      }
      if(bestEnemy==-1)
         return -1;
      gc.attack(id, bestEnemy);
      return 0;
            //HashSet<Integer> enemy = toIDList(gc.senseNearbyUnitsByTeam(gc.unit(id).location().mapLocation(), (int) gc.unit(id).attackRange(), enemyTeam));
            // if(engage&&enemy.size()>0)
               // return -3;
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
      if(gc.karboniteAt(gc.unit(u.id()).location().mapLocation().add(best)) == 0) {
         return -1;      
      }
      gc.harvest(u.id(), best);
      return 0;
   }
   //return -3 means enemy spotted, -2 means path blocked, -1 means too much heat or halting, 0 means move success, 1 means reached target
   public static int move(int id) throws Exception
   {
      if(tasks.get(id).halting)
         return -1;
      Unit u = gc.unit(id);
      Pair prev = unitPair(id);
      Pair target = tasks.get(id).moveTarget;
      if(prev.equals(target)||target==null)
         return 1;
      Path pa = tasks.get(id).path;
      if(pa==null)
         return -2;
      if(gc.isMoveReady(id))
         if(gc.canMove(id, pa.seq.getFirst()))
         {
            gc.moveRobot(id, pa.seq.getFirst());
            Pair p = mapPair(eMapLoc[prev.x][prev.y].add(pa.seq.getFirst()));
            tasks.get(id).path.seq.removeFirst();
            if(tasks.get(id).path.seq.size()==0)
               tasks.get(id).path = null;
            return 0;
         }
         else
         {
            pa = tasks.get(id).path = findPath(prev, target, true);
            if(pa==null)
               return -2;
            if(gc.canMove(id, pa.seq.getFirst()))
            {
               gc.moveRobot(id, pa.seq.getFirst());
               Pair p = mapPair(eMapLoc[prev.x][prev.y].add(pa.seq.getFirst()));
               tasks.get(id).path.seq.removeFirst();
               if(tasks.get(id).path.seq.size()==0)
                  tasks.get(id).path = null;
               return 0;
            }
         }
      else
         return -1;
      return 0;
   }
   public static void updateUnits() throws Exception
   {
      if(gc.round()>325)
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
            healers.add(id);
         else if(ut.equals(UnitType.Knight))
            knights.add(id);
         else if(ut.equals(UnitType.Mage))
            mages.add(id);
         else if(ut.equals(UnitType.Ranger))
            rangers.add(id);
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
                  if(tasks.get(id).buildTarget!=null&&siteID[unitPair(id).x][unitPair(id).y]==-1)
                     siteID[unitPair(id).x][unitPair(id).y]=-2;
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
      for(int id: myUnits)
         if(!gc.unit(id).location().isInGarrison())
         {
            enemyUnits = toIDList(gc.senseNearbyUnitsByTeam(gc.unit(id).location().mapLocation(), 5000, enemyTeam));
            break;
         }
      attackList.clear();
      if(enemyUnits != null)
         for(int id: enemyUnits)
            attackList.add(unitPair(id));
      if(attackList.size()==0)
         for(int id: enemyInit.keySet())
            attackList.add(enemyInit.get(id));
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
   public static Path findPath(Pair start, Pair end, boolean avoid)
   {
      if(start.equals(end))
         return null;
      if(regions[start.x][start.y]!=regions[end.x][end.y])
      {
         System.out.println("Different regions "+start+" to "+end);
         return null;
      }
      if(paths[start.x][start.y][end.x][end.y]!=null&&!avoid)
         return paths[start.x][start.y][end.x][end.y];
      boolean[][] used = new boolean[eWidth][eHeight];
      Pair[][] prev = new Pair[eWidth][eHeight];
      used[start.x][start.y] = true;
      LinkedList<Pair> q = new LinkedList<Pair>();
      q.add(start);
      Pair id = null;
      boolean pathFound = false;
      cycle: while(!q.isEmpty())
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
               Pair next = mapPair(eMapLoc[id.x][id.y].add(d));
               if(inBounds(next))
               {
               // if(avoid!=null)
                  //System.out.println(id+" "+next+" : "+used[next.x][next.y]+" "+regions[next.x][next.y]);
                  if(!used[next.x][next.y]&&regions[next.x][next.y]!=0)
                     if(siteID[next.x][next.y]==-1)
                        if(!occupied[next.x][next.y]||!avoid)
                        {
                           prev[next.x][next.y] = id;
                           q.add(next);
                        }
                  used[next.x][next.y] = true;
               }
            }
            catch(Exception e) {e.printStackTrace();}
         }
      }   
      if(!pathFound)
         return null;
      Path p = new Path(start, end);
      while(prev[id.x][id.y]!=null)
      {
         p.seq.addFirst(findDirection(prev[id.x][id.y], id));
         id = prev[id.x][id.y];
      }
      if(!avoid)
         paths[start.x][start.y][end.x][end.y] = p;
      return p;
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
   public static Site initSite(Pair site) throws Exception
   {
      int initCount = 0;
      int totalDist = 0;
      HashSet<Integer> initBuild = toIDList(gc.senseNearbyUnitsByTeam(eMapLoc[site.x][site.y], 20, myTeam));
      LinkedList<Integer> toRemove = new LinkedList<Integer>();
      for(int id: initBuild)
         if(regions[unitPair(id).x][unitPair(id).y]!=regions[site.x][site.y])
            toRemove.add(id);
      for(int r: toRemove)
         initBuild.remove(r);
      HashSet<Integer> ret = new HashSet<Integer>();
      int minDist = -1;
      for(int id: initBuild)
      {
         Pair cur = unitPair(id);
         int dist = 0;
         if(!isCardinal(cur, site))
         {
            HashSet<Pair> possible = locWithin(site, 0, 1);
            Pair bestLoc = null;
            int bestDist = 0;
            for(Pair target: possible)
            {
               paths[cur.x][cur.y][target.x][target.y] = findPath(cur, target, true);
               if(paths[cur.x][cur.y][target.x][target.y]==null)
                  continue;
               int d = paths[cur.x][cur.y][target.x][target.y].seq.size();
               if(bestLoc==null||d<bestDist)
               {
                  bestLoc = target;
                  bestDist = d;
               }
            }
            dist = bestDist;
         }
         if(dist<=3)
         {
            totalDist+=dist;
            ret.add(id);
            if(minDist==-1||dist<minDist)
               minDist = dist;
         }
      }
      return new Site(site, ret, totalDist);
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
   public static Pair bestAttack(int id)
   {
      Pair start = unitPair(id);
      int minRange = 0;
      if(gc.unit(id).unitType().equals(UnitType.Ranger))
         minRange = (int) gc.unit(id).rangerCannotAttackRange();
      int maxRange = (int) gc.unit(id).attackRange();
      HashSet<Pair> all = new HashSet<Pair>();
      for(Pair en: attackList)
         all.addAll(locWithin(en, minRange, maxRange));
      if(all.size()==0)
         return null;
      boolean[][] used = new boolean[eWidth][eHeight];
      used[start.x][start.y] = true;
      Pair[][] prev = new Pair[eWidth][eHeight];
      LinkedList<Pair> q = new LinkedList<Pair>();
      q.add(unitPair(id));
      Pair p = null;
      boolean pathFound = false;
      cycle: while(!q.isEmpty())
      {
         p = q.remove();
         if(all.contains(p))
         {
            pathFound = true;
            break;
         }
         for(Direction d: directions)
            try
            {
               Pair next = mapPair(eMapLoc[p.x][p.y].add(d));
               if(inBounds(next))
               {
                  if(!used[next.x][next.y]&&regions[next.x][next.y]!=0)
                     if(siteID[next.x][next.y]==-1)
                        if(!occupied[next.x][next.y])
                        {
                           prev[next.x][next.y] = p;
                           q.add(next);
                        }
                  used[next.x][next.y] = true;
               }
            }
            catch(Exception e) {e.printStackTrace();}
      }   
      if(!pathFound)
      {
         System.out.println("Ranger "+id+" cant find path");
         return null;
      }
      Pair end = p;
      Path pa = new Path(start, end);
      while(prev[p.x][p.y]!=null)
      {
         pa.seq.addFirst(findDirection(prev[p.x][p.y], p));
         p = prev[p.x][p.y];
      }
      paths[start.x][start.y][end.x][end.y] = pa;
      //System.out.println("Ranger "+id+" is targeting "+end);
      return end;
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
   public static KarbAdjacent bestMine(int id) throws Exception
   {
      Pair pos = unitPair(id);
      if(bestKarbAdj.get(regions[pos.x][pos.y])==null||bestKarbAdj.get(regions[pos.x][pos.y]).size()==0)
         return null;
      KarbAdjacent best = null;
      int bestPathSize = 0;
      int bestScore = 0;
      for(KarbAdjacent k: bestKarbAdj.get(regions[pos.x][pos.y]))
      {
         if(k.size<bestScore)
            break;
         if(!noKarbonite[k.loc.x][k.loc.y]&&!usedMine[k.loc.x][k.loc.y]&&!viableSite[k.loc.x][k.loc.y])
         {
            paths[pos.x][pos.y][k.loc.x][k.loc.y] = findPath(pos, k.loc, true);
            if(paths[pos.x][pos.y][k.loc.x][k.loc.y]==null)
               continue;
            int pathSize = paths[pos.x][pos.y][k.loc.x][k.loc.y].seq.size();
            int score = k.size-pathSize*((int)gc.unit(id).workerHarvestAmount());
            if(best==null||(score==bestScore&&pathSize<bestPathSize)||score>bestScore)
            {
               best = k;
               bestPathSize = pathSize;
               bestScore = score;
            }
         }
      }
      return best;
   }
   //END OF KARBONITE-RELATED METHODS
   
   //HELPER METHODS
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
      if(u1.equals(u2))
         return 0;
      if(u1.equals(UnitType.Factory)&&ua.structureIsBuilt()!=0)
         return -1;
      if(u2.equals(UnitType.Factory)&&ub.structureIsBuilt()!=0)
         return 1;
      if(u1.equals(UnitType.Knight))
         return -1;
      if(u2.equals(UnitType.Knight))
         return 1;
      if(u1.equals(UnitType.Mage))
         return -1;
      if(u2.equals(UnitType.Mage))
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
   public static int distance(Pair p1, Pair p2)
   {
      return (p1.x-p2.x)*(p1.x-p2.x)+(p1.y-p2.y)*(p1.y-p2.y);
   }
   public static boolean moveable(Pair p)
   {
      if(!gc.hasUnitAtLocation(eMapLoc[p.x][p.y]))
         return true;
      Unit u = gc.senseUnitAtLocation(eMapLoc[p.x][p.y]);
      return u==null||u.team().equals(enemyTeam)||isStructure(u.id());
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
      Pair cur = unitPair(id);
      Path bestPath = null;
      Pair bestP = null;
      for(int ro: rockets)
      {
         Pair target = unitPair(ro);
         Path pa = findPath(cur, target, true);
         if(pa==null)
            continue;
         if(bestPath==null||pa.seq.size()<bestPath.seq.size())
         {
            bestPath = pa;
            bestP = target;
         }
      }
      return bestP;
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
      Pair buildTarget = null; //for building: target blueprint
      Pair moveTarget = null; //for moving: final destination
      UnitType produceType = null; //for producing: type of robot
      UnitType buildType = null;
      Path path = null;
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
      public boolean startMoving(Pair target)
      {
         if(target==null)
            return false;
         Pair curLoc = unitPair(unitID);
         path = paths[curLoc.x][curLoc.y][target.x][target.y] = findPath(curLoc, target, true);
         if(paths[curLoc.x][curLoc.y][target.x][target.y]==null)
            return false;
         moveTarget = target;
         return true;
      }
      public boolean stopMoving()
      {
         moveTarget = null;
         path = null;
         return true;
      }
      public boolean startMining(Pair target) throws Exception 
      {
         if(target==null)
         {
            System.out.println(unitPair(unitID)+" to "+target);
            //return false;
         }
         typeOn[0] = maxStep++;
         startMoving(target);
         usedMine[target.x][target.y] = true;
         return true;
      }
      public boolean stopMining()
      {
         //System.out.println("Unit "+unitID+" has stopped mining");
         typeOn[0] = 0;
         stopMoving();
         return true;
      }
      public boolean startBuilding(Pair site, UnitType ut) throws Exception
      {
         if(gc.round()>325&&Math.random()<0.2)
            ut = UnitType.Rocket;
         Pair cur = unitPair(unitID);
         //System.out.println("The unit at "+unitPair(unitID)+" is going to build at "+site);
         typeOn[1] = maxStep++;
         buildTarget = site;
         buildType = ut;
         if(isCardinal(cur, site))
            return true;
         HashSet<Pair> possible = locWithin(site, 0, 1);
         Pair bestLoc = null;
         int bestDist = 0;
         for(Pair target: possible)
         {
            paths[cur.x][cur.y][target.x][target.y] = findPath(cur, target, true);
            if(paths[cur.x][cur.y][target.x][target.y]==null)
               continue;
            int d = paths[cur.x][cur.y][target.x][target.y].seq.size();
            if(bestLoc==null||d<bestDist)
            {
               bestLoc = target;
               bestDist = d;
            }
         }
         startMoving(bestLoc);
         return true;
      }
      public boolean stopBuilding()
      {
         typeOn[1] = 0;
         stopMoving();
         buildTarget = null;
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
      public boolean startAttacking(Pair target) throws Exception
      {
         typeOn[4] = maxStep++;
         startMoving(target);
         return true;
      }
      public boolean stopAttacking()
      {
         typeOn[4] = 0;
         stopMoving();
         return true;
      }
      public boolean startLoading()
      {
         Pair closest = closestRocket(unitID);
         if(closest==null)
            return false;
         startMoving(closest);
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
         Pair p = unitPair(unitID);
         int ret = 0;
         int status;
         switch(getTask()) {
            case 0:
               if(noKarbonite[moveTarget.x][moveTarget.y])
                  if(bestMine(unitID)!=null)
                     startMining(bestMine(unitID).loc);
                  else
                  {
                     stopMining();
                     return -1;
                  }
               status = mine(unitID);
               if(moveTarget.equals(p)&&status==-1)
               {
                  ret = -1;
                  noKarbonite[p.x][p.y] = true;
                  stopMining();
               }
               break;
            case 1:
               if(siteID[buildTarget.x][buildTarget.y]==-1)
                  siteID[buildTarget.x][buildTarget.y] = -2;
               if(isCardinal(p, buildTarget))
                  stopMoving();
               status = build(unitID, buildTarget, buildType);
               if(status==-2)
                  if(gc.isMoveReady(unitID))
                     for(Direction d: directions)
                        if(!d.equals(Direction.Center))
                           if(gc.canMove(unitID, d))
                           {
                              gc.moveRobot(unitID, d);
                              status = blueprint(unitID, buildTarget, buildType);
                              break;
                           }
               if(status==1)  
                  stopBuilding();
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
            case 4:
               startAttacking(bestAttack(unitID));
               attack(unitID);
               break;
            case 6:
               if(!unitType(unitID).equals(UnitType.Worker))
                  attack(unitID);
               halting = false;
               break;
            default:
               if(unitType(unitID).equals(UnitType.Worker))
                  mine(unitID);
         }
         if(moveTarget!=null)
            move(unitID);
         return ret;
      }
   }
   static class Site implements Comparable<Site>
   {
      Pair loc;
      HashSet<Integer> init;
      int dist;
      public Site(Pair p, HashSet<Integer> ib, int d)
      {
         loc = p;
         init = ib;
         dist = d;
      }
      public int compareTo(Site s)
      {
         if(s==null)
            return -1;
         if(s.init.size()!=init.size())
            return s.init.size()-init.size();
         return dist-s.dist;
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
