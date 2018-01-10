import bc.*;
import java.util.*;
public class Player {
	static PriorityQueue<KarbDeposit> earthKarbs;
	static GameController gc;
	static int numDep;
    public static void main(String[] args) {
        gc = new GameController();
        
        Direction[] dirs = Direction.values();
        boolean onEarth = true;
        earthKarbs = new PriorityQueue<KarbDeposit>();
        Planet earth = Planet.Earth;
        PlanetMap er = gc.startingMap(earth);
        for(int i = 0; i < (int) er.getWidth(); i++) {
        	for(int j = 0; j < (int) er.getHeight(); j++) {
        		MapLocation temp = new MapLocation(earth,i,j);
        		if(er.initialKarboniteAt(temp) != 0) {
        			KarbDeposit k = new KarbDeposit(temp,er.initialKarboniteAt(temp));
        			earthKarbs.add(k);
        		}
        	}
        }

        numDep = earthKarbs.size();
        while (true) {
            if(onEarth) {
                earth();
                onEarth = !onEarth;
                gc.nextTurn();
            }   
            else {
                mars();
                onEarth = !onEarth;
                gc.nextTurn();
            } 	

        }      


    }

    public static void earth() {
        // Do Something
        System.out.println("Earth Stuffs");
        PlanetMap er = gc.startingMap(Planet.Earth);
        VecUnit units = gc.myUnits();
        long karb = gc.karbonite();
        
                
        
        // Deploy Workers Made 
        for(long i = 0; i < units.size(); i++) {
        	Unit temp = units.get(i);
        	if(temp.unitType() == UnitType.Factory) {
        		
        	}
        }
        
        // 
        for(long i= 0; i < units.size(); i++) {
        	Unit temp = units.get(i);
        	
        }
        
        
        while(karb >= 150) {
        	
        }
        
                
    }

    public static void mars() {
        // Do Something
        System.out.println("Mars Stuffs");
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
