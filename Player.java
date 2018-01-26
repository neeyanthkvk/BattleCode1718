//ranger bug

import bc.*;
import java.util.*;
import java.io.*;
@SuppressWarnings("unchecked")
 public class Player {
   public static void main(String[] args) {
      GameController gc = new GameController();
      if(gc.planet().equals(Planet.Earth))
         Earth.run();
      else
         Mars.run();
      while(true)
      {
         gc.nextTurn();
      }
   }
}