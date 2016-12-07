/**
 * Created by halleyfroeb on 9/22/16.
 */
public class User {
    String name;
    int id;
    String password;

    public User(String name) {
        this.name = name;
    }

    public User(int id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }
}
