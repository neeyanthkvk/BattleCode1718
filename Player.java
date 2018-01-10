import bc.*;

public class Player {
    public static void main(String[] args) {
        GameController gc = new GameController();
        
        Direction[] dirs = Direction.values();
        
        PriorityQueue<Task> earthpq = new PriorityQueue<Task>();
        PriorityQueue<Task> marspq  = new PriorityQueue<Task>();
        boolean onEarth = true;
        while (true) {
            if(onEarth) {
                earth(gc,earthpq);
                onEarth = !onEarth;
                gc.nextTurn();
            }   
            else {
                mars(gc,marspq);
                onEarth = !onEarth;
                gc.nextTurn();
            }

        }      


    }

    public static void earth(GameController gc, PriorityQueue<Task> pq) {
        // Do Something
        System.out.println("Earth Stuffs");
    }

    public static void mars(GameController gc, PriorityQueue<Task> pq) {
        // Do Something
        System.out.println("Mars Stuffs");
    }
}
