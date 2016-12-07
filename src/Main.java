import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by halleyfroeb on 9/22/16.
 */
public class Main {

    public static void createTables(Connection conn) throws SQLException{
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users (id IDENTITY, name VARCHAR, password VARCHAR)");
        stmt.execute("CREATE TABLE IF NOT EXISTS MESSAGES (id IDENTITY, user_id INT, reply_id INT, text VARCHAR)");
    }

    public static void insertUser(Connection conn, String name, String password) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES (NULL, ?, ?)");
        stmt.setString(1, name);
        stmt.setString(2, password);
        stmt.execute();
    }

    public static User selectUser(Connection conn, String name) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
        stmt.setString(1, name);
        ResultSet results = stmt.executeQuery();
        if (results.next()){ // instead of while loop
            int id = results.getInt("id");
            String password = results.getString("password");
            return new User(id, name, password);
        }
        return null;
    }
    public static void insertMessage(Connection conn, int userId, int replyId, String text) throws SQLException{
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO messages VALUES (NULL, ?, ?, ?)");
        stmt.setInt(1, userId);
        stmt.setInt(2, replyId);
        stmt.setString(3, text);
        stmt.execute();
    }

    public static Message selectMessage(Connection conn, int id)throws SQLException{
        PreparedStatement stmt = conn.prepareStatement
                ("SELECT * FROM messages INNER JOIN users ON messages.user_id = users.id WHERE messages.id = ?");
        stmt.setInt(1, id);
        ResultSet results = stmt.executeQuery();
        if(results.next()){
            int replyId = results.getInt("reply_Id");
            String name = results.getString("users.name");
            // column id is users.name; since there is multiple columns with the same name
            String text = results.getString("text");
            return new Message(id, replyId, name, text);
        }
        return null; // if not found
    }
    public static ArrayList<Message> selectReplies(Connection conn, int replyId) throws SQLException{
        ArrayList<Message> messages = new ArrayList<>();
        PreparedStatement stmt = conn.prepareStatement
                ("SELECT * FROM messages INNER JOIN users ON messages.user_id = users.id WHERE messages.reply_id = ? ");
        stmt.setInt(1, replyId);
        ResultSet results = stmt.executeQuery();
        while (results.next()){
            int id = results.getInt("messages.id");
            String name = results.getString("users.name");
            String text = results.getString("messages.text");
            Message message = new Message(id, replyId, name, text);
            messages.add(message);
        }
        return messages;
    }

    public static void main(String[] args) throws SQLException {
        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        createTables(conn);

//
//        addTestUsers(conn);
//        addTestMessages(conn);

        Spark.init();
        Spark.get("/",
                ((request, response) -> {
                    String replyId = request.queryParams("replyId");
                    int replyIdNum = -1;
                    if (replyId != null) {
                        replyIdNum = Integer.parseInt(replyId);
                    }

// ternary operator: int replyIdNum = (replyId == null) ? -1  :Integer.parseInt(replyId);

                    HashMap m = new HashMap();
                    ArrayList<Message> threads = selectReplies(conn, replyIdNum);
//                    for (Message message : messages) {
//                        if (message.replyId == replyIdNum) {
//                            threads.add(message);
//                        }
//                    }
                    Session session = request.session();
                    String userName = session.attribute("userName");

                    m.put("userName", userName);
                    m.put("messages", threads);
                    m.put("replyId", replyIdNum);
                    return new ModelAndView(m, "home.html");
                }),
                new MustacheTemplateEngine()
        );

        Spark.post("/login",
                ((request, response) -> {
                    String userName = request.queryParams("loginName");
                    if (userName == null){
                        throw new Exception("login name not found");
                    }

                    User user = selectUser(conn, userName);
                    if (user == null){
                        user = new User(userName);
                        insertUser(conn, userName, "");
                    }

                    Session session = request.session();
                    session.attribute("userName", userName);

                    response.redirect("/");
                    return "";
                })
        );
        Spark.post("/logout",
                ((request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "";
                })
        );

        Spark.post("/create-message",
                ((request, response) -> {
                    String replyId = request.queryParams("replyId");
                    int replyIdNum = -1;
                    if (replyId != null) {
                        replyIdNum = Integer.parseInt(replyId);
                    }

                    Session session = request.session();
                    String userName = session.attribute("userName");
                    if (userName == null){
                        throw new Exception("Not logged in.");
                    }

                    String text = request.queryParams("messageText");
                    if (text == null && text.isEmpty()){
                        throw new Exception("didn't get necessary query parameters");
                    }
                    //Message m = new Message(messages.size(), replyIdNum, userName, text);
                   // messages.add(m);

                    User user = selectUser(conn, userName);
                    insertMessage(conn, user.id, replyIdNum, text);

                    response.redirect(request.headers("Referer"));
                    return "";

                }));


    }

    static void addTestUsers(Connection conn) throws SQLException {
        insertUser(conn, "Alice", "");
        insertUser(conn, "Bob", "");
        insertUser(conn, "Charlie", "");

    }

    static void addTestMessages(Connection conn) throws SQLException{
        User alice = selectUser(conn, "Alice");
        User bob = selectUser(conn, "Bob");
        User charlie = selectUser(conn, "Charlie");

        insertMessage(conn, alice.id, -1, "Hello, world!");
        insertMessage(conn, bob.id, -1, "this is another thread!" );
        insertMessage(conn, charlie.id, 0, "cool thread alice!" );
        insertMessage(conn, alice.id, 2, "thanks");

    }

}
