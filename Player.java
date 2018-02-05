//ranger bug

import bc.*;
import java.util.*;
import java.io.*;
@SuppressWarnings("unchecked")
 public class Player {
   public static void main(String[] args) {
      GameController gc = new GameController();
      if(gc.planet().equals(Planet.Earth))
         Earth.init(gc);
      else
         Mars.init(gc);
      while(true)
      {
         gc.nextTurn();
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