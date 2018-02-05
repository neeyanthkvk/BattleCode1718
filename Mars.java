//ranger bug

import bc.*;
import java.util.*;
import java.io.*;
@SuppressWarnings("unchecked")
 public class Mars {
   //Basic Info 
   static long totalTime = 0;
   static GameController gc;
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
   static HashMap<Direction, Pair> dirMove = new HashMap<Direction, Pair>();
   static PlanetMap mMap;
   static int mWidth;
   static int mHeight;
   static Team myTeam;
   static Team enemyTeam;
   static Random random = new Random(1);
   static boolean rocketResearched = false;
   static int[][][][] moveDist;
   static int[][] adjCount;
   static Pair[][] pairs;
   
   
   //Unit Info
   static HashSet<Integer> myInit = new HashSet<Integer>();
   static HashSet<Pair> newSite;
   static HashSet<Integer> myUnits;
   static HashSet<Integer> healers;
   static HashSet<Integer> factories;
   static HashSet<Integer> knights;
   static HashSet<Integer> mages;
   static HashSet<Integer> rangers;
   static HashSet<Integer> rockets;
   static HashSet<Integer> workers;
   static HashMap<Integer, Task> tasks = new HashMap<Integer, Task>();
   static HashMap<Integer, Pair> enemyInit = new HashMap<Integer, Pair>();
   static TreeSet<Integer> enemyUnits = new TreeSet<Integer>();
   static HashSet<Pair> attackList = new HashSet<Pair>();
   
   // Map Info
   static MapLocation[][] mMapLoc;
   static int[][] karbDep;//amount of karbonite in the square
   static int[][] initKarb;
   static int[][] karbAdj;//sum of karbonite on and adjacent to the square
   static int[][] regions;
   static int regionCount = 0;
   static int[][] usedMine;
   static boolean[][] viableSite;
   static int[][] siteID;
   static HashSet<Pair>[][] adjPair;
   static boolean[] open = new boolean[512];
   static AsteroidStrike[] asteroids = new AsteroidStrike[maxRound+1];
      
   public static void init(GameController g)
   {
      try {
         long startTime = System.nanoTime();
         System.out.println("first random is "+random.nextDouble());
      
         gc = g;
         mMap = gc.startingMap(mars);
         mWidth = (int) mMap.getWidth();
         mHeight = (int) mMap.getHeight();
         myTeam = gc.team();
         moveDist = new int[mWidth][mHeight][mWidth][mHeight];
         adjCount = new int[mWidth][mHeight];
         pairs = new Pair[mWidth][mHeight];
         mMapLoc = new MapLocation[mWidth][mHeight];
         karbDep = new int[mWidth][mHeight];//amount of karbonite in the square
         initKarb = new int[mWidth][mHeight];
         karbAdj = new int[mWidth][mHeight];//sum of karbonite on and adjacent to the square
         regions = new int[mWidth][mHeight];
         usedMine = new int[mWidth][mHeight];
         viableSite = new boolean[mWidth][mHeight];
         siteID = new int[mWidth][mHeight];
         adjPair = new HashSet[mWidth][mHeight];
      
         AsteroidPattern ap = gc.asteroidPattern();
         // for(int x = 1; x <= maxRound; x++)
            // if(ap.hasAsteroid(x))
               // asteroids[x] = ap.asteroid(x);
         
         dirMove.put(Direction.North, new Pair(0, 1));
         dirMove.put(Direction.Northeast, new Pair(1, 1));
         dirMove.put(Direction.East, new Pair(1, 0));
         dirMove.put(Direction.Southeast, new Pair(1, -1));
         dirMove.put(Direction.South, new Pair(0, -1));
         dirMove.put(Direction.Southwest, new Pair(-1, -1));
         dirMove.put(Direction.West, new Pair(-1, 0));
         dirMove.put(Direction.Northwest, new Pair(-1, 1));
         dirMove.put(Direction.Center, new Pair(0, 0));
      
         for(int x = 0; x < mWidth; x++)
            for(int y = 0; y < mHeight; y++)
            {
               pairs[x][y] = new Pair(x, y);
               adjPair[x][y] = new HashSet<Pair>();
            }
                    
      //initializes mMapLoc, karbDep
         System.out.println("Initializing basic map info");
         for(int x = 0; x < mWidth; x++)
            for(int y = 0; y < mHeight; y++) 
               try {
                  mMapLoc[x][y] = new MapLocation(mars,x,y);
                  initKarb[x][y] = karbDep[x][y] = (int) mMap.initialKarboniteAt(mMapLoc[x][y]);
               } 
               catch(Exception e) {e.printStackTrace();}
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
            q.add(pairs[i/3][i%3]);
            array[i] = false;
            while(!q.isEmpty())
            {
               Pair p = q.remove();
               for(Direction d: adjacent)
               {
                  Pair next = addDirection(p, d);
                  if(next!=null)
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
      
             
      //initializes karbAdj
         System.out.println("Finding karbonite sums");
         for(int x = 0; x < mWidth; x++)
            for(int y = 0; y < mHeight; y++)
               try {
                  karbAdj[x][y] = countKarbAdj(pairs[x][y], false);
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
         int count = 1;
         for(int x = 0; x < mWidth; x++)
            cycle: for(int y = 0; y < mHeight; y++)
               try {
                  if(regions[x][y]!=0||mMap.isPassableTerrainAt(mMapLoc[x][y])==0)
                     continue cycle;
                  regionCount++;
                  floodRegion(pairs[x][y], regionCount);
               } 
               catch(Exception e) {e.printStackTrace();}
         System.out.println("Finished finding regions");
         
         //initializes adjPair
         for(int x = 0; x < mWidth; x++)
            for(int y = 0; y < mHeight; y++)
               for (int dx = -1; dx <= 1; dx++)
                  for (int dy = -1; dy <= 1; dy++)
                  {
                     int newX = x+dx;
                     int newY = y+dy;
                     if(inBounds(newX, newY))
                        if(regions[newX][newY]!=0)
                           adjPair[x][y].add(pairs[newX][newY]);
                  }
                  
      //initializes adjCount
         System.out.println("Finding adjacent counts");
         for(int x = 0; x < mWidth; x++)
            for(int y = 0; y < mHeight; y++)
               for(Direction d: adjacent)
               {
                  Pair p = addDirection(pairs[x][y], d);
                  if(inBounds(p)&&regions[p.x][p.y]!=0)
                     adjCount[p.x][p.y]++;
               }
         
      //initializes moveDist
         System.out.println("Finding moveDist");
         for(int x = 0; x < mWidth; x++)
            for(int y = 0; y < mHeight; y++)
            {
               for(int a = 0; a < mWidth; a++)
                  for(int b = 0; b < mHeight; b++)
                     moveDist[x][y][a][b] = -1;
               moveDist[x][y][x][y] = 0;
            }
         ArrayDeque<Pair> q = new ArrayDeque<Pair>();
         for(int x = 0; x < mWidth; x++)
         {
            for(int y = 0; y < mHeight; y++)
            {
               long time = System.nanoTime();
               if(regions[x][y]==0)
               {
                  //System.out.println("("+x+", "+y+") is not passable");
                  continue;
               }
               q.addFirst(pairs[x][y]);
               cycle: while(!q.isEmpty())
               {
                  Pair cur = q.removeLast();
                  for(Pair next: adjPair[cur.x][cur.y])
                     if(moveDist[x][y][next.x][next.y]==-1)
                     {
                        moveDist[x][y][next.x][next.y] = moveDist[x][y][cur.x][cur.y]+1;
                        q.addFirst(next);
                     }
               }   
               //System.gc();
               //System.out.println(" ("+x+", "+y+") Time taken: "+(System.nanoTime()-time)/1000000.0+" ms");
            }
         }
               
      //finds viable sites
         System.out.println("Finding viable sites");
         for(int x = 0; x < mWidth; x++)
            for(int y = 0; y < mHeight; y++)
            {
               int index = 0;
               for(int a = -1; a <= 1; a++)
                  for(int b = -1; b <= 1; b++)
                     if(inBounds(x+a, y+b)&&regions[x+a][y+b]!=0)
                        index+=(1<<((a+1)*3+b+1));
               viableSite[x][y] = open[index];
            }
         
         totalTime+=System.nanoTime()-startTime;
         System.out.println("Took "+totalTime/1000000.0+" ms to initialize");
      //end of initialization
      } 
      catch(Exception e) {e.printStackTrace();}
      run();
   }
   public static void run() {
      while(gc.round()<=maxRound)
      {
         long startTime = System.nanoTime();
         try
         {
            System.out.println("Mars Round "+gc.round()+": ");
            updateUnits();
                   
            for(int id: workers)
               try
               {
                  if(!usable(id))
                     continue;
                  Pair start = unitPair(id);
                  int val = tasks.get(id).doTask();
               }
               catch(Exception e)
               {
                  e.printStackTrace();
               }
               
            try {
               rangerTask();
            } 
            catch(Exception e) {e.printStackTrace();}
            
            for(int id: rockets)
               try {
                  if(!usable(id))
                     continue;
                  for(Direction d: adjacent)
                  {
                     MapLocation dest = gc.unit(id).location().mapLocation().add(d);
                     if(gc.hasUnitAtLocation(dest))
                     {
                        int pushID = gc.senseUnitAtLocation(dest).id();
                        move(pushID, true, null);
                     }
                     if(gc.canUnload(id, d))
                        gc.unload(id, d);
                  }
               }
               catch(Exception e)
               {
                  e.printStackTrace();
               }
         } 
         catch(Exception e) {e.printStackTrace();} 
         
         try {
            for(int x = 0; x < 100; x++)
               gc.writeTeamArray(x, -1);
            int index = 0;
            HashSet<Integer> used = new HashSet<Integer>();
            complete: for(int x = 0; x < mWidth; x++)
               cycle:for(int y = 0; y < mHeight; y++)
               {
                  if(!viableSite[x][y])
                     continue;
                  for(int i: used)
                  {
                     int dx = Math.abs(i/100-x);
                     int dy = Math.abs(i%100-y);
                     if(dx<=1&&dy<=1)
                        continue cycle;
                  }
                  gc.writeTeamArray(index++, x*100+y);
                  used.add(x*100+y);
                  if(index==100)
                     break complete;
               }
         } 
         catch(Exception e) {e.printStackTrace();}
         
            //System.gc();
         totalTime+=System.nanoTime()-startTime;
         System.out.println("Mars Round "+gc.round()+" took "+(System.nanoTime()-startTime)/1000000.0+" ms");
         System.out.println("Mars Round "+gc.round()+" ended at "+(totalTime)/1000000.0+" ms");
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
   
   //-2 means too much heat, -1 means no karbonite in adjacent squares, 0 means successfully mined
   public static int mine(int id)
   {
      Unit u = gc.unit(id);
      Pair p = unitPair(id);
      int xPos = p.x;
      int yPos = p.y;
      Direction best = null;
      int max = 0;
      for(Direction d: directions)
         try
         {
            Pair loc = addDirection(p, d);
            if(inBounds(loc))
               if(gc.karboniteAt(mMapLoc[loc.x][loc.y])>max)
               {
                  best = d;
                  max = (int)gc.karboniteAt(mMapLoc[p.x][p.y].add(d));
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
   public static int move(int id, boolean push, HashSet<Integer> prevPush)
   {
      if(prevPush==null)
         prevPush = new HashSet<Integer>();
      if(prevPush.contains(id))
         return -1;
      prevPush.add(id);
      Unit u = gc.unit(id);
      Pair start = unitPair(id);
      Pair target = tasks.get(id).moveTarget;
      if((target==null&&!push)||!gc.isMoveReady(id))
         return -1;
      //System.out.println("moving from "+start+" to "+target);
      HashSet<Direction> possible = nextMove(start, target, tasks.get(id).targetDist);
      if(possible!=null)
      {
         for(Direction d: possible)
            if(gc.canMove(id, d))
            {
               Pair dest = addDirection(start, d);
               if(siteID[dest.x][dest.y]!=-1)
                  continue;
               gc.moveRobot(id, d);
               return 0;
            }
         for(Direction d: possible)
         {
            Pair dest = addDirection(start, d);
            if(siteID[dest.x][dest.y]!=-1)
               continue;
            if(gc.canSenseLocation(mMapLoc[dest.x][dest.y])&&gc.hasUnitAtLocation(mMapLoc[dest.x][dest.y]))
            {
               Unit block = gc.senseUnitAtLocation(mMapLoc[dest.x][dest.y]);
               if(block==null||block.team().equals(enemyTeam))
                  continue;
               if(move(block.id(), true, prevPush)==-1)
                  continue;
            }
            if(gc.canMove(id, d))
            {
               gc.moveRobot(id, d);
               return 0;
            }
         }
      }
      if(push)
         for(Direction d: adjacent)
            if(gc.canMove(id, d))
            {
               Pair dest = addDirection(start, d);
               if(siteID[dest.x][dest.y]!=-1)
                  continue;
               if(gc.canSenseLocation(mMapLoc[dest.x][dest.y])&&gc.hasUnitAtLocation(mMapLoc[dest.x][dest.y]))
               {
                  Unit block = gc.senseUnitAtLocation(mMapLoc[dest.x][dest.y]);
                  if(block==null||block.team().equals(enemyTeam))
                     continue;
                  if(move(block.id(), true, prevPush)==-1)
                     continue;
               }
               if(gc.canMove(id, d))
               {
                  gc.moveRobot(id, d);
                  return 0;
               }
            }
      return -1;
   }
   public static void replicate(int id)
   {
      double val = random.nextDouble();
      if(gc.karbonite()<60||gc.unit(id).abilityHeat()>=10)//REPLICATE COST
         return;
      int type = -1;
      if(type!=-1)
      {
         Pair start = unitPair(id);
         Pair target = tasks.get(id).moveTarget;
         int targetDist = tasks.get(id).targetDist;
         Direction best = null;
         int bestDist = 0;
         for(Direction d: adjacent)
            if(gc.canReplicate(id, d))
            {
               Pair next = addDirection(start, d);
               if(siteID[next.x][next.y]==-1)
               {
                  if(best==null||Math.abs(moveDist[target.x][target.y][next.x][next.y]-targetDist)<Math.abs(bestDist-targetDist))
                  {
                     best = d;
                     bestDist = moveDist[target.x][target.y][next.x][next.y];
                  }
               }
            }
         if(best!=null)
         {
            gc.replicate(id, best);
            MapLocation repLoc = mMapLoc[start.x][start.y].add(best);
            int newID = gc.senseUnitAtLocation(repLoc).id();
            tasks.put(newID, new Task(newID));
            tasks.get(newID).doTask();
         }
         else 
            System.out.println("replication fail at "+start);
      }
   }
   public static void updateUnits() throws Exception
   {
      int round = (int) gc.round();
      if(asteroids[round]!=null)
      {
         int add = (int) asteroids[round].getKarbonite();
         Pair loc = mapPair(asteroids[round].getLocation());
         int x = loc.x;
         int y = loc.y;
         initKarb[x][y]+=add;
         karbDep[x][y]+=add;
         asteroids[round] = null;
      }
      for(int x = 0; x < mWidth; x++)
         for(int y = 0; y < mHeight; y++)
            if(gc.canSenseLocation(mMapLoc[x][y]))
               karbDep[x][y] = (int) gc.karboniteAt(mMapLoc[x][y]);
            else
               karbDep[x][y] = Math.min(karbDep[x][y], initKarb[x][y]);  
   
      if(gc.researchInfo().getLevel(UnitType.Rocket)>0)
         rocketResearched = true;
      myUnits = toIDList(gc.myUnits());
      healers = new HashSet<Integer>();
      factories = new HashSet<Integer>();
      knights = new HashSet<Integer>();
      mages = new HashSet<Integer>();
      rangers = new HashSet<Integer>();
      rockets = new HashSet<Integer>();
      workers = new HashSet<Integer>();
      newSite = new HashSet<Pair>();
      for(int x = 0; x < mWidth; x++)
         for(int y = 0; y < mHeight; y++)
         {
            siteID[x][y] = -1;
            usedMine[x][y] = -1;
         }
      for(int id: myUnits)
      {
         if(tasks.get(id)==null)
            tasks.put(id, new Task(id));
         UnitType ut = gc.unit(id).unitType();
         Location uLoc = gc.unit(id).location();
         if(ut.equals(UnitType.Healer))
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
                  if(tasks.get(id).moveTarget!=null)
                     usedMine[tasks.get(id).moveTarget.x][tasks.get(id).moveTarget.y]=id;
            } 
            catch(Exception e) {
               System.out.println("unit "+id+" broke");
               e.printStackTrace();
            }
         }
      }
      enemyUnits.clear();
      for(int id: myUnits)
         if(usable(id))
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
   }
   //END OF UNIT METHODS
   
   //MAP METHODS
   public static HashSet<Direction> nextMove(Pair start, Pair end, int goal)
   {
      if(end==null||moveDist[end.x][end.y][start.x][start.y]==goal)
         return null;
      int curDist = moveDist[end.x][end.y][start.x][start.y];
      int direction = 1;
      if(goal<moveDist[end.x][end.y][start.x][start.y])
         direction = -1;
         
      if(moveDist[end.x][end.y][start.x][start.y]==-1)
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
         Pair next = addDirection(start, d);
         if(inBounds(next)&&moveDist[end.x][end.y][next.x][next.y]==curDist+direction)
            possible.add(d);
      }
      return possible;
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
         for(Direction d: adjacent)
            try{
               Pair next = addDirection(id, d); 
               if(inBounds(next)&&regions[next.x][next.y]==0&&mMap.isPassableTerrainAt(mMapLoc[next.x][next.y])!=0)
               {
                  regions[next.x][next.y] = c; 
                  q.add(next);
               }
            } 
            catch(Exception e) {e.printStackTrace();}
      }
   }
   public static HashSet<Pair> locWithin(Pair target, int minRange, int maxRange)
   {
      VecMapLocation minVec = gc.allLocationsWithin(mMapLoc[target.x][target.y], minRange);
      VecMapLocation maxVec = gc.allLocationsWithin(mMapLoc[target.x][target.y], maxRange);
      HashSet<Pair> ret = new HashSet<Pair>();
      for(int x = 0; x < maxVec.size(); x++)
         ret.add(mapPair(maxVec.get(x)));
      for(int y = 0; y < minVec.size(); y++)
         ret.remove(mapPair(minVec.get(y)));
      return ret;
   }
   public static Pair closestEnemy(Pair start, HashSet<Pair> all)
   {
      Pair best = null;
      int bestScore = 100000;
      for(Pair end: all)
      {
         int score = moveDist[start.x][start.y][end.x][end.y];
         if(score==-1)
            continue;
         if(score<bestScore)
         {
            best = end;
            bestScore = score;
         }
      }
      return best;
   }
   public static void rangerTask()
   {
      if(rangers.size()==0)
         return;
      int minRange = 0;
      int maxRange = 0;
      for(int id: rangers)
      {
         minRange = (int)gc.unit(id).rangerCannotAttackRange();
         maxRange = (int)gc.unit(id).attackRange();
         break;
      }
      HashSet<Pair> all = new HashSet<Pair>();
      for(Pair target: attackList)
      {
         VecMapLocation maxVec = gc.allLocationsWithin(mMapLoc[target.x][target.y], maxRange);
         for(int x = 0; x < maxVec.size(); x++)
            all.add(mapPair(maxVec.get(x)));
      }
      for(Pair target: attackList)
      {
         VecMapLocation minVec = gc.allLocationsWithin(mMapLoc[target.x][target.y], minRange);
         for(int x = 0; x < minVec.size(); x++)
            all.remove(mapPair(minVec.get(x)));
      }
      for(int id: rangers)
      {
         if(!usable(id))
            continue;
         Pair cloEn = closestEnemy(unitPair(id), all);
         if(tasks.get(id).getTask()!=6)
            tasks.get(id).startMoving(cloEn, 0);
         //System.out.println("ranger at "+unitPair(id)+" going to "+tasks.get(id).moveTarget);
         move(id, false, null);
      }
      for(int id: rangers)
         if(usable(id)&&gc.isAttackReady(id))
         {
            VecUnit vu = gc.senseNearbyUnitsByTeam(mMapLoc[unitPair(id).x][unitPair(id).y], gc.unit(id).attackRange(), enemyTeam);
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
      boolean[][] used = new boolean[mWidth][mHeight];
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
               Pair next = addDirection(id, d);   
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
         for(int x = Math.max(0, xPos-1); x < Math.min(mWidth-1, xPos+1); x++)
            for(int y = Math.max(0, yPos-1); y < Math.min(mHeight-1, yPos+1); y++)
               count+=gc.karboniteAt(mMapLoc[x][y]);
         return count;
      }
      else
      {
         int count = 0;
         for(int x = Math.max(0, xPos-1); x < Math.min(mWidth-1, xPos+1); x++)
            for(int y = Math.max(0, yPos-1); y < Math.min(mHeight-1, yPos+1); y++)
               count+=karbDep[x][y];
         return count;
      }
   }
   //return best mine given known current conditions
   public static Pair bestMine(int id)
   {
      Pair start = unitPair(id);
      LinkedList<Pair> q = new LinkedList<Pair>();
      boolean[][] used = new boolean[mWidth][mHeight];
      q.add(start);
      used[start.x][start.y] = true;
      while(!q.isEmpty())
      {
         Pair cur = q.remove();
         if(karbDep[cur.x][cur.y]>0)
            if(usedMine[cur.x][cur.y]==-1||usedMine[cur.x][cur.y]==id)
               if(siteID[cur.x][cur.y]==-1)
                  return cur;
         for(Direction d: adjacent)
         {
            Pair next = addDirection(cur, d);
            if(inBounds(next)&&!used[next.x][next.y]&&regions[next.x][next.y]!=0)
            {
               q.add(next);
               used[next.x][next.y] = true;
            }
         }
      }
      System.out.println("bestmine not found");
      return null;
   }
   //END OF KARBONITE-RELATED METHODS
   
   //HELPER METHODS
   //determines whether or not an enemy poses a threat to a building at this site
   public static boolean vulnerable(int id, Pair start)
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
   public static boolean usable(int id)
   {
      return !gc.unit(id).location().isInGarrison()&&!gc.unit(id).location().isInSpace();
   }
   public static boolean isCardinal(Pair p1, Pair p2)
   {
      MapLocation m1 = mMapLoc[p1.x][p1.y];
      MapLocation m2 = mMapLoc[p2.x][p2.y];
      for(Direction d: cardinals)
         if(m1.add(d).equals(m2))
            return true;
      return false;
   }
   public static boolean isDiagonal(Pair p1, Pair p2)
   {
      MapLocation m1 = mMapLoc[p1.x][p1.y];
      MapLocation m2 = mMapLoc[p2.x][p2.y];
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
   public static Pair addDirection(Pair p, Direction d)
   {
      int dx = dirMove.get(d).x;
      int dy = dirMove.get(d).y;
      if(inBounds(p.x+dx, p.y+dy))
         return pairs[p.x+dx][p.y+dy];
      return null;
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
      return mMapLoc[start.x][start.y].directionTo(mMapLoc[end.x][end.y]);
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
      return gc.hasUnitAtLocation(mMapLoc[id.x][id.y])&&regions[id.x][id.y]!=0;
   }
   public static boolean inBounds(Pair id)
   {
      if(id==null)
         return false;
      return id.x>=0&&id.x<mWidth&&id.y>=0&&id.y<mHeight;
   }
   public static boolean inBounds(int x, int y)
   {
      return x>=0&&x<mWidth&&y>=0&&y<mHeight;
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
      return pairs[m.getX()][m.getY()];
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
         if(gc.unit(ro).structureIsBuilt()==0)
            continue;
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
      public boolean startMining(Pair target)
      {
         if(target==null)
         {
            return false;
         }
         typeOn[0] = maxStep++;
         startMoving(target, 0);
         usedMine[target.x][target.y] = unitID;
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
      */
      public int doTask()
      {
         Pair start = unitPair(unitID);
         if(getTask()==-1)
            startMining(bestMine(unitID));
         start = unitPair(unitID);
         if(moveTarget!=null)
            move(unitID, false, null);
         int ret = 0;
         int status;
         //System.out.println("pos "+start+" "+getTask());
         switch(getTask()) {
            case 0:
                  //System.out.println("worker at "+start+" is going to mine at "+moveTarget);
               status = mine(unitID);
               if(gc.round()>=750)
                  replicate(unitID);
               break;
         }
         if(unitType(unitID).equals(UnitType.Worker))
            mine(unitID);
         return ret;
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
