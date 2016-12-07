import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Created by halleyfroeb on 9/28/16.
 */
public class MainTest {
    public Connection startConnection() throws SQLException{
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:test") ;// will only go in temp memory
        Main.createTables(conn);
        return conn;
    }
    // ^^ helper method not actual test
    // every test should be separate so that each test resets itself, do not depend on other tests

    @Test
    public void testUser() throws SQLException{
        Connection conn = startConnection();
        Main.insertUser(conn, "Alice", "");     //test data put into database
        User user = Main.selectUser(conn, "Alice"); // pull out of database
        conn.close(); // only do in tests
        assertTrue(user != null);
    }
    @Test
    public void testReplies() throws SQLException{
        Connection conn = startConnection();
        Main.insertUser(conn, "Alice", "");
        Main.insertUser(conn, "Bob", "");
        User alice = Main.selectUser(conn, "Alice");
        User bob = Main.selectUser(conn, "Bob");
        Main.insertMessage(conn, alice.id, -1, "Hello, world!");
        Main.insertMessage(conn, bob.id, 1, "This is a reply");
        Main.insertMessage(conn, bob.id, 1, "This is another reply");
        ArrayList<Message> replies = Main.selectReplies(conn, 1);
        assertTrue(replies.size() == 2);

        Message m = Main.selectMessage(conn, bob.id);
        conn.close();
        assertTrue(m.id == 2);
    }

}