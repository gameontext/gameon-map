package net.wasdev.gameon.map.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.wasdev.gameon.map.models.Exit;
import net.wasdev.gameon.map.models.RoomInfo;
import net.wasdev.gameon.map.models.Site;

public class ExitTest {
    
    @Test
    public void creatingExitToDoorlessSiteShouldntNPE() {
        Site doorlessSite = new Site();
        doorlessSite.setInfo(new RoomInfo());
        Exit exit = new Exit(doorlessSite, "N");
        assertEquals("A door", exit.getDoor());
    }

}
