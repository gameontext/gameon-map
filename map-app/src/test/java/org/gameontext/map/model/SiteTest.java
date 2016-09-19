package org.gameontext.map.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SiteTest {

    @Test
    public void testDates() {

        // construction via Json magic provides no values
        Site s = new Site();
        assertNotNull(s.getCreatedOn());
        assertNull(s.getAssignedOn());

        // parsing the instants when null is ok
        s.getAssignedInstant();
        s.getCreatedInstant();

        // construction via code assigns created date
        s = new Site(0, 0);
        assertNotNull(s.getCreatedOn());
        assertNull(s.getAssignedOn());

        // assigning a room sets the assigned date
        s.setInfo(new RoomInfo());
        assertNotNull(s.getAssignedOn());

        // parsing the instants when not null is ok
        s.getAssignedInstant();
        s.getCreatedInstant();

        // clearing the room clears the assigned date.
        s.setInfo(null);
        assertNull(s.getAssignedOn());
    }
}
