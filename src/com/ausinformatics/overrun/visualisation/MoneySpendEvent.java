package com.ausinformatics.overrun.visualisation;

import com.ausinformatics.phais.core.visualisation.VisualGameEvent;

public class MoneySpendEvent extends VisualGameEvent {

   public int playerId;
   public int moneySpend;
   
   public MoneySpendEvent(int playerId, int moneySpend) {
      this.playerId = playerId;
      this.moneySpend = moneySpend;
  }
}
