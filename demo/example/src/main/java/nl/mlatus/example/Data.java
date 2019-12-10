package nl.mlatus.example;

public class Data {
    static User[] users = {
            new User("John", "user", 18),
            new User("Mary", "user", 17),
            new User("Jane", "admin", 22),
            new User("Alice", "user", 19),
            new User("Steve", "user", 19)
    };

    public static User getUser(int serialNumber){
        return users[serialNumber];
    }
}
