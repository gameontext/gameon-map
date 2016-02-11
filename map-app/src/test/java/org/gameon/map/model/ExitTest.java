package org.gameon.map.model;

import static org.junit.Assert.assertEquals;

import org.gameon.map.models.Exit;
import org.gameon.map.models.RoomInfo;
import org.gameon.map.models.Site;
import org.junit.Test;

public class ExitTest {
    
    @Test
    public void creatingExitToDoorlessSiteShouldntNPE() {
        Site doorlessSite = new Site();
        doorlessSite.setInfo(new RoomInfo());
        Exit exit = new Exit(doorlessSite, "N");
        assertEquals("A door", exit.getDoor());
    }

}
