import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.habit.server.RoomManager;
import com.habit.server.TaskManager;

public class RoomManagerTest {

    @Test
    void createRoomAndRetrieveManager() {
        RoomManager rm = new RoomManager();
        assertTrue(rm.createRoom("room1"));
        assertTrue(rm.roomExists("room1"));
        TaskManager tm = rm.getTaskManager("room1");
        assertNotNull(tm, "Task manager should be returned for created room");
        // retrieving again returns same instance
        assertSame(tm, rm.getTaskManager("room1"));
    }

    @Test
    void createRoomFailsWhenExists() {
        RoomManager rm = new RoomManager();
        assertTrue(rm.createRoom("r"));
        assertFalse(rm.createRoom("r"), "Creating same room should fail");
    }
}
